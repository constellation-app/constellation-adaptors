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

import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.sail.ConstellationSail;
import au.gov.asd.tac.constellation.graph.Graph;
import au.gov.asd.tac.constellation.graph.ReadableGraph;
import au.gov.asd.tac.constellation.graph.WritableGraph;
import au.gov.asd.tac.constellation.graph.locking.DualGraph;
import au.gov.asd.tac.constellation.graph.monitor.GraphChangeEvent;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStoreUtilities;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import au.gov.asd.tac.constellation.graph.schema.Schema;
import au.gov.asd.tac.constellation.graph.schema.SchemaFactoryUtilities;
import au.gov.asd.tac.constellation.graph.schema.analytic.AnalyticSchemaFactory;
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.AnalyticConcept;
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;
import au.gov.asd.tac.constellation.graph.utilities.io.SaveGraphUtilities;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.TupleQueryResultBuilder;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.inferencer.fc.CustomGraphQueryInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.DedupingInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.DirectTypeHierarchyInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ShaclSailValidationException;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.locationtech.jts.util.Assert;
import org.openide.util.Exceptions;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

/**
 * TODO:
 * <ul>
 * <li> Plugin 1 & 2 - RDF4J SailRepo for Constellation or Utility conversion
 * <li> Plugin 1 & 2 - Chain Direct Type, Duplicate Remover and RDFS inferencing
 * in importing example (done)
 * <li> Plugin 3 - OWL-API inferencing interface/conversion to/from
 * Constellation data
 * <li> Plugin 4 - SPARQL query using OWL inferences (if possible)
 * <li> Plugin 5 - User adding a custom SPARQL CONSTRUCT inference rule (stretch
 * goal)
 * </ul>
 *
 * @author arcturus2
 * @author scorpius77
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

    /**
     * Demonstrates basic turtle file importing.
     *
     * @throws Exception
     */
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
     * Learning how to use the RDF4J Model and RDFS Inferencing.
     *
     * @throws Exception
     */
    @Test
    public void testRdf4jModelAndInferencing() throws Exception {
        final GraphRecordStore results = new GraphRecordStore();
        final Model model = new LinkedHashModel();
        final ValueFactory VF = SimpleValueFactory.getInstance();

        // 1) read a file into a model
        {
            final URL documentUrl = new URL("file://" + this.getClass().getResource("./resources/example.ttl").getFile());
            final InputStream inputStream = documentUrl.openStream();
            final String baseURI = documentUrl.toString();
            final RDFFormat format = RDFFormat.TURTLE;
            loadTriples(model, inputStream, baseURI, format);
        }

        // 2) add it to a consty graph using RDFUtilities.PopulateRecordStore()
        // 3) convert the graph into a model
        // 4) load the model into an in RDF4J memory model
        {
            System.out.println("4) load the model into an in RDF4J memory model");
            final Repository repo = new SailRepository(new MemoryStore());

            try ( RepositoryConnection conn = repo.getConnection()) {
                // add the model
                conn.add(model);

                // let's check that our data is actually in the database
                try ( RepositoryResult<Statement> result = conn.getStatements(null, null, null);) {
                    for (Statement st : result) {
                        System.out.println("raw triples: " + st);
                    }
                }
            } finally {
                repo.shutDown();
            }
        }

        // 5.1) apply the RDFS class rules
        {
            System.out.println("5.1) apply the RDFS class rules");
            final Repository repo = new SailRepository(new DirectTypeHierarchyInferencer(new MemoryStore()));

            try ( RepositoryConnection conn = repo.getConnection()) {
                // add the model
                conn.add(model);

                // let's check that our data is actually in the database
                try ( RepositoryResult<Statement> result = conn.getStatements(null, null, null);) {
                    for (Statement st : result) {
                        System.out.println("direct type inference: " + st);
                    }
                }

                // what type is Bob? This should be easy.
                try ( RepositoryResult<Statement> result = conn.getStatements(
                        VF.createIRI("http://foo.org/bar#Bob"),
                        SESAME.DIRECTTYPE,
                        null);) {
                    assertTrue(result.hasNext());
                    Statement st = result.next();
                    assertEquals(st.getObject().stringValue(), "http://foo.org/bar#Man");
                    assertFalse(result.hasNext());
                }

                // what type is Alice? This is harder because there are multiple options.
                try ( RepositoryResult<Statement> result = conn.getStatements(
                        VF.createIRI("http://foo.org/bar#Alice"),
                        SESAME.DIRECTTYPE,
                        null);) {
                    assertTrue(result.hasNext());
                    assertEquals(result.next().getObject().stringValue(), "http://foo.org/bar#Woman");
                    assertTrue(result.hasNext());
                    assertEquals(result.next().getObject().stringValue(), "http://foo.org/bar#Hacker");
                    assertFalse(result.hasNext());
                }

                // what type is the action exchangesKeysWith'?
                try ( RepositoryResult<Statement> result = conn.getStatements(
                        VF.createIRI("http://foo.org/bar#exchangesKeysWith"),
                        SESAME.DIRECTTYPE,
                        null);) {
                    assertTrue(result.hasNext());
                    assertEquals(result.next().getObject().stringValue(), RDF.PROPERTY.toString());
                    assertFalse(result.hasNext());
                }

                // what type is the attribute 'age'?
                try ( RepositoryResult<Statement> result = conn.getStatements(
                        VF.createIRI("http://foo.org/bar#age"),
                        SESAME.DIRECTTYPE,
                        null);) {
                    assertTrue(result.hasNext());
                    assertEquals(result.next().getObject().stringValue(), RDF.PROPERTY.toString());
                    assertFalse(result.hasNext());
                }
            } finally {
                repo.shutDown();
            }
        }

        // 5.2) apply the custom inferencing rules
        {
            System.out.println("5.2) apply custom inferencing");
            String pre = "PREFIX : <http://foo.org/bar#>\n";
            String rule = pre + "CONSTRUCT { ?p :relatesTo :Cryptography } WHERE "
                    + "{ { :Bob ?p :Alice } UNION { :Alice ?p :Bob } }";
            String match = pre + "CONSTRUCT { ?p :relatesTo :Cryptography } "
                    + "WHERE { ?p :relatesTo :Cryptography }";

            final Repository repo = new SailRepository(new CustomGraphQueryInferencer(new MemoryStore(), QueryLanguage.SPARQL, rule, match));
            try ( RepositoryConnection conn = repo.getConnection()) {
                // add the model
                conn.add(model);

                // let's check that our data is actually in the database
                try ( RepositoryResult<Statement> result = conn.getStatements(null, null, null);) {
                    for (Statement st : result) {
                        System.out.println("custom inference: " + st);
                    }
                }

                // check the new triples are added
                try ( RepositoryResult<Statement> result = conn.getStatements(
                        null,
                        VF.createIRI("http://foo.org/bar#relatesTo"),
                        VF.createIRI("http://foo.org/bar#Cryptography"));) {
                    assertTrue(result.hasNext());
                    assertEquals(result.next().getSubject().stringValue(), "http://foo.org/bar#sendsMessageTo");
                    assertTrue(result.hasNext());
                    assertEquals(result.next().getSubject().stringValue(), "http://foo.org/bar#exchangesKeysWith");
                    assertFalse(result.hasNext());
                }
            } finally {
                repo.shutDown();
            }
        }

        // 5.3) apply the SHACL inferencing rules
        {
            System.out.println("5.3) apply shacl inferencing");
            final Repository repo = new SailRepository(new ShaclSail(new MemoryStore()));
            repo.init();
            try ( RepositoryConnection conn = repo.getConnection()) {
                // start a transaction
                conn.begin();

                // Persons have a single age and it is an integer
                StringReader shaclRules = new StringReader(
                        String.join("\n", "",
                                "@prefix : <http://foo.org/bar#> .",
                                "@prefix sh: <http://www.w3.org/ns/shacl#> .",
                                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
                                ":PersonShape",
                                "  a sh:NodeShape  ;",
                                "  sh:targetClass :Person ;",
                                "  sh:property :PersonShapeProperty .",
                                ":PersonShapeProperty ",
                                "  sh:path :age ;",
                                "  sh:datatype xsd:integer ;",
                                "  sh:maxCount 1 ;",
                                "  sh:minCount 1 ."
                        ));

                // add the rules and the model
                conn.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
                conn.add(model);
                try {
                    conn.commit();
                } catch (RepositoryException exception) {
                    System.err.println(exception.toString());
                    Throwable cause = exception.getCause();
                    if (cause instanceof ShaclSailValidationException) {
                        ValidationReport validationReport = ((ShaclSailValidationException) cause).getValidationReport();
                        System.out.println("validationReport=" + validationReport);

                        Model validationReportModel = ((ShaclSailValidationException) cause).validationReportAsModel();
                        //Rio.write(validationReportModel, System.out, RDFFormat.TURTLE);
                        for (Statement st : validationReportModel.getStatements(null, null, null)) {
                            System.out.println("validationReportModel: " + st);
                        }
                    }
                    fail(exception.toString());
                }

                // let's check that our data is actually in the database
                try ( RepositoryResult<Statement> result = conn.getStatements(null, null, null);) {
                    for (Statement st : result) {
                        System.out.println("shacl inference: " + st);
                    }
                }

                // now lets load some invalid data
                conn.begin();
                StringReader invalidSampleData = new StringReader(
                        String.join("\n", "",
                                "@prefix : <http://foo.org/bar#> .",
                                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
                                ":Peter a :Man ;",
                                "  :age 20, \"30\"^^xsd:integer ." // two ages!
                        ));

                // Peter is defined as a Man, but the constraint is on Person,
                // so type inference needs to occur for this to work correctly!
                conn.add(invalidSampleData, "", RDFFormat.TURTLE);
                try {
                    conn.commit();
                    fail("An exception should be thrown!");
                } catch (RepositoryException exception) {
                    Throwable cause = exception.getCause();
                    assertTrue(cause instanceof ShaclSailValidationException, "Exception wrong type: " + cause.toString());
                    if (cause instanceof ShaclSailValidationException) {
                        ValidationReport validationReport = ((ShaclSailValidationException) cause).getValidationReport();
                        System.out.println("validationReport=" + validationReport);

                        Model validationReportModel = ((ShaclSailValidationException) cause).validationReportAsModel();
                        //Rio.write(validationReportModel, System.out, RDFFormat.TURTLE);
                        for (Statement st : validationReportModel.getStatements(null, null, null)) {
                            System.out.println("validationReportModel: " + st);
                        }
                    }
                }
            } finally {
                repo.shutDown();
            }
        }

        // 6) process the model to the Consty graph using the RDFUtilities.PopulateRecordStore()
    }

    private void loadTriples(Model model, InputStream inputStream, String baseURI, RDFFormat format) throws IOException {
        try ( GraphQueryResult res = QueryResults.parseGraphBackground(inputStream, baseURI, format)) {
            while (res.hasNext()) {
                //LOGGER.info("Processing next record...");

                final Statement st = res.next();
                model.add(st);

                final Map<String, String> namespaces = res.getNamespaces();
                final Resource subject = st.getSubject();
                final IRI predicate = st.getPredicate();
                final Value object = st.getObject();
                final Resource context = st.getContext();

                //LOGGER.log(Level.INFO, "Saw Subject: {0}, Predicate: {1}, Object: {2}, Context: {3}", new Object[]{subject, predicate, object, context});
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
    }

    /**
     * Learning how to use the RDF4J RDFS reasoners.
     *
     * @throws Exception
     */
    @Test
    public void testRdf4jRdfsReasoners() throws Exception {
        final GraphRecordStore results = new GraphRecordStore();
        final Model model = new LinkedHashModel();
        final ValueFactory VF = SimpleValueFactory.getInstance();
        final String baseURI = "http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl";

        // read the onology into a model
        {
            final URL documentUrl = getClass().getResource("./resources/univ-bench.owl");
            final InputStream inputStream = documentUrl.openStream();
            final RDFFormat format = RDFFormat.RDFXML;
            loadTriples(model, inputStream, baseURI, format);
        }

        // read the data into a model
        {
            final URL documentUrl = getClass().getResource("./resources/university0-0.owl");
            final InputStream inputStream = documentUrl.openStream();
            final RDFFormat format = RDFFormat.RDFXML;
            loadTriples(model, inputStream, baseURI, format);
        }

        int rawCount = 0;
        {
            // add it to a consty graph using RDFUtilities.PopulateRecordStore()
            // convert the graph into a model
            // load the model into an in RDF4J memory model
            System.out.println("load the model into an in RDF4J memory model");
            final Repository repo = new SailRepository(new MemoryStore());
            try ( RepositoryConnection conn = repo.getConnection()) {
                // add the model
                conn.add(model);

                // let's check that our data is actually in the database
                try ( RepositoryResult<Statement> result = conn.getStatements(null, null, null);) {
                    for (Statement st : result) {
                        rawCount++;
                        //System.out.println("db contains: " + st);
                    }
                    System.out.println("db raw count: " + rawCount);
                }
            } finally {
                repo.shutDown();
            }
        }

        int inferenceCount = 0;
        {
            // apply the RDFS inferencing rules
            final Repository repo = new SailRepository(new SchemaCachingRDFSInferencer(new MemoryStore(), true));
            try ( RepositoryConnection conn = repo.getConnection()) {
                // add the model
                conn.add(model);

                // let's check that our data is actually in the database
                try ( RepositoryResult<Statement> result = conn.getStatements(null, null, null);) {
                    for (Statement st : result) {
                        inferenceCount++;
                        //System.out.println("db now contains: " + st);
                    }
                    System.out.println("after inference count: " + inferenceCount);
                }
            }
        }

        // the inference engine should have created more triples
        assertTrue(inferenceCount > rawCount, inferenceCount + " > " + rawCount);
    }

    /**
     * Learning how to use the RDF4J RDFS reasoners against the LUBM data set.
     *
     * Note that some of the answers here are wrong, which is expected, since
     * RDF4J only implements RDFS, and not OWL, which is required for some of
     * the questions. We need OWL support to get the right answer.
     *
     * @throws Exception
     */
    @Test
    public void testRdf4jRdfsReasonersWithLUBM() throws Exception {
        final Model model = new LinkedHashModel();
        final ValueFactory VF = SimpleValueFactory.getInstance();
        final String baseURI = "http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl";

        // read the onology into a model
        {
            final URL documentUrl = getClass().getResource("./resources/univ-bench.owl");
            final InputStream inputStream = documentUrl.openStream();
            final RDFFormat format = RDFFormat.RDFXML;
            loadTriples(model, inputStream, baseURI, format);
        }

        // read the data into a model
        {
            final URL documentUrl = getClass().getResource("./resources/university0-0.owl");
            final InputStream inputStream = documentUrl.openStream();
            final RDFFormat format = RDFFormat.RDFXML;
            loadTriples(model, inputStream, baseURI, format);
        }

        // apply the RDFS inferencing rules
        final Repository repo = new SailRepository(
                new DedupingInferencer(
                        new DirectTypeHierarchyInferencer(
                                new SchemaCachingRDFSInferencer(
                                        new MemoryStore(), true))));
        try ( RepositoryConnection conn = repo.getConnection()) {
            // add the model
            conn.add(model);

            // let's check that our data is actually in the database
            try ( RepositoryResult<Statement> result = conn.getStatements(null, null, null);) {
                int count = 0;
                for (Statement st : result) {
                    count++;
                    //System.out.println("db contains: " + st);
                }
                System.out.println("inference count: " + count);
            }
            /*
            try (RepositoryResult<Statement> result = conn.getStatements(null, RDF.TYPE, null);) {
                for (Statement st : result) {
                    System.out.println(RDF.TYPE + ": " + st);
                }
            }
             */

            {
                // Query 1
                // This query bears large input and high selectivity. It queries about just one class and
                // one property and does not assume any hierarchy information or inference.
                TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, ""
                        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                        + "PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>\n"
                        + "SELECT ?X\n"
                        + "WHERE\n"
                        + "{\n"
                        + "  ?X ub:takesCourse <http://www.Department0.University0.edu/GraduateCourse0> .\n"
                        + "  ?X rdf:type ub:GraduateStudent .\n"
                        + "}");
                TupleQueryResult result = query.evaluate();
                List<BindingSet> expected = new ArrayList<>();
                expected.add(new ListBindingSet(Lists.newArrayList("X"), VF.createIRI("http://www.Department0.University0.edu/GraduateStudent44")));
                expected.add(new ListBindingSet(Lists.newArrayList("X"), VF.createIRI("http://www.Department0.University0.edu/GraduateStudent101")));
                expected.add(new ListBindingSet(Lists.newArrayList("X"), VF.createIRI("http://www.Department0.University0.edu/GraduateStudent124")));
                expected.add(new ListBindingSet(Lists.newArrayList("X"), VF.createIRI("http://www.Department0.University0.edu/GraduateStudent142")));
                assertEquals(result, expected);
            }

            {
                // Query 2
                // This query increases in complexity: 3 classes and 3 properties are involved. Additionally,
                // there is a triangular pattern of relationships between the objects involved.
                // Note: Modified GraduateStudent88 to make this work in sample data set.
                TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, ""
                        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                        + "PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>\n"
                        + "SELECT ?X ?Y ?Z\n"
                        + "WHERE\n"
                        + "{\n"
                        + "  ?X ub:memberOf ?Z .\n"
                        + "  ?Z ub:subOrganizationOf ?Y .\n"
                        + "  ?X ub:undergraduateDegreeFrom ?Y .\n"
                        + "  ?Y rdf:type ub:University .\n"
                        + "  ?Z rdf:type ub:Department .\n"
                        + "  ?X rdf:type ub:GraduateStudent .\n"
                        + "}");
                TupleCountHandler tupleCountHandler = new TupleCountHandler();
                query.evaluate(tupleCountHandler);
                assertEquals(tupleCountHandler.getCount(), 1);

                TupleQueryResult result = query.evaluate();
                List<BindingSet> expected = new ArrayList<>();
                MapBindingSet expected1 = new MapBindingSet();
                expected1.addBinding("X", VF.createIRI("http://www.Department0.University0.edu/GraduateStudent88"));
                expected1.addBinding("Y", VF.createIRI("http://www.University0.edu"));
                expected1.addBinding("Z", VF.createIRI("http://www.Department0.University0.edu"));
                expected.add(expected1);
                assertEquals(result, expected);
            }

            {
                // Query 3
                // This query is similar to Query 1 but class Publication has a wide hierarchy.
                TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, ""
                        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                        + "PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>\n"
                        + "SELECT ?X\n"
                        + "WHERE\n"
                        + "{\n"
                        + "  ?X ub:publicationAuthor <http://www.Department0.University0.edu/AssistantProfessor0> .\n"
                        + "  ?X rdf:type ub:Publication .\n"
                        + "}");
                TupleQueryResult result = query.evaluate();
                List<BindingSet> expected = new ArrayList<>();
                expected.add(new ListBindingSet(Lists.newArrayList("X"), VF.createIRI("http://www.Department0.University0.edu/AssistantProfessor0/Publication0")));
                expected.add(new ListBindingSet(Lists.newArrayList("X"), VF.createIRI("http://www.Department0.University0.edu/AssistantProfessor0/Publication1")));
                expected.add(new ListBindingSet(Lists.newArrayList("X"), VF.createIRI("http://www.Department0.University0.edu/AssistantProfessor0/Publication2")));
                expected.add(new ListBindingSet(Lists.newArrayList("X"), VF.createIRI("http://www.Department0.University0.edu/AssistantProfessor0/Publication3")));
                expected.add(new ListBindingSet(Lists.newArrayList("X"), VF.createIRI("http://www.Department0.University0.edu/AssistantProfessor0/Publication4")));
                expected.add(new ListBindingSet(Lists.newArrayList("X"), VF.createIRI("http://www.Department0.University0.edu/AssistantProfessor0/Publication5")));
                assertEquals(result, expected);
            }

            {
                // Query 4
                // This query has small input and high selectivity. It assumes subClassOf relationship
                // between Professor and its subclasses. Class Professor has a wide hierarchy. Another
                // feature is that it queries about multiple properties of a single class.
                TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, ""
                        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                        + "PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>\n"
                        + "SELECT ?X ?Y1 ?Y2 ?Y3\n"
                        + "WHERE\n"
                        + "{\n"
                        + "  ?X ub:worksFor <http://www.Department0.University0.edu> .\n"
                        + "  ?X rdf:type ub:Professor .\n"
                        + "  ?X ub:name ?Y1 .\n"
                        + "  ?X ub:emailAddress ?Y2 .\n"
                        + "  ?X ub:telephone ?Y3 .\n"
                        + "}");
                TupleCountHandler tupleCountHandler = new TupleCountHandler();
                query.evaluate(tupleCountHandler);
                assertEquals(tupleCountHandler.getCount(), 34);
            }

            {
                // Query 5
                // This query assumes subClassOf relationship between Person and its subclasses
                // and subPropertyOf relationship between memberOf and its subproperties.
                // Moreover, class Person features a deep and wide hierarchy.
                TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, ""
                        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                        + "PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>\n"
                        + "SELECT ?X\n"
                        + "WHERE\n"
                        + "{\n"
                        + "  ?X ub:memberOf <http://www.Department0.University0.edu> .\n"
                        + "  ?X rdf:type ub:Person .\n"
                        + "}");
                TupleCountHandler tupleCountHandler = new TupleCountHandler();
                query.evaluate(tupleCountHandler);
                assertEquals(tupleCountHandler.getCount(), 719);
            }

            {
                // Query 6
                // This query queries about only one class. But it assumes both the explicit
                // subClassOf relationship between UndergraduateStudent and Student and the
                // implicit one between GraduateStudent and Student. In addition, it has large
                // input and low selectivity.
                TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, ""
                        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                        + "PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>\n"
                        + "SELECT (COUNT(?X) AS ?count) WHERE {?X rdf:type ub:Student}");
                TupleQueryResult result = query.evaluate();
                String actual = result.iterator().next().getValue("count").stringValue();
                // TODO: This is expected behaviour but the answer is incorrect.
                // This inference engine doesn't support the implicit OWL relationship
                // required between GraduateStudent and Student, so this number should be larger.
                assertEquals(actual, "571");
            }

            {
                // Query 7
                // This query is similar to Query 6 in terms of class Student but it increases in the
                // number of classes and properties and its selectivity is high.
                TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, ""
                        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                        + "PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>\n"
                        + "SELECT ?X ?Y\n"
                        + "WHERE \n"
                        + "{\n"
                        + "  <http://www.Department0.University0.edu/AssociateProfessor0> ub:teacherOf ?Y .\n"
                        + "  ?Y rdf:type ub:Course .\n"
                        + "  ?X ub:takesCourse ?Y .\n"
                        + "  ?X rdf:type ub:Student .\n"
                        + "}");
                TupleCountHandler tupleCountHandler = new TupleCountHandler();
                query.evaluate(tupleCountHandler);
                assertEquals(tupleCountHandler.getCount(), 61);
            }

            {
                // Query 8
                // This query is further more complex than Query 7 by including one more property.
                TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, ""
                        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                        + "PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>\n"
                        + "SELECT ?X ?Y ?Z\n"
                        + "WHERE\n"
                        + "{\n"
                        + "  ?Y ub:subOrganizationOf <http://www.University0.edu> .\n"
                        + "  ?Y rdf:type ub:Department .\n"
                        + "  ?X ub:memberOf ?Y .\n"
                        + "  ?X rdf:type ub:Student .\n"
                        + "  ?X ub:emailAddress ?Z .\n"
                        + "}");
                TupleCountHandler tupleCountHandler = new TupleCountHandler();
                query.evaluate(tupleCountHandler);
                assertEquals(tupleCountHandler.getCount(), 571);
            }

            {
                // Query 9
                // Besides the aforementioned features of class Student and the wide hierarchy of
                // class Faculty, like Query 2, this query is characterized by the most classes and
                // properties in the query set and there is a triangular pattern of relationships.
                TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, ""
                        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                        + "PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>\n"
                        + "SELECT ?X ?Y ?Z\n"
                        + "WHERE\n"
                        + "{\n"
                        + "  ?X ub:advisor ?Y .\n"
                        + "  ?Y ub:teacherOf ?Z .\n"
                        + "  ?X ub:takesCourse ?Z .\n"
                        + "  ?X rdf:type ub:Student .\n"
                        + "  ?Z rdf:type ub:Course .\n"
                        + "  ?Y rdf:type ub:Faculty .\n"
                        + "}\n"
                        + "LIMIT 100");
                TupleCountHandler tupleCountHandler = new TupleCountHandler();
                query.evaluate(tupleCountHandler);
                assertEquals(tupleCountHandler.getCount(), 8);

                TupleQueryResult result = query.evaluate();
                MapBindingSet expected1 = new MapBindingSet();
                expected1.addBinding("X", VF.createIRI("http://www.Department0.University0.edu/UndergraduateStudent275"));
                expected1.addBinding("Y", VF.createIRI("http://www.Department0.University0.edu/FullProfessor1"));
                expected1.addBinding("Z", VF.createIRI("http://www.Department0.University0.edu/Course1"));
                assertEquals(result.iterator().next(), expected1);
            }

            {
                // Query 10
                // This query differs from Query 6, 7, 8 and 9 in that it only requires the
                // (implicit) subClassOf relationship between GraduateStudent and Student, i.e.,
                // subClassOf relationship between UndergraduateStudent and Student does not add
                // to the results.
                TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, ""
                        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                        + "PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>\n"
                        + "SELECT ?X\n"
                        + "WHERE\n"
                        + "{\n"
                        + "  ?X ub:takesCourse <http://www.Department0.University0.edu/GraduateCourse0> .\n"
                        + "  ?X rdf:type ub:Student .\n"
                        + "}");
                TupleCountHandler tupleCountHandler = new TupleCountHandler();
                query.evaluate(tupleCountHandler);
                // TODO: This is expected behaviour but the answer is incorrect.
                // This inference engine doesn't support the implicit OWL relationship
                // required between GraduateStudent and Student.
                assertEquals(tupleCountHandler.getCount(), 0);
            }

            {
                // Query 11
                // Query 11, 12 and 13 are intended to verify the presence of certain OWL reasoning
                // capabilities in the system. In this query, property subOrganizationOf is defined
                // as transitive. Since in the benchmark data, instances of ResearchGroup are stated
                // as a sub-organization of a Department individual and the later suborganization of
                // a University individual, inference about the subOrgnizationOf relationship between
                // instances of ResearchGroup and University is required to answer this query.
                // Additionally, its input is small.
                TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, ""
                        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                        + "PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>\n"
                        + "SELECT ?X\n"
                        + "WHERE\n"
                        + "{\n"
                        + "  ?X ub:subOrganizationOf <http://www.University0.edu> .\n"
                        + "  ?X rdf:type ub:ResearchGroup .\n"
                        + "}");
                TupleCountHandler tupleCountHandler = new TupleCountHandler();
                query.evaluate(tupleCountHandler);
                // TODO: OWL not supported.
                assertEquals(tupleCountHandler.getCount(), 0);
            }

            {
                // Query 12
                // The benchmark data do not produce any instances of class Chair. Instead, each
                // Department individual is linked to the chair professor of that department by
                // property headOf. Hence this query requires realization, i.e., inference that
                // that professor is an instance of class Chair because he or she is the head of a
                // department. Input of this query is small as well.
                TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, ""
                        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                        + "PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>\n"
                        + "SELECT ?X ?Y\n"
                        + "WHERE\n"
                        + "{\n"
                        + "  ?X ub:worksFor ?Y .\n"
                        + "  ?Y ub:subOrganizationOf <http://www.University0.edu> .\n"
                        + "  ?X rdf:type ub:Chair .\n"
                        + "?Y rdf:type ub:Department .\n"
                        + "}\n"
                        + "LIMIT 100");
                TupleCountHandler tupleCountHandler = new TupleCountHandler();
                query.evaluate(tupleCountHandler);
                // TODO: OWL not supported.
                assertEquals(tupleCountHandler.getCount(), 0);
            }

            {
                // Query 13
                // Property hasAlumnus is defined in the benchmark ontology as the inverse of
                // property degreeFrom, which has three subproperties: undergraduateDegreeFrom,
                // mastersDegreeFrom, and doctoralDegreeFrom. The benchmark data state a person as
                // an alumnus of a university using one of these three subproperties instead of
                // hasAlumnus. Therefore, this query assumes subPropertyOf relationships between
                // degreeFrom and its subproperties, and also requires inference about inverseOf.
                TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, ""
                        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                        + "PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>\n"
                        + "SELECT ?X\n"
                        + "WHERE\n"
                        + "{\n"
                        + "  <http://www.University0.edu> ub:hasAlumnus ?X .\n"
                        + "  ?X rdf:type ub:Person .\n"
                        + "}\n"
                        + "LIMIT 10000");
                TupleCountHandler tupleCountHandler = new TupleCountHandler();
                query.evaluate(tupleCountHandler);
                // TODO: OWL not supported.
                assertEquals(tupleCountHandler.getCount(), 0);
            }

            {
                // Query 14
                // This query is the simplest in the test set. This query represents
                // those with large input and low selectivity and does not assume any
                // hierarchy information or inference.
                TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, ""
                        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                        + "PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>\n"
                        + "SELECT (COUNT(?X) as ?count)\n"
                        + "WHERE {?X rdf:type ub:UndergraduateStudent}");
                TupleQueryResult result = query.evaluate();
                String actual = result.iterator().next().getValue("count").stringValue();
                assertEquals(actual, "532");
            }

        } finally {
            repo.shutDown();
        }

        // process the model to the Consty graph using the RDFUtilities.PopulateRecordStore()
    }

    /**
     * Planning for Plugins 1 and 2.
     *
     * @throws Exception
     */
    @Test
    public void testPlugin1and2() throws Exception {
// Pseudo code:
//        Model model = new LinkedHashModel()); or Model model = new ConstellationSailRepo();
//        model.add(file or sparql);
//        model.add(Utilities.toRDF4J(constellationGraph));
//        connnection = model.getConnection();
//        connnection.runInferencrer(Duplicate Removal, RDFS, Direct Type, Duplicate Removal);
//        constellation.insert(Utilities.toConstallation(connection.getTriples()));
//        model = null;

        final Model model = new LinkedHashModel();
        final ValueFactory VF = SimpleValueFactory.getInstance();
        final String baseURI = "http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl";

        // read the onology into a model
        {
            final URL documentUrl = getClass().getResource("./resources/univ-bench.owl");
            final InputStream inputStream = documentUrl.openStream();
            final RDFFormat format = RDFFormat.RDFXML;
            loadTriples(model, inputStream, baseURI, format);
        }

        // read the data into a model
        {
            final URL documentUrl = getClass().getResource("./resources/university0-0.owl");
            final InputStream inputStream = documentUrl.openStream();
            final RDFFormat format = RDFFormat.RDFXML;
            loadTriples(model, inputStream, baseURI, format);
        }

        // TODO: This should take in a Constellation Graph instance I'm guessing...
        final ConstellationSail sail = new ConstellationSail();

        // apply the RDFS inferencing rules to a Constellation SAIL !
        final Repository repo = new SailRepository(
                new DedupingInferencer(
                        new DirectTypeHierarchyInferencer(
                                new SchemaCachingRDFSInferencer(sail, true))));

        // Add records via RDF4J connection
        try ( RepositoryConnection conn = repo.getConnection()) {
            // add the model
            conn.add(model);
        } finally {
            //repo.shutDown();
        }

        // TODO: Add records to RDF4J by adding to Constellation graph. Doesn't work yet.
//        final RecordStore recordStore = new HookRecordStore(new GraphRecordStore(), sail);
//        recordStore.add();
//        recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, "some string");
//        recordStore.close();
        // Alternatively, use the GraphChangeListener
    }

    @Test
    public void testNullChangeDoesNotAddToModel() throws Exception {
        final ConstellationSail sail = new ConstellationSail();
        sail.initialize();

        // new consty graph
        final Schema schema = SchemaFactoryUtilities.getSchemaFactory(AnalyticSchemaFactory.ANALYTIC_SCHEMA_ID).createSchema();
        final Graph graph = new DualGraph(schema);
        sail.newActiveGraph(graph);

        // bootstrap the Sail API
        final Repository repo = new SailRepository(
                new DedupingInferencer(
                        new DirectTypeHierarchyInferencer(
                                new SchemaCachingRDFSInferencer(sail, true)
                        )
                )
        );

        // confirm we have an empty model
        Assert.equals(0, sail.getModel().size());

        sail.graphChanged(new GraphChangeEvent(null, graph, null, null));

        // wait for the RDF memory model to update
        Thread.sleep(1000);

        Assert.equals(0, sail.getModel().size());
    }

    @Test
    public void testGraphCanAddToModel() throws Exception {
        final ConstellationSail sail = new ConstellationSail();
        sail.initialize();

        // new consty graph
        final Schema schema = SchemaFactoryUtilities.getSchemaFactory(AnalyticSchemaFactory.ANALYTIC_SCHEMA_ID).createSchema();
        final Graph graph = new DualGraph(schema);
        sail.newActiveGraph(graph);

        // bootstrap the Sail API
        final Repository repo = new SailRepository(
                new DedupingInferencer(
                        new DirectTypeHierarchyInferencer(
                                new SchemaCachingRDFSInferencer(sail, true)
                        )
                )
        );

        // confirm we have an empty model
        Assert.equals(0, sail.getModel().size());

        // confirm we have an empty graph
        {
            final ReadableGraph readableGraph = graph.getReadableGraph();
            try {
                Assert.equals(0, readableGraph.getVertexCount());
                Assert.equals(0, readableGraph.getTransactionCount());
            } finally {
                readableGraph.release();
            }
        }

        // add some nodes to the in-memory model
        {
            final WritableGraph writableGraph = graph.getWritableGraph("Add nodes", true);
            try {
                final int vertexIdentifierAttributeId = VisualConcept.VertexAttribute.IDENTIFIER.ensure(writableGraph);
                final int vertexTypeAttributeId = AnalyticConcept.VertexAttribute.TYPE.ensure(writableGraph);
                final int vertexSourceAttributeId = AnalyticConcept.VertexAttribute.SOURCE.ensure(writableGraph);
                final int transactionTypeAttributeId = AnalyticConcept.TransactionAttribute.TYPE.ensure(writableGraph);
                final int transactionSourceAttributeId = AnalyticConcept.TransactionAttribute.SOURCE.ensure(writableGraph);

                final int vxId0 = writableGraph.addVertex();
                writableGraph.setStringValue(vertexIdentifierAttributeId, vxId0, "someone@from.com");
                writableGraph.setStringValue(vertexTypeAttributeId, vxId0, "Email");
                writableGraph.setStringValue(vertexSourceAttributeId, vxId0, "local");

                final int vxId1 = writableGraph.addVertex();
                writableGraph.setStringValue(vertexIdentifierAttributeId, vxId1, "someone@to.com");
                writableGraph.setStringValue(vertexTypeAttributeId, vxId1, "Email");
                writableGraph.setStringValue(vertexSourceAttributeId, vxId1, "local");

                final int txId0 = writableGraph.addTransaction(vxId0, vxId1, true);
                writableGraph.setStringValue(transactionTypeAttributeId, txId0, "Communication");
                writableGraph.setStringValue(transactionSourceAttributeId, txId0, "local");
            } finally {
                writableGraph.commit();
            }
        }

        // wait for the RDF memory model to update
        Thread.sleep(1000);

        Assert.equals(3, sail.getModel().size());

        repo.shutDown();
    }

    @Test
    public void testRdfModelCanAddToGraph() throws Exception {
        final ConstellationSail sail = new ConstellationSail();
        sail.initialize();

        // new consty graph
        final Schema schema = SchemaFactoryUtilities.getSchemaFactory(AnalyticSchemaFactory.ANALYTIC_SCHEMA_ID).createSchema();
        final Graph graph = new DualGraph(schema);
        sail.newActiveGraph(graph);

        // bootstrap the Sail API
        final Repository repo = new SailRepository(
                new DedupingInferencer(
                        new DirectTypeHierarchyInferencer(
                                new SchemaCachingRDFSInferencer(sail, true)
                        )
                )
        );

        // confirm we have an empty graph
        {
            final ReadableGraph readableGraph = graph.getReadableGraph();
            try {
                Assert.equals(0, readableGraph.getVertexCount());
                Assert.equals(0, readableGraph.getTransactionCount());
            } finally {
                readableGraph.release();
            }
        }

        // setting up the connection first time adds some baseline triples
        try ( RepositoryConnection conn = repo.getConnection()) {
            Assert.isTrue(repo.isInitialized());
        } finally {
//            repo.shutDown();
        }

        Assert.equals(141, sail.getModel().size());

        final Model model = new LinkedHashModel();
        final String baseURI = "http://foo.org/bar#";

        // read a simple file into a model
        {
            final URL documentUrl = getClass().getResource("./resources/simple.ttl");
            final InputStream inputStream = documentUrl.openStream();
            final RDFFormat format = RDFFormat.TURTLE;
            loadTriples(model, inputStream, baseURI, format);
        }

        Assert.equals(2, model.size());
        Assert.equals(141, sail.getModel().size());

        // Add records via RDF4J connection
        try ( RepositoryConnection conn = repo.getConnection()) {
            // add the model
            conn.add(model);
        } finally {
//            repo.shutDown();
        }

        Assert.equals(210, sail.getModel().size()); // TODO: There should be more, its small because of the concurrent mod exception

        // confirm no sync has happened just yet
        {
            final ReadableGraph readableGraph = graph.getReadableGraph();
            try {
                Assert.equals(0, readableGraph.getVertexCount());
                Assert.equals(0, readableGraph.getTransactionCount());
            } finally {
                readableGraph.release();
            }
        }

        sail.writeModelToGraph();

        SaveGraphUtilities.saveGraphToTemporaryDirectory(graph, "after_synced_with_model", true);

        // confirm the graph now has records
        {
            final ReadableGraph readableGraph = graph.getReadableGraph();
            try {
                Assert.equals(347, readableGraph.getVertexCount()); // TODO: These numbers are wrong because of the concurrent mod exception
                Assert.equals(137, readableGraph.getTransactionCount()); // TODO: These numbers are wrong because of the concurrent mod exception
            } finally {
                readableGraph.release();
            }
        }

        Assert.equals(359, sail.getModel().size()); // TODO: There should be more, its small because of the concurrent mod exception
    }

    /**
     * Planning for Plugins 1 and 2.
     *
     * Note that this unit test can replace testPlugin1and2 when this works.
     *
     * Note that this is WIP
     *
     * @throws Exception
     */
    @Test
    public void testPlugin1and2Simplified() throws Exception {
// Pseudo code:
//        Model model = new LinkedHashModel()); or Model model = new ConstellationSailRepo();
//        model.add(file or sparql);
//        model.add(Utilities.toRDF4J(constellationGraph));
//        connnection = model.getConnection();
//        connnection.runInferencrer(Duplicate Removal, RDFS, Direct Type, Duplicate Removal);
//        constellation.insert(Utilities.toConstallation(connection.getTriples()));
//        model = null;

        final ConstellationSail sail = new ConstellationSail();
        sail.initialize();

        // new consty graph
        final Schema schema = SchemaFactoryUtilities.getSchemaFactory(AnalyticSchemaFactory.ANALYTIC_SCHEMA_ID).createSchema();
        final Graph graph = new DualGraph(schema);
        sail.newActiveGraph(graph);

        // bootstrap the Sail API
        final Repository repo = new SailRepository(
                new DedupingInferencer(
                        new DirectTypeHierarchyInferencer(
                                new SchemaCachingRDFSInferencer(sail, true)
                        )
                )
        );

        Assert.equals(0, sail.getModel().size());

        // setting up the connection first time adds some baseline triples
        try ( RepositoryConnection conn = repo.getConnection()) {
            Assert.isTrue(repo.isInitialized());
        } finally {
//            repo.shutDown();
        }

        Assert.equals(141, sail.getModel().size());

        final Model model = new LinkedHashModel();
        final String baseURI = "http://foo.org/bar#";

        // read the onology into a model
        {
            final URL documentUrl = getClass().getResource("./resources/simple.ttl");
            final InputStream inputStream = documentUrl.openStream();
            final RDFFormat format = RDFFormat.TURTLE;
            loadTriples(model, inputStream, baseURI, format);
        }

        Assert.equals(2, model.size());

        // Add records via RDF4J connection
        try ( RepositoryConnection conn = repo.getConnection()) {
            // add the model
            conn.add(model);
        } finally {
//            repo.shutDown();
        }

        Assert.equals(210, sail.getModel().size());// TODO: getting 210 because of the concurrent mod exception

        // TODO: how can you apply an inferencing rule to the new sail connection?
        // apply the custom inferencing rules
        {
            System.out.println("5.2) apply custom inferencing");
            String pre = "PREFIX : <http://foo.org/bar#>\n";
            String rule = pre + "CONSTRUCT { ?p :relatesTo :Cryptography } WHERE "
                    + "{ { :Bob ?p :Alice } UNION { :Alice ?p :Bob } }";
            String match = pre + "CONSTRUCT { ?p :relatesTo :Cryptography } "
                    + "WHERE { ?p :relatesTo :Cryptography }";

//            final Repository repo = new SailRepository(new CustomGraphQueryInferencer(new MemoryStore(), QueryLanguage.SPARQL, rule, match));
            try ( RepositoryConnection conn = repo.getConnection()) {
                // add the model
                conn.add(model);

                // let's check that our data is actually in the database
                try ( RepositoryResult<Statement> result = conn.getStatements(null, null, null);) {
                    for (Statement st : result) {
                        System.out.println("custom inference: " + st);
                    }
                }

                // check the new triples are added
                try ( RepositoryResult<Statement> result = conn.getStatements(
                        null,
                        sail.getValueFactory().createIRI("http://foo.org/bar#relatesTo"),
                        sail.getValueFactory().createIRI("http://foo.org/bar#Cryptography"));) {
//                    assertTrue(result.hasNext());
//                    assertEquals(result.next().getSubject().stringValue(), "http://foo.org/bar#sendsMessageTo");
//                    assertTrue(result.hasNext());
//                    assertEquals(result.next().getSubject().stringValue(), "http://foo.org/bar#exchangesKeysWith");
//                    assertFalse(result.hasNext());
                }
            } finally {
//                repo.shutDown();
            }
        }

        // apply the model to the graph
        sail.writeModelToGraph();

        // the graph was updated
        {
            final ReadableGraph readableGraph = graph.getReadableGraph();
            try {
//                Assert.equals(?, readableGraph.getVertexCount());
//                Assert.equals(?, readableGraph.getTransactionCount());
            } finally {
                readableGraph.release();
            }
        }

        repo.shutDown();
    }

    /**
     * Learning how to use the OWL API reasoners.
     *
     * TODO: How do we run a SPARQL query against an OWL-API Model?
     *
     * @throws Exception
     */
    @Test
    public void testOwlApiReasonersPlugin3() throws Exception {
        final GraphRecordStore results = new GraphRecordStore();
        final Model model = new LinkedHashModel();
        final ValueFactory VF = SimpleValueFactory.getInstance();
        final String baseURI = "http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl";
        final OWLOntologyManager manager;
        final OWLOntology ontology;

        // read the onology into a model
        {
            //final URL documentUrl = getClass().getResource("./resources/univ-bench.owl");
            final URL documentUrl = getClass().getResource("./resources/university0-0.owl");
            final InputStream inputStream = documentUrl.openStream();
            final RDFFormat format = RDFFormat.RDFXML;

            manager = OWLManager.createOWLOntologyManager();
            ontology = manager.loadOntologyFromOntologyDocument(inputStream);
            assertNotNull(ontology);
            System.out.println("Ontology: " + ontology);
        }

        // read the data into a model
//        {
//            final URL documentUrl = getClass().getResource("./resources/university0-0.owl");
//            final InputStream inputStream = documentUrl.openStream();
//            final RDFFormat format = RDFFormat.RDFXML;
//
//            OWLOntology o = manager.loadOntologyFromOntologyDocument(inputStream);
//            assertNotNull(o);
//            System.out.println("Data: " + o);
//            ontology.addAxioms(o.axioms());
//        }
//        System.out.println("Combined: " + ontology);
        // get and configure a reasoner (HermiT)
        OWLReasonerFactory reasonerFactory = new ReasonerFactory();
        ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
        OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor);

        OWLDataFactory factory = manager.getOWLDataFactory();

        // load the ontology to the reasoner
        //Hermi reasoner = com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory.getInstance().createReasoner(ontology);
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);

        // read the data into a model
