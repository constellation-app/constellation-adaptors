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
import au.gov.asd.tac.constellation.graph.GraphWriteMethods;
import au.gov.asd.tac.constellation.graph.interaction.InteractiveGraphPluginRegistry;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import au.gov.asd.tac.constellation.graph.schema.rdf.concept.RDFConcept;
import au.gov.asd.tac.constellation.graph.schema.visual.VisualSchemaPluginRegistry;
import au.gov.asd.tac.constellation.plugins.Plugin;
import au.gov.asd.tac.constellation.plugins.PluginException;
import au.gov.asd.tac.constellation.plugins.PluginExecutor;
import au.gov.asd.tac.constellation.plugins.PluginInfo;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.PluginType;
import au.gov.asd.tac.constellation.plugins.parameters.ParameterChange;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameter;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import au.gov.asd.tac.constellation.plugins.parameters.types.BooleanParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.BooleanParameterType.BooleanParameterValue;
import au.gov.asd.tac.constellation.plugins.parameters.types.FileParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.FileParameterType.FileParameterValue;
import au.gov.asd.tac.constellation.plugins.parameters.types.SingleChoiceParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.SingleChoiceParameterType.SingleChoiceParameterValue;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPluginCoreType;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import au.gov.asd.tac.constellation.views.layers.state.LayersViewConcept;
import au.gov.asd.tac.constellation.views.layers.state.LayersViewState;
import au.gov.asd.tac.constellation.views.layers.utilities.LayersUtilities;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

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

    // parameters
    public static final String INPUT_FILE_URI_PARAMETER_ID = PluginParameter.buildId(ImportFromRDFPlugin.class, "input_file_uri");
    public static final String INPUT_FILE_FORMAT_PARAMETER_ID = PluginParameter.buildId(ImportFromRDFPlugin.class, "input_file_format");

    // parameters when in advance mode
    public static final String ADVANCED_MODE_PARAMETER_ID = PluginParameter.buildId(ImportFromRDFPlugin.class, "advanced_mode");
    public static final String SCHEMA_FILE_URI_PARAMETER_ID = PluginParameter.buildId(ImportFromRDFPlugin.class, "schema_file_uri");
    public static final String DATA_FILE_URI_PARAMETER_ID = PluginParameter.buildId(ImportFromRDFPlugin.class, "data_file_uri");
    public static final String MAPPING_FILE_URI_PARAMETER_ID = PluginParameter.buildId(ImportFromRDFPlugin.class, "mapping_file_uri");

    final private static int SCHEMA_LAYER = 1 | (1 << 1);
    final private static int DATA_LAYER = 1 | (1 << 2);
    final private static int MAPPING_LAYER = 1 | (1 << 3);

    final static Map<String, RDFFormat> rdfFileFormats = new HashMap<>();

    private static final Logger LOGGER = Logger.getLogger(ImportFromRDFPlugin.class.getName());

    static {
        rdfFileFormats.put(RDFFormat.BINARY.getName(), RDFFormat.BINARY);
        rdfFileFormats.put(RDFFormat.HDT.getName(), RDFFormat.HDT);
        rdfFileFormats.put(RDFFormat.JSONLD.getName(), RDFFormat.JSONLD);
        rdfFileFormats.put(RDFFormat.N3.getName(), RDFFormat.N3);
        rdfFileFormats.put(RDFFormat.NQUADS.getName(), RDFFormat.NQUADS);
        rdfFileFormats.put(RDFFormat.NTRIPLES.getName(), RDFFormat.NTRIPLES);
        rdfFileFormats.put(RDFFormat.RDFA.getName(), RDFFormat.RDFA);
        rdfFileFormats.put(RDFFormat.RDFJSON.getName(), RDFFormat.RDFJSON);
        rdfFileFormats.put(RDFFormat.RDFXML.getName(), RDFFormat.RDFXML);
        rdfFileFormats.put(RDFFormat.TRIG.getName(), RDFFormat.TRIG);
        rdfFileFormats.put(RDFFormat.TRIGSTAR.getName(), RDFFormat.TRIGSTAR);
        rdfFileFormats.put(RDFFormat.TRIX.getName(), RDFFormat.TRIX);
        rdfFileFormats.put(RDFFormat.TURTLE.getName(), RDFFormat.TURTLE);
        rdfFileFormats.put(RDFFormat.TURTLESTAR.getName(), RDFFormat.TURTLESTAR);
    }

    final MultiKeyMap literalToValue = MultiKeyMap.decorate(new LinkedMap());
    Set<Statement> bNodeStatements = new HashSet<>();

    @Override
    protected RecordStore query(RecordStore query, PluginInteraction interaction, PluginParameters parameters) throws InterruptedException, PluginException {

        // simple mode
        final String inputFileName = parameters.getParameters().get(INPUT_FILE_URI_PARAMETER_ID).getStringValue();
        final String inputRdfFormat = parameters.getParameters().get(INPUT_FILE_FORMAT_PARAMETER_ID).getStringValue();

        // advanced mode
        final Boolean advancedMode = parameters.getParameters().get(ADVANCED_MODE_PARAMETER_ID).getBooleanValue();
        final String schemaFileName = parameters.getParameters().get(SCHEMA_FILE_URI_PARAMETER_ID).getStringValue();
        final String dataFileName = parameters.getParameters().get(DATA_FILE_URI_PARAMETER_ID).getStringValue();
        final String mappingFileName = parameters.getParameters().get(MAPPING_FILE_URI_PARAMETER_ID).getStringValue();

        // if the input uri is a local file, add the file:// protocol and convert the slashes to forward slashes
        final String fixedInputFileName = fixProtocol(inputFileName);
        final String fixedSchemaFileName = fixProtocol(schemaFileName);
        final String fixedDataFileName = fixProtocol(dataFileName);
        final String fixedMappingFileName = fixProtocol(mappingFileName);

        final RDFFormat rdfFormat = getRdfFormat(inputRdfFormat);
        final RDFFormat inputFileFormat = Rio.getParserFormatForFileName(fixedInputFileName).orElse(rdfFormat);
        final RDFFormat schemaFileFormat = Rio.getParserFormatForFileName(fixedSchemaFileName).orElse(rdfFormat);
        final RDFFormat dataFileFormat = Rio.getParserFormatForFileName(fixedDataFileName).orElse(rdfFormat);
        final RDFFormat mappingFileFormat = Rio.getParserFormatForFileName(fixedMappingFileName).orElse(rdfFormat);

        //TODO Research base predicates; RDFS standard and what they map to in consty
        //TODO Seperate queries to retrieve base predicates
        //TODO Develop ontology for constellation -> mapping RDF stuff to existing constellation stuff (Allow for icons etc) <BASIC MAPPING>
        //TODO Potentially: Seperate query for mapping from RDF to Consty <SPECIFIC MAPPING>
        //TODO Add triples to constellation graph and update display; RDF view?
        //TODO Change the additional INFO logging to DEBUG or remove them once things are working
        final GraphRecordStore recordStore = new GraphRecordStore();

        if (advancedMode) {
            importFile(fixedSchemaFileName, recordStore, schemaFileFormat, SCHEMA_LAYER); // constellation_schema.ttl 
            importFile(fixedDataFileName, recordStore, dataFileFormat, DATA_LAYER); // example.ttl
            importFile(fixedMappingFileName, recordStore, mappingFileFormat, MAPPING_LAYER); // example_mapping.ttl
        } else {
            importFile(fixedInputFileName, recordStore, inputFileFormat, SCHEMA_LAYER);
        }

        return recordStore;
    }

    private String fixProtocol(final String filename) {
        final File file = new File(filename);
        if (file.exists()) {
            return "file:///" + filename.replaceAll("\\\\", "/");
        }

        return filename;
    }

    private RDFFormat getRdfFormat(final String format) {
        return rdfFileFormats.get(format);
    }

    @Override
    public String getType() {
        return DataAccessPluginCoreType.IMPORT;
    }

    @Override
    public int getPosition() {
        return 100;
    }

    @Override
    public String getDescription() {
        return "Import an RDF dataset";
    }

    @Override
    public PluginParameters createParameters() {
        final PluginParameters params = new PluginParameters();

        final PluginParameter<FileParameterValue> inputFileUriParameter = FileParameterType.build(INPUT_FILE_URI_PARAMETER_ID);
        inputFileUriParameter.setName("Input File");
        inputFileUriParameter.setDescription("RDF file URI");
//        inputFileUriParameter.setStringValue("https://raw.githubusercontent.com/jbarrasa/datasets/master/rdf/music.ttl");
//        inputFileUriParameter.setStringValue("http://eulersharp.sourceforge.net/2003/03swap/countries");
        //inputFileUriParameter.setStringValue("https://raw.githubusercontent.com/stardog-union/pellet/master/examples/src/main/resources/data/university0-0.owl");
        inputFileUriParameter.setStringValue("http://protege.stanford.edu/ontologies/pizza/pizza.owl");
        params.addParameter(inputFileUriParameter);

        final PluginParameter<SingleChoiceParameterValue> inputFileFormat = SingleChoiceParameterType.build(INPUT_FILE_FORMAT_PARAMETER_ID);
        inputFileFormat.setName("File Format");
        inputFileFormat.setDescription("RDF file format");
        SingleChoiceParameterType.setOptions(inputFileFormat, Lists.newArrayList(rdfFileFormats.keySet()));
        SingleChoiceParameterType.setChoice(inputFileFormat, RDFFormat.RDFXML.getName());
        params.addParameter(inputFileFormat);

        final PluginParameter<BooleanParameterValue> advancedModeParameter = BooleanParameterType.build(ADVANCED_MODE_PARAMETER_ID);
        advancedModeParameter.setName("Advanced Mode");
        advancedModeParameter.setDescription("Specify schema, data and mapping files explicitly");
        advancedModeParameter.setBooleanValue(false);
        params.addParameter(advancedModeParameter);

        final PluginParameter<FileParameterValue> schemaFileUriParameter = FileParameterType.build(SCHEMA_FILE_URI_PARAMETER_ID);
        schemaFileUriParameter.setName("Schema File");
        schemaFileUriParameter.setDescription("The RDF schema definition file");
        schemaFileUriParameter.setVisible(false);
        params.addParameter(schemaFileUriParameter);

        final PluginParameter<FileParameterValue> dataFileUriParameter = FileParameterType.build(DATA_FILE_URI_PARAMETER_ID);
        dataFileUriParameter.setName("Data File");
        dataFileUriParameter.setDescription("The RDF file with concreate instances");
        dataFileUriParameter.setVisible(false);
        params.addParameter(dataFileUriParameter);

        final PluginParameter<FileParameterValue> mappingFileUriParameter = FileParameterType.build(MAPPING_FILE_URI_PARAMETER_ID);
        mappingFileUriParameter.setName("Mapping File");
        mappingFileUriParameter.setDescription("The mapping file from the RDF schema to Constellation types");
        mappingFileUriParameter.setVisible(false);
        params.addParameter(mappingFileUriParameter);

        params.addController(INPUT_FILE_URI_PARAMETER_ID, (final PluginParameter<?> master, final Map<String, PluginParameter<?>> parameters, final ParameterChange change) -> {
            if (change == ParameterChange.VALUE) {
                // If the Rio parser can auto detect the file format, set it and lock the dropdown
                RDFFormat format = Rio.getParserFormatForFileName(master.getStringValue()).orElse(null);
                if (format != null) {
                    inputFileFormat.setObjectValue(parameters.get(INPUT_FILE_FORMAT_PARAMETER_ID).getObjectValue());
                    parameters.get(INPUT_FILE_FORMAT_PARAMETER_ID).setStringValue(format.getName());
                }
                parameters.get(INPUT_FILE_FORMAT_PARAMETER_ID).setEnabled(format == null);
            }
        });
        
        params.addController(ADVANCED_MODE_PARAMETER_ID, (final PluginParameter<?> master, final Map<String, PluginParameter<?>> parameters, final ParameterChange change) -> {
            if (change == ParameterChange.VALUE) {
                parameters.get(SCHEMA_FILE_URI_PARAMETER_ID).setVisible(master.getBooleanValue());
                parameters.get(DATA_FILE_URI_PARAMETER_ID).setVisible(master.getBooleanValue());
                parameters.get(MAPPING_FILE_URI_PARAMETER_ID).setVisible(master.getBooleanValue());
            }
        });
        return params;
    }

    @Override
    protected void edit(GraphWriteMethods wg, PluginInteraction interaction, PluginParameters parameters) throws InterruptedException, PluginException {
        super.edit(wg, interaction, parameters);

        RDFUtilities.setLiteralValuesVertexAttribute(wg, literalToValue);

        // Add BNODES in the graph attribute
        final int rdfBlankNodesAttributeId = RDFConcept.GraphAttribute.RDF_BLANK_NODES.ensure(wg);
        wg.setObjectValue(rdfBlankNodesAttributeId, 0, bNodeStatements);

        // Set layers state
        final int layersStateAttributeId = LayersViewConcept.MetaAttribute.LAYERS_VIEW_STATE.ensure(wg);
        final LayersViewState state = new LayersViewState();

        LayersUtilities.addLayerAt(state, "RDF Schema Layer", 1);
        LayersUtilities.addLayerAt(state, "RDF Data Layer", 2);
        LayersUtilities.addLayerAt(state, "RDF Mapping Layer", 3);

        wg.setObjectValue(layersStateAttributeId, 0, state);

        PluginExecutor.startWith(VisualSchemaPluginRegistry.COMPLETE_SCHEMA)
                .followedBy(InteractiveGraphPluginRegistry.RESET_VIEW)
                .executeNow(wg);
    }

    /**
     * Imports the file to the recordstore and draws it on the respective layer
     *
     * @param fileName
     * @param recordStore
     * @param format
     * @param layerNumber
     */
    private void importFile(final String fileName, final GraphRecordStore recordStore, final RDFFormat format, final int layerNumber) {
        LOGGER.log(Level.INFO, "importing {0}", fileName);
        
        try {
            final URL documentUrl = new URL(fileName);
            final InputStream inputStream = documentUrl.openStream();

            //final RDFFormat format = getRdfFormat(intpuFileFormat);
            //Model model = Rio.parse(inputStream, baseURI, format);
            final String baseURI = documentUrl.toString();

            if (FilenameUtils.getExtension(fileName).equalsIgnoreCase("owl") && fileName.startsWith("http://")) {
                final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
                final IRI iri = IRI.create(fileName);
                final OWLOntology ontology = manager.loadOntologyFromOntologyDocument(iri);

                final File tempFile = File.createTempFile("TempOwlFile", ".owl");
                manager.saveOntology(ontology, IRI.create(tempFile.toURI()));

                final Repository repo = new SailRepository(new MemoryStore());
                final RepositoryConnection conn = repo.getConnection();
                conn.add(tempFile, tempFile.toURI().toString(), RDFFormat.RDFXML);
                try ( RepositoryResult<Statement> statements = conn.getStatements(null, null, null, true)) {
                    RDFUtilities.PopulateRecordStore(recordStore, statements, literalToValue, bNodeStatements, layerNumber);
                } finally {
                    inputStream.close();
                }
//                String beginning = "<?xml version=\"1.0\"?>\n";
//
//                List<InputStream> streams = Arrays.asList(
//                        new ByteArrayInputStream(beginning.getBytes()),
//                        inputStream);
//                inputStream = new SequenceInputStream(Collections.enumeration(streams));
//
            } else {

                try ( GraphQueryResult queryResult = QueryResults.parseGraphBackground(inputStream, baseURI, format)) {
                    //try (GraphQueryResult evaluate = QueryResults.parseGraphBackground(inputStream, baseURI, format)) {
                    //Model res = QueryResults.asModel(evaluate);
                    if (queryResult.hasNext()) {
                        RDFUtilities.PopulateRecordStore(recordStore, queryResult, literalToValue, bNodeStatements, layerNumber);
                    } else {
                        LOGGER.warning("queryResult IS EMPTY ");
                    }
                } catch (RDF4JException ex) {
                    LOGGER.log(Level.SEVERE, "RDF4JException: {0}", ex);
                    // TODO: handle unrecoverable error
                } finally {
                    inputStream.close();
                }
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (OWLOntologyStorageException ex) {
            Exceptions.printStackTrace(ex);
        } catch (OWLOntologyCreationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
