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
import au.gov.asd.tac.constellation.plugins.Plugin;
import au.gov.asd.tac.constellation.plugins.PluginException;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.rio.RDFFormat;
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

    private static final Logger LOGGER = Logger.getLogger(ImportFromRDFPlugin.class.getName());

    // parameters
    public static final String INPUT_FILE_URI_PARAMETER_ID = PluginParameter.buildId(ImportFromRDFPlugin.class, "input_file_uri");
    public static final String INPUT_FILE_FORMAT_PARAMETER_ID = PluginParameter.buildId(ImportFromRDFPlugin.class, "input_file_format");

    @Override
    protected RecordStore query(RecordStore query, PluginInteraction interaction, PluginParameters parameters) throws InterruptedException, PluginException {

        final String inputFilename = parameters.getParameters().get(INPUT_FILE_URI_PARAMETER_ID).getStringValue();
        final String intpuFileFormat = parameters.getParameters().get(INPUT_FILE_FORMAT_PARAMETER_ID).getStringValue();

        //TODO Research RDF4J etc
        //TODO Research base predicates; RDFS standard and what they map to in consty
        //TODO Seperate queries to retrieve base predicates
        //TODO Develop ontology for constellation -> mapping RDF stuff to existing constellation stuff (Allow for icons etc) <BASIC MAPPING>
        //TODO Potentially: Seperate query for mapping from RDF to Consty <SPECIFIC MAPPING>
        //TODO Add triples to constellation graph and update display; RDF view?
        //TODO Change the additional INFO logging to DEBUG or remove them once things are working
        GraphRecordStore results = new GraphRecordStore();

//        final Map<String, String> prefixes = new HashMap<>();
//        prefixes.put("country", "http://eulersharp.sourceforge.net/2003/03swap/countries#");
//        prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
//        prefixes.put("jur", "http://sweet.jpl.nasa.gov/2.3/humanJurisdiction.owl#");
//        prefixes.put("dce", "http://purl.org/dc/elements/1.1/");
//        prefixes.put("dct", "http://purl.org/dc/terms/");
//        prefixes.put("owl", "http://www.w3.org/2002/07/owl#");
//        prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
//        prefixes.put("skos", "http://www.w3.org/2004/02/skos/core#");
//        prefixes.put("music", "http://neo4j.com/voc/music#");
//        prefixes.put("ind", "http://neo4j.com/indiv#");
        //try (RepositoryConnection conn = repo.getConnection()) {
        // Create query string
//        StringBuilder prefixString = new StringBuilder();
//        prefixes.forEach((String key, String value) -> {
//            prefixString.append("PREFIX ");
//            prefixString.append(key);
//            prefixString.append(": <");
//            prefixString.append(value);
//            prefixString.append("> ");
//        }
//        );
        final Map<String, String> subjectToType = new HashMap<>();

        try {
            final URL documentUrl = new URL(inputFilename);
            final InputStream inputStream = documentUrl.openStream();
            final String baseURI = documentUrl.toString();
            final RDFFormat format = getRdfFormat(intpuFileFormat);

            try (GraphQueryResult res = QueryResults.parseGraphBackground(inputStream, baseURI, format)) {
                Resource activeNodeIdentifier = null;
                String activeNodeType = null;

                while (res.hasNext()) {
                    LOGGER.info("Processing next record...");
                    final Statement st = res.next();

                    final Map<String, String> namespaces = res.getNamespaces();
                    final Resource subject = st.getSubject();
                    final IRI predicate = st.getPredicate();
                    final Value object = st.getObject();
                    final Resource context = st.getContext();

                    LOGGER.log(Level.INFO, "Saw Subject: {0}, Predicate: {1}, Object: {2}, Context: {3}", new Object[]{subject, predicate, object, context});

                    boolean objectIsAttribute = false;

                    // PROCESS: Subject
                    // ----------------
                    String subjectName = null;
                    if (subject instanceof Literal) {
                        subjectName = ((Literal) subject).getLabel();
                    } else if (subject instanceof IRI) {
                        subjectName = ((IRI) subject).getLocalName();
                    } else {
                        LOGGER.log(Level.WARNING, "Unknown subject type: {0}, dropping", subject);
                    }

                    // PROCESS: Predicate
                    // ----------------
                    String predicateName = predicate.getLocalName();//predicate.stringValue()

                    // PROCESS: Object
                    // ----------------
                    String objectName = null;
                    if (object instanceof Literal) {
                        objectName = ((Literal) object).getLabel();
                        objectIsAttribute = true;
                    } else if (object instanceof IRI) {
                        objectName = ((IRI) object).getLocalName();
                    } else {
                        LOGGER.log(Level.WARNING, "Unknown object type: {0}, dropping", object);
                    }

                    LOGGER.log(Level.INFO, "Processing Subject: {0}, Predicate: {1}, Object: {2}, Context: {3}", new Object[]{subjectName, predicateName, objectName, context});

                    // create the record store
                    if (objectIsAttribute) { // literal object values are added as Vertex properties
                        LOGGER.log(Level.INFO, "Adding Literal \"{0}\"", objectName);
                        results.add();
                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, subjectName);
                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, subjectToType.get(subjectName));
//                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.PLACEHOLDER);
                        results.set(GraphRecordStoreUtilities.SOURCE + predicateName, objectName); // TODO: the "name" should be the identifier
                    } else if ("type".equals(predicateName)) {
                        results.add();
                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, subjectName);
                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, objectName);

                        activeNodeIdentifier = subject;
                        activeNodeType = objectName;

                        subjectToType.put(subjectName, activeNodeType);
                    } else if ("member".equals(predicateName)
                            || "writer".equals(predicateName)
                            || "artist".equals(predicateName)
                            || "track".equals(predicateName)) {
                        results.add();

                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, subjectName);
                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, subjectToType.get(subjectName));

                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, objectName);
                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, subjectToType.get(objectName));

                        results.set(GraphRecordStoreUtilities.TRANSACTION + VisualConcept.TransactionAttribute.IDENTIFIER, predicateName);
                        // results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, "rdf tx type"); //ObjectProperty?
                    } else {
                        LOGGER.log(Level.WARNING, "Predicate: {0} not mapped.", predicateName);
                    }
                }

//                Graph graph = GraphManager.getDefault().getActiveGraph();
//                WritableGraph wg = graph.getWritableGraph("", true);
//                final Comparator<SchemaVertexType> dominanceComparator = (Comparator<SchemaVertexType>) VertexDominanceCalculator.getDefault().getComparator();
//                PlaceholderUtilities.collapsePlaceholders(wg, dominanceComparator, false);
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
        inputFileUriParameter.setStringValue("https://raw.githubusercontent.com/jbarrasa/datasets/master/rdf/music.ttl");//file:///tmp/mutic.ttl
//        openFileParam.setStringValue("http://eulersharp.sourceforge.net/2003/03swap/countries");
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

}