//        {
//            final URL documentUrl = getClass().getResource("./resources/university0-0.owl");
//            final InputStream inputStream = documentUrl.openStream();
//            final RDFFormat format = RDFFormat.RDFXML;
//            loadTriples(model, inputStream, baseURI, format);
//            OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();
//            Iterable<Statement> iterable = model.getStatements(null, null, null);
//            for (Statement s : iterable) {
//                OWLNamedIndividual subject = df.getOWLNamedIndividual(org.semanticweb.owlapi.model.IRI.create(s.getSubject().stringValue()));
//                OWLNamedIndividual predicate = df.getOWLNamedIndividual(org.semanticweb.owlapi.model.IRI.create(s.getPredicate().stringValue()));
//                OWLNamedIndividual object = df.getOWLNamedIndividual(org.semanticweb.owlapi.model.IRI.create(s.getObject().stringValue()));
//                //OWLAxiom axiom = df.(subject, object);
//                //ontology.add(axiom);
//            }
//        }
        // create property and resources to query the reasoner
        OWLClass Student = factory.getOWLClass(org.semanticweb.owlapi.model.IRI.create("http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#Student"));
        OWLDataProperty studentName = factory.getOWLDataProperty(org.semanticweb.owlapi.model.IRI.create("http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#name"));
        OWLDataProperty emailAddress = factory.getOWLDataProperty(org.semanticweb.owlapi.model.IRI.create("http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#emailAddress"));
        OWLObjectProperty takesCourse = factory.getOWLObjectProperty(org.semanticweb.owlapi.model.IRI.create("http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#takesCourse"));

        // get all instances of Person class
        Set<OWLNamedIndividual> individuals = reasoner.getInstances(Student, false).getFlattened();
        int count = 0;
        for (OWLNamedIndividual ind : individuals) {
            count++;
            System.out.println("------------------------------------");

            // get the info about this specific individual
            Set<OWLLiteral> names = reasoner.getDataPropertyValues(ind, studentName);
            Set<OWLLiteral> emailAddrs = reasoner.getDataPropertyValues(ind, emailAddress);
            NodeSet<OWLClass> types = reasoner.getTypes(ind, true);
            NodeSet<OWLNamedIndividual> courses = reasoner.getObjectPropertyValues(ind, takesCourse);

            // we know there is a single name for each person so we can get that value directly
            String name = names.iterator().next().getLiteral();
            System.out.println("Name:     " + name);

            // we know there is a single name for each person so we can get that value directly
            String emailAddr = emailAddrs.iterator().next().getLiteral();
            System.out.println("Email:    " + emailAddr);

            // at least one direct type is guaranteed to exist for each individual
            OWLClass type = types.iterator().next().getRepresentativeElement();
            System.out.println("Type:     " + type.getIRI().getShortForm());
            System.out.print("Types:   ");
            for (Node<OWLClass> typeNode : types) {
                System.out.print(" " + typeNode.getRepresentativeElement().getIRI().getShortForm());
            }
            System.out.println();

            // there may be zero or more homepages so check first if there are any found
            if (courses.isEmpty()) {
                System.out.print("Courses:  None");
            } else {
                System.out.print("Courses: ");
                for (Node<OWLNamedIndividual> course : courses) {
                    System.out.print(" " + course.getRepresentativeElement().getIRI().getShortForm());
                }
            }
            System.out.println();
        }
        System.out.println("------------------------------------");
        System.out.println("Query 6:  " + count); // This is equiv to LUBM Query 6
        assertEquals(count, 678); // 532 + 146 is the correct answer. Awesome!

        // Test serialising the ontology back to triples
        ontology.saveOntology(System.out);
    }

    private class TupleCountHandler extends TupleQueryResultBuilder {

        public int getCount() {
            final AtomicInteger count = new AtomicInteger(0);
            getQueryResult().forEach(bs -> {
                count.incrementAndGet();
            });
            return count.get();
        }

    }

}
