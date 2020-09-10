/*
 * Copyright 2010-2020 Australian Signals Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.importing;

import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStoreUtilities;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.AnalyticConcept;
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.SpatialConcept;
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;
import au.gov.asd.tac.constellation.plugins.Plugin;
import au.gov.asd.tac.constellation.plugins.PluginException;
import au.gov.asd.tac.constellation.plugins.PluginInfo;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.PluginType;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPluginCoreType;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 *
 * @author arcturus
 */
@ServiceProviders({
    @ServiceProvider(service = DataAccessPlugin.class),
    @ServiceProvider(service = Plugin.class)})
@PluginInfo(pluginType = PluginType.IMPORT, tags = {"IMPORT"})
@NbBundle.Messages("ImportFromRDFPlugin=Import From RDF")
public class ImportFromRDFPlugin extends RecordStoreQueryPlugin implements DataAccessPlugin {

    private static final String typePredicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"; 
    
    @Override
    protected RecordStore query(RecordStore query, PluginInteraction interaction, PluginParameters parameters) throws InterruptedException, PluginException {

        Repository repo = new SailRepository(new MemoryStore());

        try {
            RepositoryConnection con = repo.getConnection();
            try {
                URL url = new URL("http://eulersharp.sourceforge.net/2003/03swap/countries");
                con.add(url, url.toString(), RDFFormat.TURTLE);
            } catch (MalformedURLException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } catch (RDFParseException | RepositoryException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                con.close();
            }
        } catch (RDF4JException e) {
            // handle exception
        }
        
        //TODO Research RDF4J etc
        //TODO Research base predicates; RDFS standard and what they map to in consty
        //TODO Seperate queries to retrieve those
        //TODO Develop ontology for constellation -> mapping RDF stuff to existing constellation stuff (Allow for icons etc) <BASIC MAPPING>
        //TODO Potentially: Seperate query for mapping from RDF to Consty <SPECIFIC MAPPING>
        //TODO Add triples to constellation graph and update display; RDF view?
        
        HashMap<String, String> prefixes = new HashMap<>();
        prefixes.put("country", "http://eulersharp.sourceforge.net/2003/03swap/countries#");
        prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
        prefixes.put("jur", "http://sweet.jpl.nasa.gov/2.3/humanJurisdiction.owl#");
        prefixes.put("dce", "http://purl.org/dc/elements/1.1/");
        prefixes.put("dct", "http://purl.org/dc/terms/");
        prefixes.put("owl", "http://www.w3.org/2002/07/owl#");
        prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        prefixes.put("skos", "http://www.w3.org/2004/02/skos/core#");
        
        GraphRecordStore results = new GraphRecordStore();
        try (RepositoryConnection conn = repo.getConnection()) {
            // Create query string
            StringBuilder prefixString = new StringBuilder();
            prefixes.forEach( (String key, String value) -> {
                prefixString.append("PREFIX ");
                prefixString.append(key);
                prefixString.append(": <");
                prefixString.append(value);
                prefixString.append("> ");
                }
            );
            String selectString = "SELECT ?Subject ?Predicate ?Object WHERE { ?Subject ?Predicate ?Object}";
            String queryString = prefixString + selectString;
//            String queryString = "PREFIX country: <" + COUNTRY + "> PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?Subject ?Country WHERE { ?Subject foaf:name ?Country} "; // For specific Country query/results
//            String queryString = "PREFIX country: <" + COUNTRY + "> PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?Subject ?Predicate ?Object WHERE { ?Subject ?Predicate ?Object} "; // Generic query
            
            Map<String, String> predicateMap = new HashMap<>();
            predicateMap.put(prefixes.get("foaf") + "name", GraphRecordStoreUtilities.SOURCE + SpatialConcept.VertexAttribute.COUNTRY);
            predicateMap.put(typePredicate, GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE);
            
            TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                ArrayList<String> nodeIdentifiers = new ArrayList<>();
                while (result.hasNext()) {  // iterate over the result
                    BindingSet bindingSet = result.next();
                    String subject = bindingSet.getValue("Subject").stringValue(); //Generic
                    String predicate = bindingSet.getValue("Predicate").stringValue();//Generic
                    String object = bindingSet.getValue("Object").stringValue();//Generic
                    for ( String prefix : prefixes.values()) {
                        subject = removePrefix(subject, prefix);
                        object = removePrefix(object, prefix);
                    }
                    int index;
                    if (nodeIdentifiers.contains(subject)){
                        index = nodeIdentifiers.indexOf(subject);
                    } else {
                        nodeIdentifiers.add(subject);
                        results.add();//Generic
                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, subject);//Generic
                        index = results.index();
                        
                    }
                    String attribute = predicateMap.getOrDefault(predicate, GraphRecordStoreUtilities.SOURCE + predicate);
                    results.set(index, attribute, object);//Generic
                }
            }
        }

        return results;
    }
    
    private String removePrefix(String value, String prefix) {
        if (value.contentEquals(prefix)) {
            return value;
        } else if (value.contains(prefix)) {
           return value.replaceFirst(prefix, "");
        } else {
        return value;
        }
    }

    @Override
    public String getType() {
        return DataAccessPluginCoreType.IMPORT;
    }

    @Override
    public int getPosition() {
        return 100;
    }

}
