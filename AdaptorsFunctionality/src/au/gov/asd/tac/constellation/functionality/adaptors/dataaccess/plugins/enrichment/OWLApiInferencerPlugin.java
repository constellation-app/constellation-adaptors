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
package au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.enrichment;

import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.DataAccessPluginAdaptorType;
import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.utilities.RDFUtilities;
import au.gov.asd.tac.constellation.graph.Graph;
import au.gov.asd.tac.constellation.graph.ReadableGraph;
import au.gov.asd.tac.constellation.graph.manager.GraphManager;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import au.gov.asd.tac.constellation.plugins.Plugin;
import au.gov.asd.tac.constellation.plugins.PluginException;
import au.gov.asd.tac.constellation.plugins.PluginInfo;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.PluginType;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredClassAssertionAxiomGenerator;
import org.semanticweb.owlapi.util.InferredDataPropertyCharacteristicAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredInverseObjectPropertiesAxiomGenerator;
import org.semanticweb.owlapi.util.InferredObjectPropertyCharacteristicAxiomGenerator;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.InferredPropertyAssertionGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredSubDataPropertyAxiomGenerator;
import org.semanticweb.owlapi.util.InferredSubObjectPropertyAxiomGenerator;

@ServiceProviders({
    @ServiceProvider(service = DataAccessPlugin.class),
    @ServiceProvider(service = Plugin.class)})
@PluginInfo(pluginType = PluginType.IMPORT, tags = {"ENRICH"})
@NbBundle.Messages("OWLApiInferencerPlugin=Inferring with the OWL API")
public class OWLApiInferencerPlugin extends RecordStoreQueryPlugin implements DataAccessPlugin { //extends SimpleEditPlugin

    private static final Logger LOGGER = Logger.getLogger(OWLApiInferencerPlugin.class.getName());

    final private static int layer_Mask = 9;

    final Map<String, String> subjectToType = new HashMap<>();
    final Map<String, String> bnodeToSubject = new HashMap<>();

