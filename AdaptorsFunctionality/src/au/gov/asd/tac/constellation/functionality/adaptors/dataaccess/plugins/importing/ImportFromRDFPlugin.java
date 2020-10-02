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
import au.gov.asd.tac.constellation.graph.Graph;
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
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameter;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import au.gov.asd.tac.constellation.plugins.parameters.types.FileParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.FileParameterType.FileParameterValue;
import au.gov.asd.tac.constellation.plugins.parameters.types.SingleChoiceParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.SingleChoiceParameterType.SingleChoiceParameterValue;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPluginCoreType;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    final Map<String, String> subjectToType = new HashMap<>();

    @Override
    protected RecordStore query(RecordStore query, PluginInteraction interaction, PluginParameters parameters) throws InterruptedException, PluginException {

        final String inputFilename = parameters.getParameters().get(INPUT_FILE_URI_PARAMETER_ID).getStringValue();
        final String intpuFileFormat = parameters.getParameters().get(INPUT_FILE_FORMAT_PARAMETER_ID).getStringValue();

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

                RDFUtilities.PopulateRecordStore(results, res, subjectToType);

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
        switch (format) {
            case "Turtle":
                return RDFFormat.TURTLE;
            default:
                throw new AssertionError();
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
        inputFileUriParameter.setStringValue("http://eulersharp.sourceforge.net/2003/03swap/countries");
        params.addParameter(inputFileUriParameter);

        final List<String> rdfFileFormats = new ArrayList<>();
        rdfFileFormats.add(RDFFormat.TURTLE.getName());

        final PluginParameter<SingleChoiceParameterValue> inputFileFormat = SingleChoiceParameterType.build(INPUT_FILE_FORMAT_PARAMETER_ID);
        inputFileFormat.setName("File Format");
        inputFileFormat.setDescription("RDF file format");
        SingleChoiceParameterType.setOptions(inputFileFormat, rdfFileFormats);
        SingleChoiceParameterType.setChoice(inputFileFormat, RDFFormat.TURTLE.getName());
        params.addParameter(inputFileFormat);

        return params;
    }

    @Override
    public void updateParameters(final Graph graph, final PluginParameters parameters) {
        if (parameters != null && parameters.getParameters() != null && !parameters.getParameters().get(INPUT_FILE_URI_PARAMETER_ID).getStringValue().isBlank()) {
            final String inputFilename = parameters.getParameters().get(INPUT_FILE_URI_PARAMETER_ID).getStringValue();
            RDFFormat format = Rio.getParserFormatForFileName(inputFilename.toString()).orElse(RDFFormat.TURTLE);

            @SuppressWarnings("unchecked")
            final PluginParameter<SingleChoiceParameterValue> inputFileFormat = (PluginParameter<SingleChoiceParameterValue>) parameters.getParameters().get(INPUT_FILE_FORMAT_PARAMETER_ID);

            final List<String> rdfFileFormats = new ArrayList<>();
            rdfFileFormats.add(format.getName());

            SingleChoiceParameterType.setOptions(inputFileFormat, rdfFileFormats);

            inputFileFormat.suppressEvent(true, new ArrayList<>());
            SingleChoiceParameterType.setChoice(inputFileFormat, format.getName());
            inputFileFormat.suppressEvent(false, new ArrayList<>());
            inputFileFormat.setObjectValue(parameters.getObjectValue(INPUT_FILE_FORMAT_PARAMETER_ID));
        }
    }

    @Override
    protected void edit(GraphWriteMethods wg, PluginInteraction interaction, PluginParameters parameters) throws InterruptedException, PluginException {
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
