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
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.AnalyticConcept;
import au.gov.asd.tac.constellation.graph.schema.visual.VisualSchemaPluginRegistry;
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;
import au.gov.asd.tac.constellation.plugins.Plugin;
import au.gov.asd.tac.constellation.plugins.PluginException;
import au.gov.asd.tac.constellation.plugins.PluginExecution;
import au.gov.asd.tac.constellation.plugins.PluginExecutor;
import au.gov.asd.tac.constellation.plugins.PluginInfo;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.PluginType;
import au.gov.asd.tac.constellation.plugins.parameters.ParameterChange;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameter;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import au.gov.asd.tac.constellation.plugins.parameters.types.FileParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.FileParameterType.FileParameterValue;
import au.gov.asd.tac.constellation.plugins.parameters.types.SingleChoiceParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.SingleChoiceParameterType.SingleChoiceParameterValue;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPluginCoreType;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
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

    //private static final Logger LOGGER = Logger.getLogger(ImportFromRDFPlugin.class.getName());
    // parameters
    public static final String INPUT_FILE_URI_PARAMETER_ID = PluginParameter.buildId(ImportFromRDFPlugin.class, "input_file_uri");
    public static final String INPUT_FILE_FORMAT_PARAMETER_ID = PluginParameter.buildId(ImportFromRDFPlugin.class, "input_file_format");
    private static int layer_Mask = 3;
    final static Map<String, RDFFormat> rdfFileFormats = new HashMap<>();

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

    final Map<String, String> subjectToType = new HashMap<>();

    @Override
    protected RecordStore query(RecordStore query, PluginInteraction interaction, PluginParameters parameters) throws InterruptedException, PluginException {

        String inputFilename = parameters.getParameters().get(INPUT_FILE_URI_PARAMETER_ID).getStringValue();
        final String intpuFileFormat = parameters.getParameters().get(INPUT_FILE_FORMAT_PARAMETER_ID).getStringValue();

        // if the input uri is a local file, add the file:// protocol and convert the slashes to forward slashes
        final File file = new File(inputFilename);
        if (file.exists()) {
            inputFilename = "file:///" + inputFilename.replaceAll("\\\\", "/");
        }

        final RDFFormat format = Rio.getParserFormatForFileName(inputFilename.toString()).orElse(RDFFormat.TURTLE);

        //TODO Research RDF4J etc
        //TODO Research base predicates; RDFS standard and what they map to in consty
        //TODO Seperate queries to retrieve base predicates
        //TODO Develop ontology for constellation -> mapping RDF stuff to existing constellation stuff (Allow for icons etc) <BASIC MAPPING>
        //TODO Potentially: Seperate query for mapping from RDF to Consty <SPECIFIC MAPPING>
        //TODO Add triples to constellation graph and update display; RDF view?
        //TODO Change the additional INFO logging to DEBUG or remove them once things are working
        GraphRecordStore results = new GraphRecordStore();

        try {
            final URL documentUrl = new URL(inputFilename);
            final InputStream inputStream = documentUrl.openStream();
            final String baseURI = documentUrl.toString();
            //final RDFFormat format = getRdfFormat(intpuFileFormat);

            try (GraphQueryResult res = QueryResults.parseGraphBackground(inputStream, baseURI, format)) {
                //try (GraphQueryResult evaluate = QueryResults.parseGraphBackground(inputStream, baseURI, format)) {
                //Model res = QueryResults.asModel(evaluate);

                RDFUtilities.PopulateRecordStore(results, res, subjectToType, layer_Mask);

            } catch (RDF4JException e) {
                // handle unrecoverable error
            } finally {
                inputStream.close();
            }

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return results;
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
//        inputFileUriParameter.setStringValue("https://raw.githubusercontent.com/jbarrasa/datasets/master/rdf/music.ttl");//file:///tmp/mutic.ttl
//        inputFileUriParameter.setStringValue("http://eulersharp.sourceforge.net/2003/03swap/countries");
        inputFileUriParameter.setStringValue("https://raw.githubusercontent.com/stardog-union/pellet/master/examples/src/main/resources/data/university0-0.owl");
        params.addParameter(inputFileUriParameter);

        final PluginParameter<SingleChoiceParameterValue> inputFileFormat = SingleChoiceParameterType.build(INPUT_FILE_FORMAT_PARAMETER_ID);
        inputFileFormat.setName("File Format");
        inputFileFormat.setDescription("RDF file format");
        SingleChoiceParameterType.setOptions(inputFileFormat, Lists.newArrayList(rdfFileFormats.keySet()));
        SingleChoiceParameterType.setChoice(inputFileFormat, RDFFormat.RDFXML.getName());
        params.addParameter(inputFileFormat);

        params.addController(INPUT_FILE_URI_PARAMETER_ID, (final PluginParameter<?> master, final Map<String, PluginParameter<?>> parameters, final ParameterChange change) -> {
            if (change == ParameterChange.VALUE) {
                // If the Rio parder can auto detect the file format, set it and lock the dropdown
                RDFFormat format = Rio.getParserFormatForFileName(master.getStringValue()).orElse(null);
                if (format != null) {
                    inputFileFormat.setObjectValue(parameters.get(INPUT_FILE_FORMAT_PARAMETER_ID).getObjectValue());
                    parameters.get(INPUT_FILE_FORMAT_PARAMETER_ID).setStringValue(format.getName());
                }
                parameters.get(INPUT_FILE_FORMAT_PARAMETER_ID).setEnabled(format == null);
            }
        });
        return params;
    }

    @Override
    protected void edit(GraphWriteMethods wg, PluginInteraction interaction,
            PluginParameters parameters) throws InterruptedException, PluginException {
        super.edit(wg, interaction, parameters);

        // Add the Vertex Type attribute based on subjectToType map
        // Had to do this later to avoid duplicate nodes with "Unknown" Type.
        final int vertexIdentifierAttributeId = VisualConcept.VertexAttribute.IDENTIFIER.ensure(wg);
        final int vertexTypeAttributeId = AnalyticConcept.VertexAttribute.TYPE.ensure(wg);
        final int graphVertexCount = wg.getVertexCount();
        for (int position = 0; position < graphVertexCount; position++) {
            final int currentVertexId = wg.getVertex(position);
            final String identifier = wg.getStringValue(vertexIdentifierAttributeId, currentVertexId);
            if (subjectToType.containsKey(identifier)) {
                String value = subjectToType.get(identifier);
                wg.setStringValue(vertexTypeAttributeId, currentVertexId, value);
            }
        }
        PluginExecution.withPlugin(VisualSchemaPluginRegistry.COMPLETE_SCHEMA).executeNow(wg);
        PluginExecutor.startWith(InteractiveGraphPluginRegistry.RESET_VIEW).executeNow(wg);
    }
}
