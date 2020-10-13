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
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.inferencer.fc.CustomGraphQueryInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.openide.util.Exceptions;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author arcturus
 */
public class ImportFromRDFPluginNGTest {

    private static final Logger LOGGER = Logger.getLogger(ImportFromRDFPluginNGTest.class.getName());

    public ImportFromRDFPluginNGTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    @Test
    public void testMusicTurtleQuery() throws Exception {
        final RecordStore query = new GraphRecordStore();
        final PluginInteraction interaction = null;
        final ImportFromRDFPlugin instance = new ImportFromRDFPlugin();
        final PluginParameters parameters = instance.createParameters();
        parameters.setStringValue(ImportFromRDFPlugin.INPUT_FILE_URI_PARAMETER_ID, "file://" + this.getClass().getResource("./resources/music.ttl").getFile());

        final RecordStore expResult = new GraphRecordStore();
        final RecordStore result = instance.query(query, interaction, parameters);
//        TODO: instance.edit()
//        System.out.println(result.toStringVerbose());

        final Set<String> expectedIdentifiers = new TreeSet(result.getAll(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER));
        Set<String> actualIdentifiers = new TreeSet<>();
        actualIdentifiers.add("Bill_Wyman");
        actualIdentifiers.add("Charlie_Watts");
        actualIdentifiers.add("George_Harrison");
        actualIdentifiers.add("Helter_Skelter");
        actualIdentifiers.add("John_Lennon");
        actualIdentifiers.add("Keith_Richards");
        actualIdentifiers.add("Mick_Jagger");
        actualIdentifiers.add("Out_of_Our_Heads");
        actualIdentifiers.add("Paul_McCartney");
        actualIdentifiers.add("Ringo_Starr");
        actualIdentifiers.add("Ronnie_Wood");
        actualIdentifiers.add("Satisfaction");
        actualIdentifiers.add("The_Beatles");
        actualIdentifiers.add("The_Stones");
        actualIdentifiers.add("White_Album");
        assertEquals(expectedIdentifiers, actualIdentifiers);
        
//        final Set<String> expectedTypes = new TreeSet(result.getAll(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE));
//        Set<String> actualTypes = new TreeSet<>();
//        actualTypes.add("Album");
//        actualTypes.add("Artist");
//        actualTypes.add("Band");
//        actualTypes.add("Song");
//        assertEquals(expectedTypes, actualTypes);
}

    /**
     * Learning how to use the RDF4J Model and inferencing class
     *
     * @throws Exception
     */
    @Test
    public void testRdf4jModelAndInferencing() throws Exception {
        final GraphRecordStore results = new GraphRecordStore();
        final Model model = new LinkedHashModel();

        // 1) read a file into a model
        try {
            final URL documentUrl = new URL("file://" + this.getClass().getResource("./resources/example.ttl").getFile());
            final InputStream inputStream = documentUrl.openStream();
            final String baseURI = documentUrl.toString();
            final RDFFormat format = RDFFormat.TURTLE;

            try (GraphQueryResult res = QueryResults.parseGraphBackground(inputStream, baseURI, format)) {
                while (res.hasNext()) {
                    LOGGER.info("Processing next record...");

                    final Statement st = res.next();
                    model.add(st);

                    final Map<String, String> namespaces = res.getNamespaces();
                    final Resource subject = st.getSubject();
                    final IRI predicate = st.getPredicate();
                    final Value object = st.getObject();
                    final Resource context = st.getContext();

                    LOGGER.log(Level.INFO, "Saw Subject: {0}, Predicate: {1}, Object: {2}, Context: {3}", new Object[]{subject, predicate, object, context});
                }

                // play with the model
                final Set<Resource> subjects = model.subjects();
                final Set<IRI> predicates = model.predicates();
                final Set<Value> objects = model.objects();

                for (final IRI predicate : predicates) {
                    final Model predicateModel = model.filter(null, predicate, null);
                    LOGGER.log(Level.INFO, "Predicate {0} has {1} rows in model", new Object[]{predicate.getLocalName(), predicateModel.size()});
                }
            } catch (RDF4JException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                inputStream.close();
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        // 2) add it to a consty graph using RDFUtilities.PopulateRecordStore()
        // 3) convert the graph into a model
        // 4) load the model into an in RDF4J memory model
        System.out.println("4) load the model into an in RDF4J memory model");
        final Repository repo = new SailRepository(new MemoryStore());

        try (RepositoryConnection conn = repo.getConnection()) {
            // add the model
            conn.add(model);

            // let's check that our data is actually in the database
            try (RepositoryResult<Statement> result = conn.getStatements(null, null, null);) {
                for (Statement st : result) {
                    System.out.println("db contains: " + st);
                }
            }
        } finally {
            repo.shutDown();
        }

        // 5) apply the inferenceing rule
        System.out.println("5) apply the inferenceing rule");
        String pre = "PREFIX : <http://foo.org/bar#>\n";
        String rule = pre + "CONSTRUCT { ?p :relatesTo :Cryptography } WHERE "
                + "{ { :Bob ?p :Alice } UNION { :Alice ?p :Bob } }";
        String match = pre + "CONSTRUCT { ?p :relatesTo :Cryptography } "
                + "WHERE { ?p :relatesTo :Cryptography }";

        final Repository repo2 = new SailRepository(new CustomGraphQueryInferencer(new MemoryStore(), QueryLanguage.SPARQL, rule, match));
        try (RepositoryConnection conn = repo2.getConnection()) {
            // add the model
            conn.add(model);

            // let's check that our data is actually in the database
            try (RepositoryResult<Statement> result = conn.getStatements(null, null, null);) {
                for (Statement st : result) {
                    System.out.println("db now contains: " + st);
                }
            }
        } finally {
            repo2.shutDown();
        }

        // 6) process the model to the Consty graph using the RDFUtilities.PopulateRecordStore()
    }

}
