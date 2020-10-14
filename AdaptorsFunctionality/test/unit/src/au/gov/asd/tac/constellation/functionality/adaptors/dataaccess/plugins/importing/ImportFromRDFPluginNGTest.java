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
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import java.io.IOException;
import java.io.InputStream;
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
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.inferencer.fc.CustomGraphQueryInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.openide.util.Exceptions;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

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
     * Learning how to use the RDF4J Model and Inferencing class
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
            loadTriples(model, inputStream, baseURI, format);
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
        System.out.println("5) apply the inferencing rule");
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

    private void loadTriples(Model model, InputStream inputStream, String baseURI, RDFFormat format) throws IOException {
        try (GraphQueryResult res = QueryResults.parseGraphBackground(inputStream, baseURI, format)) {
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
        try {
            final URL documentUrl = new URL("file://" + this.getClass().getResource("./resources/univ-bench.owl").getFile());
            final InputStream inputStream = documentUrl.openStream();
            final RDFFormat format = RDFFormat.RDFXML;
            loadTriples(model, inputStream, baseURI, format);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        // read the data into a model
        try {
            final URL documentUrl = new URL("file://" + this.getClass().getResource("./resources/university0-0.owl").getFile());
            final InputStream inputStream = documentUrl.openStream();
            final RDFFormat format = RDFFormat.RDFXML;
            loadTriples(model, inputStream, baseURI, format);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        // add it to a consty graph using RDFUtilities.PopulateRecordStore()
        // convert the graph into a model
        // load the model into an in RDF4J memory model
        System.out.println("load the model into an in RDF4J memory model");
        final Repository repo = new SailRepository(new MemoryStore());
        try (RepositoryConnection conn = repo.getConnection()) {
            // add the model
            conn.add(model);

            // let's check that our data is actually in the database
            try (RepositoryResult<Statement> result = conn.getStatements(null, null, null);) {
                int count = 0;
                for (Statement st : result) {
                    count++;
                    //System.out.println("db contains: " + st);
                }
                System.out.println("db count: " + count);
            }
        } finally {
            repo.shutDown();
        }

        // apply the inferenceing rule
        final Repository repo2 = new SailRepository(new SchemaCachingRDFSInferencer(new MemoryStore(), true));
        try (RepositoryConnection conn = repo2.getConnection()) {
            // add the model
            conn.add(model);

            // let's check that our data is actually in the database
            try (RepositoryResult<Statement> result = conn.getStatements(null, null, null);) {
                int count = 0;
                for (Statement st : result) {
                    count++;
                    //System.out.println("db now contains: " + st);
                }
                System.out.println("after inference count: " + count);
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
            repo2.shutDown();
        }

        // process the model to the Consty graph using the RDFUtilities.PopulateRecordStore()
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
