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
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import au.gov.asd.tac.constellation.plugins.Plugin;
import au.gov.asd.tac.constellation.plugins.PluginException;
import au.gov.asd.tac.constellation.plugins.PluginInfo;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.PluginType;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPluginCoreType;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointFactory;
import org.eclipse.rdf4j.federated.FedXFactory;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
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

    @Override
    protected RecordStore query(RecordStore query, PluginInteraction interaction, PluginParameters parameters) throws InterruptedException, PluginException {
//        String rdf4jServer = "http://eulersharp.sourceforge.net/2003/03swap/";
//        String repositoryID = "countries";
//        Repository repo = new HTTPRepository(rdf4jServer, repositoryID);

//        Repository repo = new SailRepository(new MemoryStore());
//
//        try {
//            RepositoryConnection con = repo.getConnection();
//            try {
//                URL url = new URL("http://eulersharp.sourceforge.net/2003/03swap/countries");
//                con.add(url, url.toString(), RDFFormat.TURTLE);
//            } catch (MalformedURLException ex) {
//                Exceptions.printStackTrace(ex);
//            } catch (IOException ex) {
//                Exceptions.printStackTrace(ex);
//            } catch (RDFParseException | RepositoryException ex) {
//                Exceptions.printStackTrace(ex);
//            } finally {
//                con.close();
//            }
//        } catch (RDF4JException e) {
//            // handle exception
//        }
//
//        try (RepositoryConnection conn = repo.getConnection()) {
//            String queryString = "SELECT ?x ?y WHERE { ?x ?p ?y } ";
//            TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);
//            try (TupleQueryResult result = tupleQuery.evaluate()) {
//                while (result.hasNext()) {  // iterate over the result
//                    BindingSet bindingSet = result.next();
//                    Value valueOfX = bindingSet.getValue("x");
//                    Value valueOfY = bindingSet.getValue("y");
//
//                    System.out.println(valueOfX.stringValue());
//                    System.out.println(valueOfY.stringValue());
//                }
//            }
//        }
        //----------------------------------- Using SPARQLRepository
//        Repository endpoint = new SPARQLRepository("http://dbpedia.org/sparql");
//        try (RepositoryConnection conn = endpoint.getConnection()) {
//            TupleQueryResult result = conn.prepareTupleQuery("SELECT * WHERE { ?s ?p ?o } LIMIT 10").evaluate();
//            result.forEach(System.out::println);
//        }
        //---------------------------------------------------Using FedX
        List<Endpoint> endpoints = new ArrayList<>();
        endpoints.add(EndpointFactory.loadSPARQLEndpoint("dbpedia", "http://dbpedia.org/sparql"));
//        endpoints.add(EndpointFactory.loadSPARQLEndpoint("wiki", "https://query.wikidata.org/sparql"));
//        endpoints.add(EndpointFactory.loadSPARQLEndpoint("swdf", "http://data.semanticweb.org/sparql")); // commented this out as it was timing out.

        Repository repo = FedXFactory.createFederation(endpoints);

        String q = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
	+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\n"
	+ "SELECT ?President ?Party WHERE {\n"
	+ "?President rdf:type dbpedia-owl:President .\n"
	+ "?President dbpedia-owl:party ?Party . }";

        TupleQuery tupleQuery = repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, q);
        try (TupleQueryResult res = tupleQuery.evaluate()) {
            while (res.hasNext()) {
                System.out.println(res.next());
            }
        }

        repo.shutDown();
        System.out.println("Done.");
        System.exit(0);
//  //-----------------------------------
        return new GraphRecordStore();
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
