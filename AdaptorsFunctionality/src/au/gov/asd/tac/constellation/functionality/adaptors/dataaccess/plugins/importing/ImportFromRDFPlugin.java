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
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPluginCoreType;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

//    private static final Logger LOGGER = Logger.getLogger(RDFViewer.class.getName());
    public static final String INPUT_FILE_PARAMETER_ID = PluginParameter.buildId(ImportFromRDFPlugin.class, "input_file");

    @Override
    protected RecordStore query(RecordStore query, PluginInteraction interaction, PluginParameters parameters) throws InterruptedException, PluginException {

        String inputFilename = parameters.getParameters().get(INPUT_FILE_PARAMETER_ID).getStringValue();

        //TODO Research RDF4J etc
        //TODO Research base predicates; RDFS standard and what they map to in consty
        //TODO Seperate queries to retrieve those
        //TODO Develop ontology for constellation -> mapping RDF stuff to existing constellation stuff (Allow for icons etc) <BASIC MAPPING>
        //TODO Potentially: Seperate query for mapping from RDF to Consty <SPECIFIC MAPPING>
        //TODO Add triples to constellation graph and update display; RDF view?
        HashMap<String, String> prefixes = new HashMap<>();
        prefixes.put("country", "http://eulersharp.sourceforge.net/2003/03swap/countries#");
        prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
        prefixes.put("jur", "http://sweet.jpl.nasa.gov/2.3/humanJurisdiction.owl#");
        prefixes.put("dce", "http://purl.org/dc/elements/1.1/");
        prefixes.put("dct", "http://purl.org/dc/terms/");
        prefixes.put("owl", "http://www.w3.org/2002/07/owl#");
        prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        prefixes.put("skos", "http://www.w3.org/2004/02/skos/core#");
        prefixes.put("music", "http://neo4j.com/voc/music#");
        prefixes.put("ind", "http://neo4j.com/indiv#");

        GraphRecordStore results = new GraphRecordStore();
        //try (RepositoryConnection conn = repo.getConnection()) {
        // Create query string
        StringBuilder prefixString = new StringBuilder();
        prefixes.forEach((String key, String value) -> {
            prefixString.append("PREFIX ");
            prefixString.append(key);
            prefixString.append(": <");
            prefixString.append(value);
            prefixString.append("> ");
        }
        );

        Map<String, String> predicateMap = new HashMap<>();
        //This map is
        // predicateMap.put("name", GraphRecordStoreUtilities.SOURCE + SpatialConcept.VertexAttribute.COUNTRY);

        predicateMap.put("name", GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexType.PERSON);
        predicateMap.put("type", GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE);
        predicateMap.put("writer", GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexType.PERSON);

        predicateMap.put("artist", AnalyticConcept.VertexType.PERSON.toString()); //May be in a seperate map?

        ArrayList<String> nodeIdentifiers = new ArrayList<>();

        try {
            URL documentUrl = new URL(inputFilename);
            InputStream inputStream = documentUrl.openStream();
            String baseURI = documentUrl.toString();
            RDFFormat format = RDFFormat.TURTLE;

            try (GraphQueryResult res = QueryResults.parseGraphBackground(inputStream, baseURI, format)) {
                while (res.hasNext()) {
                    Statement st = res.next();
                    Resource r = st.getContext();
                    //String ss = r.stringValue();

                    String subjectName = st.getSubject().stringValue();
                    //String subType = getType(nodeTypeMap.get(st.getSubject())).getName();
                    //st.getSubject().getClass().getName()
                    String subjectType = predicateMap.getOrDefault(subjectName.toLowerCase(), "Subject IRI"); // check??

                    //String subRdfType = getResourceTypeShort(pm, nodeTypeMap.get(st.getSubject()));
                    String predicateName = st.getPredicate().getLocalName();
                    //String object = st.getObject().stringValue();

                    for (String prefix : prefixes.values()) {
                        subjectName = removePrefix(subjectName, prefix);
                    }
                    int index;
                    results.add();//Generic

                    if (nodeIdentifiers.contains(subjectName)) {
                        index = nodeIdentifiers.indexOf(subjectName);
                    } else {
                        nodeIdentifiers.add(subjectName);

                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, subjectName);
                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, subjectType);
                        results.set(GraphRecordStoreUtilities.SOURCE + "RDFType", "subRdfType");//Add RDFViewerConcept.VertexAttribute.RDFTYPE

                        index = results.index();

                    }

                    // Process object
                    Value object = st.getObject();
                    String objectName = "";

                    if (object instanceof Literal) {
                        // Literal object values are added as Vertex properties
                        objectName = ((Literal) object).getLabel();
                        System.out.println("\"" + ((Literal) object).getLabel() + "\"");
                        String attribute = predicateMap.getOrDefault(predicateName, GraphRecordStoreUtilities.SOURCE + predicateName);
                        results.set(attribute, objectName);

                    } else if (object instanceof IRI) {
                        // IRI object values are added as a destination node
                        objectName = ((IRI) object).getLocalName();
                        System.out.println("Added in Dest: " + objectName);

                        //String objType = predicateMap.getOrDefault(objectName, object.getClass().getName()); // check??
                        // String objRdfType = getResourceTypeShort(pm, nodeTypeMap.get(st.getObject().asResource()));
                        String objectType = predicateMap.getOrDefault(objectName.toLowerCase(), "object IRI"); // check??"object IRI";

                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, objectName);
                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, objectType);
                        results.set(GraphRecordStoreUtilities.DESTINATION + "RDFType", "subRdfType");//Add RDFViewerConcept.VertexAttribute.RDFTYPE

                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, "rdf tx type"); //ObjectProperty?
                        results.set(GraphRecordStoreUtilities.TRANSACTION + VisualConcept.TransactionAttribute.IDENTIFIER, predicateName);

                    } else {
                        // It's a blank node. Just print out the internal identifier for now.
                        //objectString = object.stringValue();
                        System.out.println("Blank node: " + object);
                    }

                }

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

    private String removePrefix(String value, String prefix) {
        if (value.contentEquals(prefix)) {
            return value;
        } else if (value.contains(prefix)) {
            return value.replaceFirst(prefix, "");
        } else {
            return value;
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
        return "Import Topology from communication network RDF data";
    }

//    private String getResourceTypeShort(PrefixMapping pm, Set<Resource> types) {
////        if (types != null) {
//////            LOGGER.log(Level.INFO, "Get Type {0}", types.toString());
////            if (types.contains(CNT.Node)) {
////                return getShortForm(pm, CNT.Node);
////            } else if (types.contains(CNT.Interface)) {
////                return getShortForm(pm, CNT.Interface);
////            } else if (types.contains(CNT.Segment)) {
////                return getShortForm(pm, CNT.Segment);
////            }
////            return types.stream().map(r -> getShortForm(pm, r)).findFirst().orElse("");
////        }
//        return "";
//    }
//
//    private SchemaVertexType getType(Set<Resource> types) {
////        if (types != null) {
//////        LOGGER.log(Level.INFO, "Get Type {0}", types.toString());
////            if (types.contains(CNT.Node)) {
////                return RDFViewerConcept.VertexType.CNTNode;
////            } else if (types.contains(CNT.Interface)) {
////                return RDFViewerConcept.VertexType.CNTInterface;
////            } else if (types.contains(CNT.Segment)) {
////                return RDFViewerConcept.VertexType.CNTSegment;
////            }
////        }
////        return RDFViewerConcept.VertexType.RDFNODE;
//    }
//
//    private String getShortForm(PrefixMapping pm, Resource res) {
//        if (res.isAnon()) {
//            return res.toString();
//        } else {
//            String shortForm = pm.shortForm(res.getURI());
//            return shortForm;
//        }
//    }
    @Override
    public PluginParameters createParameters() {
        final PluginParameters params = new PluginParameters();

        final PluginParameter<FileParameterValue> openFileParam = FileParameterType.build(INPUT_FILE_PARAMETER_ID);
        openFileParam.setName("Input file");
        openFileParam.setDescription("RDF file");
        //openFileParam.setStringValue("D:\\projects\\encaby\\encaby_ontologies\\src\\test\\resources\\dsto\\encaby\\test\\ontologies\\vxlan_layer1.trig");
        openFileParam.setStringValue("https://raw.githubusercontent.com/jbarrasa/datasets/master/rdf/music.ttl");
        //"http://eulersharp.sourceforge.net/2003/03swap/countries";
        params.addParameter(openFileParam);
        return params;
    }

}