    @Override
    //public void edit(GraphWriteMethods graph, final PluginInteraction interaction, final PluginParameters parameters) throws InterruptedException, PluginException {
    protected RecordStore query(RecordStore query, PluginInteraction interaction, PluginParameters parameters) throws InterruptedException, PluginException {
        GraphRecordStore recordStore = new GraphRecordStore();
        //populate model from query or the whole graph as required
        //Model model = RDFUtilities.getGraphModel(graph);

        final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        final OWLOntology ontology;

        try {
            final File tempFile = File.createTempFile("TempConstyFile", ".owl");
            //final IRI iri2 = IRI.create("http://protege.stanford.edu/ontologies/pizza/pizza.owl");

            //TEST
            // a collection of several RDF statements
            //FileOutputStream out2 = new FileOutputStream("https://protege.stanford.edu/ontologies/pizza/pizza.owl"); //iri.isIRI()
//                Model model2 = new LinkedHashModel();;
//                Rio.write(model2, out2, RDFFormat.RDFXML);
            java.net.URL documentUrl = new URL("https://protege.stanford.edu/ontologies/pizza/pizza.owl");
            InputStream inputStream = documentUrl.openStream();
            Model model2 = Rio.parse(inputStream, documentUrl.toString(), RDFFormat.RDFXML);
            LOGGER.log(Level.INFO, "Direct Model {0}", model2);
            LOGGER.log(Level.INFO, "Direct Model size before inferencing is {0}", model2.size());

            //END TEST
//            ontology = manager.loadOntologyFromOntologyDocument(iri);
            Graph graph = GraphManager.getDefault().getActiveGraph();

            //----------------------------Create the model from RDFUtilities (work)
            final ReadableGraph readableGraph = graph.getReadableGraph();
            try {
                final Model graphModel = RDFUtilities.getGraphModel(readableGraph);
                LOGGER.log(Level.INFO, "Model {0}", graphModel);
                LOGGER.log(Level.INFO, "Model size before inferencing is {0}", graphModel.size());
                //Save the model in a temp file
                FileOutputStream out = new FileOutputStream(tempFile.getPath()); //"/path/to/file.rdf"
                try {
                    Rio.write(graphModel, out, RDFFormat.RDFXML); //model2 to test with the hard coded file
                } finally {
                    out.close();
                }
            } catch (NullPointerException | IllegalArgumentException e) {
                LOGGER.warning("Exception : " + e);
                //TODO DISPLAY AN ERROR AND SKIP PROCESSING FURTHER
            } finally {
                readableGraph.release();
            }
            //----------------------------Access the model from the Sail (model returned is empty)
//            final ConstellationSail sail = new ConstellationSail();
//            sail.initialize();
//
//            // new consty graph
//            //final Schema schema = SchemaFactoryUtilities.getSchemaFactory(AnalyticSchemaFactory.ANALYTIC_SCHEMA_ID).createSchema();
//            //final Graph graph = new DualGraph(schema);
//            sail.newActiveGraph(graph);
//
//            // bootstrap the Sail API
////                final Repository repo = new SailRepository(
////                        new DedupingInfIerencer(
////                                new DirectTypeHierarchyInferencer(
////                                        new SchemaCachingRDFSInferencer(sail, true)
////                                )
////                        )
////                );
//            Model graphModel = sail.getModel();
//            //----------------
//            LOGGER.log(Level.INFO, "Model size before inferencing is {0}", graphModel.size());
//            //Save the model in a temp file
//            FileOutputStream out = new FileOutputStream(tempFile.getPath()); //"/path/to/file.rdf"
//            try {
//                Rio.write(graphModel, out, RDFFormat.RDFXML);
//            } finally {
//                out.close();
//            }
            //----------------------------------END -Access the model from the Sail

            //Load the ontology from the saved temp file
            final IRI iri = IRI.create("file:/" + tempFile.getPath().replaceAll("\\\\", "/"));
            ontology = manager.loadOntologyFromOntologyDocument(iri);

            //--------------------------------------- Merge 2 univ files to infer
//            String inputFilename = "C:\\Projects\\constellation-adaptors\\AdaptorsFunctionality\\src\\au\\gov\\asd\\tac\\constellation\\functionality\\adaptors\\dataaccess\\plugins\\importing\\university0-0.owl"; //https://raw.githubusercontent.com/jbarrasa/datasets/master/rdf/music.ttl";
//            //final URL documentUrl = getClass().getResource("univ-bench.owl");
//            // if the input uri is a local file, add the file:// protocol and convert the slashes to forward slashes
//            final File file = new File(inputFilename);
//            if (file.exists()) {
//                inputFilename = "file:///" + inputFilename.replaceAll("\\\\", "/");
//            }
//
//            final URL documentUrl = new URL(inputFilename);
//            //final InputStream inputStream = documentUrl.openStream();
//
//            //----------------------------------Load File2 for testing only
//            String inputFilename2 = "C:\\Projects\\constellation-adaptors\\AdaptorsFunctionality\\src\\au\\gov\\asd\\tac\\constellation\\functionality\\adaptors\\dataaccess\\plugins\\importing\\univ-bench.owl";
//            final File file2 = new File(inputFilename2);
//            if (file2.exists()) {
//                inputFilename2 = "file:///" + inputFilename2.replaceAll("\\\\", "/");
//            }
//
//            OWLOntology o1 = manager.loadOntology(IRI.create(inputFilename));
//            OWLOntology o2 = manager.loadOntology(IRI.create(inputFilename2));
//            // Create our ontology merger
//            OWLOntologyMerger merger = new OWLOntologyMerger(manager);
//            // Merge all of the loaded ontologies, specifying an IRI for the new ontology
//
//            IRI mergedOntologyIRI = IRI.create("http://www.semanticweb.com/mymergedont");
//            ontology = merger.createMergedOntology(manager, mergedOntologyIRI);
            //---------------------------------------
            LOGGER.info("Ontology: " + ontology);
            //Check if the ontology contains any axioms
            LOGGER.info("Number of axioms before: " + ontology.getAxiomCount());

            // get and configure a reasoner (HermiT)
            OWLReasonerFactory reasonerFactory = new ReasonerFactory();
            //ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
            //OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor);

            // load the ontology to the reasoner
            //Hermi reasoner = com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory.getInstance().createReasoner(ontology);
            OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);

            Stream<OWLAxiom> add = reasoner.pendingAxiomAdditions();
            Stream<OWLAxiom> rem = reasoner.pendingAxiomRemovals();
            Stream<OWLOntologyChange> s = reasoner.pendingChanges();

            LOGGER.info("Number of axioms before precompute- pendingAxiomAdditions: " + add.count());
            LOGGER.info("Number of axioms before precompute- pendingAxiomRemovals: " + rem.count());
            LOGGER.info("Number of axioms before precompute- pendingChanges: " + s.count());

            LOGGER.info("Number of axioms before precompute: " + ontology.getAxiomCount());
            ////////////////////////////////////1. PRECOMPUTE
            //precompute(reasoner);
            //reasoner.flush();
            ////////////////////////////////////2. Use generator and reasoner to infer

            // Starting to add axiom generators
            OWLDataFactory datafactory = manager.getOWLDataFactory();

            List<InferredAxiomGenerator<? extends OWLAxiom>> inferredAxioms = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
            inferredAxioms.add(new InferredSubClassAxiomGenerator());
            inferredAxioms.add(new InferredClassAssertionAxiomGenerator());
            inferredAxioms.add(new InferredDataPropertyCharacteristicAxiomGenerator());
            inferredAxioms.add(new InferredObjectPropertyCharacteristicAxiomGenerator());
            inferredAxioms.add(new InferredEquivalentClassAxiomGenerator());
            inferredAxioms.add(new InferredPropertyAssertionGenerator());
            inferredAxioms.add(new InferredInverseObjectPropertiesAxiomGenerator());
            inferredAxioms.add(new InferredSubDataPropertyAxiomGenerator());
            inferredAxioms.add(new InferredSubObjectPropertyAxiomGenerator());

            // for writing inferred axioms to the new ontology
            //OWLOntologyID id = ontology.getOntologyID();
            ///Optional<IRI> iri2 = id.getOntologyIRI();
            //String iri3 = iri2.get().getIRIString();
            //OWLOntology infOnt = manager.createOntology(IRI.create(ontology.getOntologyID().getOntologyIRI().get() + "_inferred"));
            OWLOntology infOnt = manager.createOntology(IRI.create("_inferred"));

            // use generator and reasoner to infer some axioms
            InferredOntologyGenerator inferredOntologyGenerator = new InferredOntologyGenerator(reasoner, inferredAxioms);

            //Storing the results
            inferredOntologyGenerator.fillOntology(datafactory, infOnt);

            //reasoner.precomputeInferences(precompute.toArray(InferenceType));
            LOGGER.info("Number of axioms after reasoning : " + infOnt.getAxiomCount());

            Stream<OWLAxiom> add2 = reasoner.pendingAxiomAdditions();
            Stream<OWLAxiom> rem2 = reasoner.pendingAxiomRemovals();
            Stream<OWLOntologyChange> s2 = reasoner.pendingChanges();

            LOGGER.info("Number of axioms after reasoning- pendingAxiomAdditions: " + add2.count());
            LOGGER.info("Number of axioms after reasoning- pendingAxiomRemovals: " + rem2.count());
            LOGGER.info("Number of axioms after reasoning- pendingChanges: " + s2.count());

            //-------------------------\//Create a file for the new format
            File fileformatted = File.createTempFile("InferredFile", ".owl");
            //Save the ontology in a different format
//            OWLDocumentFormat format = manager.getOntologyFormat(infOnt);

            // Save the ontology in owl/xml format
//            OWLXMLDocumentFormat owlxmlFormat = new OWLXMLDocumentFormat();
            // Some ontology formats support prefix names and prefix IRIs.
            // When we save the ontology in the new format we will copy the prefixes over
            // so that we have nicely abbreviated IRIs in the new ontology document
//            if (format.isPrefixOWLDocumentFormat()) {
//                owlxmlFormat.copyPrefixesFrom(format.asPrefixOWLDocumentFormat());
//                //Removed as this caused rdf4j parser exceptions:
//                //RDFFormat.RDFXML->org.eclipse.rdf4j.rio.RDFParseException: unqualified attribute 'name' not allowed [line 8, column 84]
//                //RDFFormat.NTRIPLES-> org.eclipse.rdf4j.rio.RDFParseException: IRI included an unencoded space:   [line 1]
//            }
            manager.saveOntology(infOnt, IRI.create(fileformatted.toURI()));
            //-------------------------\Draw the graph
            Repository sailRepo = new SailRepository(new MemoryStore());
            try {
                RepositoryConnection conn = sailRepo.getConnection();
                final URL newdocumentUrl = new URL("file:///" + fileformatted.getPath());
                final String baseURI = newdocumentUrl.toString();

                try {
                    conn.add(fileformatted, baseURI, RDFFormat.RDFXML);
                    RepositoryResult<Statement> statements = conn.getStatements(null, null, null, true);

                    try {
                        RDFUtilities.PopulateRecordStore(recordStore, statements, subjectToType, layer_Mask);
                    } finally {
                        statements.close();
                    }

                } catch (RDF4JException e) {
                    LOGGER.info("-------------------------Ex = " + e);
                } finally {
                    conn.close();
                }

            } catch (RDF4JException e) {
                LOGGER.info("-------------------------Ex = " + e);
            } finally {
                sailRepo.shutDown();
            }

            LOGGER.info("-------END-------");
        } catch (IOException | OWLOntologyStorageException | OWLOntologyCreationException e) { //OWLOntologyCreationException
            LOGGER.warning("Exception : " + e);
        }
        //LOGGER.info(recordStore.toStringVerbose());
        return recordStore;
    }

    private void precompute(OWLReasoner reasoner) {
        // Set<inferencetype> precomputeNow = EnumSet.copyOf(precompute);

        InferenceType[] precomputeNow = InferenceType.values();
        //EnumSet.copyOf(precompute);
//        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology); //getOWLModelManager().getReasoner();
//        if (!reasoner.getPendingChanges().isEmpty()) {
//            reasoner.flush();
//        }
//        precomputeNow.retainAll(reasoner.getPrecomputableInferenceTypes());
//        for (InferenceType inference : precompute) {
//            if (reasoner.isPrecomputed(inference)) {
//                precomputeNow.remove(inference);
//            }
//        }
        Set<InferenceType> re = reasoner.getPrecomputableInferenceTypes();
//if (!precomputeNow.isEmpty()) {
        if (precomputeNow.length > 0) {
            reasoner.precomputeInferences(precomputeNow);
//            /precomputeNow.toArray(new InferenceType[0]

        }
    }

    @Override
    public String getType() {
        return DataAccessPluginAdaptorType.ENRICH;
    }

    @Override
    public int getPosition() {
        return 102;
    }

    @Override
    public String getDescription() {
        return "Apply OWL API Inferencing";
    }
}
