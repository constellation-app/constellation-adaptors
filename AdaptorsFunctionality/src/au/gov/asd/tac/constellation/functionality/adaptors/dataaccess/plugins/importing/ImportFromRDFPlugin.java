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

import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.utilities.RDFMappings.AbstractRDFMap;
import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.utilities.RDFMappings.EularsharpCountriesRDFMap;
import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.utilities.RDFMappings.RDFMapStorage;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStoreUtilities;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;
import au.gov.asd.tac.constellation.plugins.Plugin;
import au.gov.asd.tac.constellation.plugins.PluginException;
import au.gov.asd.tac.constellation.plugins.PluginInfo;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.PluginType;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameter;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPluginCoreType;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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

    public static final String STRING_PARAMETER_ID = PluginParameter.buildId(ImportFromGraphMLPlugin.class, "string");
    
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
        
        
        
        AbstractRDFMap mapping = RDFMapStorage.getMap(EularsharpCountriesRDFMap.getInstance().getName());
        Map<String, String> prefixes = mapping.getPrefixes();
        
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
            
            TupleQuery tupleQuery = conn.prepareTupleQuery(queryString); //Prepare query 
            
            Map<String, String> predicateMap = mapping.getPredicateMap();
            
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
