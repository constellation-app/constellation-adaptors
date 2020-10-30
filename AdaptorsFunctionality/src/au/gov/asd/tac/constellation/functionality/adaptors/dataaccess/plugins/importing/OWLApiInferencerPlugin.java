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

import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.utilities.RDFUtilities;
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
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
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
import org.semanticweb.owlapi.util.OWLOntologyMerger;

@ServiceProviders({
    @ServiceProvider(service = DataAccessPlugin.class),
    @ServiceProvider(service = Plugin.class)})
@PluginInfo(pluginType = PluginType.IMPORT, tags = {"IMPORT"})
@NbBundle.Messages("OWLApiInferencerPlugin=Inferring with the OWL API")
public class OWLApiInferencerPlugin extends RecordStoreQueryPlugin implements DataAccessPlugin { //extends SimpleEditPlugin

    private static final Logger LOGGER = Logger.getLogger(OWLApiInferencerPlugin.class.getName());

    private static int layer_Mask = 9;

    final Map<String, String> subjectToType = new HashMap<>();
    final Map<String, String> bnodeToSubject = new HashMap<>();
    private Set<InferenceType> precompute;
    private InferredOntologyGenerator inferredOntologyGenerator;

    @Override
    //public void edit(GraphWriteMethods graph, final PluginInteraction interaction, final PluginParameters parameters) throws InterruptedException, PluginException {
    protected RecordStore query(RecordStore query, PluginInteraction interaction, PluginParameters parameters) throws InterruptedException, PluginException {
        GraphRecordStore recordStore = new GraphRecordStore();
        //populate model from query or the whole graph as required
        //Model model = RDFUtilities.getGraphModel(graph);

        final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        final OWLOntology ontology;

        try {
            ///final URL documentUrl = getClass().getResource("university0-0.owl");
            ///final InputStream inputStream = documentUrl.openStream();
            //---------------------------------------
            String inputFilename = "C:\\Projects\\constellation-adaptors\\AdaptorsFunctionality\\src\\au\\gov\\asd\\tac\\constellation\\functionality\\adaptors\\dataaccess\\plugins\\importing\\university0-0.owl"; //https://raw.githubusercontent.com/jbarrasa/datasets/master/rdf/music.ttl";
            //final URL documentUrl = getClass().getResource("univ-bench.owl");
            // if the input uri is a local file, add the file:// protocol and convert the slashes to forward slashes
            final File file = new File(inputFilename);
            if (file.exists()) {
                inputFilename = "file:///" + inputFilename.replaceAll("\\\\", "/");
            }

            final URL documentUrl = new URL(inputFilename);
            //final InputStream inputStream = documentUrl.openStream();

            //----------------------------------Load File2 for testing only
            String inputFilename2 = "C:\\Projects\\constellation-adaptors\\AdaptorsFunctionality\\src\\au\\gov\\asd\\tac\\constellation\\functionality\\adaptors\\dataaccess\\plugins\\importing\\univ-bench.owl";
            final File file2 = new File(inputFilename2);
            if (file2.exists()) {
                inputFilename2 = "file:///" + inputFilename2.replaceAll("\\\\", "/");
            }

            OWLOntology o1 = manager.loadOntology(IRI.create(inputFilename));
            OWLOntology o2 = manager.loadOntology(IRI.create(inputFilename2));
            // Create our ontology merger
            OWLOntologyMerger merger = new OWLOntologyMerger(manager);
            // Merge all of the loaded ontologies, specifying an IRI for the new ontology

            IRI mergedOntologyIRI = IRI.create("http://www.semanticweb.com/mymergedont");
            ontology = merger.createMergedOntology(manager, mergedOntologyIRI);
            //---------------------------------------

            //ontology = manager.loadOntologyFromOntologyDocument(inputStream);
            LOGGER.info("Ontology: " + ontology);
            //Check if the ontology contains any axioms
            LOGGER.info("Number of axioms before: " + ontology.getAxiomCount());

            // get and configure a reasoner (HermiT)
            OWLReasonerFactory reasonerFactory = new ReasonerFactory();
            //ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
            //OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor);

            var factory = manager.getOWLDataFactory();

            // load the ontology to the reasoner
            //Hermi reasoner = com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory.getInstance().createReasoner(ontology);
            OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
            //Check if the ontology  axioms count has changed
            LOGGER.info("Number of axioms after reasoning(?): " + ontology.getAxiomCount());

            // read the data into a model
//        final URL documentUrl = getClass().getResource("./resources/university0-0.owl");
//        final InputStream inputStream = documentUrl.openStream();
//        final RDFFormat format = RDFFormat.RDFXML;
//        loadTriples(model, inputStream, documentUrl, format);
//        OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();
//        Iterable<Statement> iterable = model.getStatements(null, null, null);
//        for (Statement s : iterable) {
//            OWLNamedIndividual subject = df.getOWLNamedIndividual(org.semanticweb.owlapi.model.IRI.create(s.getSubject().stringValue()));
//            OWLNamedIndividual predicate = df.getOWLNamedIndividual(org.semanticweb.owlapi.model.IRI.create(s.getPredicate().stringValue()));
//            OWLNamedIndividual object = df.getOWLNamedIndividual(org.semanticweb.owlapi.model.IRI.create(s.getObject().stringValue()));
//            //OWLAxiom axiom = df.(subject, object);
//            //ontology.add(axiom);
//        }
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
            long t0 = System.nanoTime();

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
            OWLOntology infOnt = manager.createOntology(IRI.create(ontology.getOntologyID().getOntologyIRI().get() + "_inferred"));

            // use generator and reasoner to infer some axioms
            InferredOntologyGenerator iog = new InferredOntologyGenerator(reasoner, inferredAxioms);

            //Storing the results
            iog.fillOntology(datafactory, infOnt);

            long elapsed_time = System.nanoTime() - t0;

            // save the ontology
            manager.saveOntology(infOnt, IRI.create("file:///C:/Projects/test.rdf"));
            LOGGER.info("Saved the ontology in file:///C:/Projects/test.rdf");
            LOGGER.info("Elapsed time: " + elapsed_time);
            ///////////////////////////////////////

            //reasoner.precomputeInferences(precompute.toArray(InferenceType));
            LOGGER.info("Number of axioms after reasoning : " + infOnt.getAxiomCount());

            Stream<OWLAxiom> add2 = reasoner.pendingAxiomAdditions();
            Stream<OWLAxiom> rem2 = reasoner.pendingAxiomRemovals();
            Stream<OWLOntologyChange> s2 = reasoner.pendingChanges();

            LOGGER.info("Number of axioms after reasoning- pendingAxiomAdditions: " + add2.count());
            LOGGER.info("Number of axioms after reasoning- pendingAxiomRemovals: " + rem2.count());
            LOGGER.info("Number of axioms after reasoning- pendingChanges: " + s2.count());

            //-------------------------\//Create a file for the new format
            File fileformatted = new File("test-format.owl");
            //Save the ontology in a different format
            OWLDocumentFormat format = manager.getOntologyFormat(infOnt);
            //OWLXMLOntologyFormat owlxmlFormat = new OWLXMLOntologyFormat();
            //OWLAPIRDFFormat format2 = OWLAPIRDFFormat.OWL_XML;

//            if (format.isPrefixOWLOntologyFormat()) {
            //owlxmlFormat.copyPrefixesFrom(format.asPrefixOWLDocumentFormat()); Removed to see if this removed parser exceptions:
            //RDFFormat.RDFXML->org.eclipse.rdf4j.rio.RDFParseException: unqualified attribute 'name' not allowed [line 8, column 84]
            //RDFFormat.NTRIPLES-> org.eclipse.rdf4j.rio.RDFParseException: IRI imelbourne ncluded an unencoded space:   [line 1]
//            }
            manager.saveOntology(infOnt, format, IRI.create(fileformatted.toURI()));
            //-------------------------\Draw the graph
            Repository repo = new SailRepository(new MemoryStore());
            try {
                RepositoryConnection conn = repo.getConnection();
                final URL newdocumentUrl = new URL("file:///" + fileformatted.getPath());
                final String baseURI = newdocumentUrl.toString();

                try {
                    conn.add(fileformatted, baseURI, RDFFormat.RDFXML);
                    RepositoryResult<Statement> statements = conn.getStatements(null, null, null, true);

                    try {
//                        while (statements.hasNext()) {
//                            Statement st = statements.next();
//                            System.out.println(st);
//                            LOGGER.info("----STATEMENT---" + st);
//                        }
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
                repo.shutDown();
            }

            LOGGER.info("-------END-------");
        } catch (IOException | OWLOntologyStorageException | OWLOntologyCreationException ex) { //OWLOntologyCreationException
            Exceptions.printStackTrace(ex);

        }
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
        return DataAccessPluginCoreType.IMPORT;
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
