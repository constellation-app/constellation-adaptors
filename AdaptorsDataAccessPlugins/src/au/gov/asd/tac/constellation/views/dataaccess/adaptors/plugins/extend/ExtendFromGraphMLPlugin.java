/*
 * Copyright 2010-2021 Australian Signals Directorate
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
package au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.extend;

import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStoreUtilities;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.AnalyticConcept;
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;
import au.gov.asd.tac.constellation.plugins.Plugin;
import au.gov.asd.tac.constellation.plugins.PluginException;
import au.gov.asd.tac.constellation.plugins.PluginInfo;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.PluginNotificationLevel;
import au.gov.asd.tac.constellation.plugins.PluginType;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameter;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import au.gov.asd.tac.constellation.plugins.parameters.types.BooleanParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.BooleanParameterType.BooleanParameterValue;
import au.gov.asd.tac.constellation.plugins.parameters.types.FileParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.FileParameterType.FileParameterValue;
import au.gov.asd.tac.constellation.utilities.xml.XmlUtilities;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPluginCoreType;
import au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.utilities.GraphMLUtilities;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javafx.stage.FileChooser;
import javax.xml.transform.TransformerException;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Read graph data from a GraphML file and add it to a graph.
 * This plugin reads the existing selection on the graph and retrieves a one-hop neighbourhood of all adjacent nodes.
 *
 * @author canis_majoris
 */
@ServiceProviders({
    @ServiceProvider(service = DataAccessPlugin.class),
    @ServiceProvider(service = Plugin.class)})
@PluginInfo(pluginType = PluginType.IMPORT, tags = {"IMPORT", "EXTEND"})
@Messages("ExtendFromGraphMLPlugin=Extend From GraphML File")
public class ExtendFromGraphMLPlugin extends RecordStoreQueryPlugin implements DataAccessPlugin {

    // plugin parameters
    public static final String FILE_PARAMETER_ID = PluginParameter.buildId(ExtendFromGraphMLPlugin.class, "file");
    public static final String INCOMING_PARAMETER_ID = PluginParameter.buildId(ExtendFromGraphMLPlugin.class, "in");
    public static final String OUTGOING_PARAMETER_ID = PluginParameter.buildId(ExtendFromGraphMLPlugin.class, "out");
    public static final String GRAPHML_TAG = "graphml";
    public static final String GRAPH_TAG = "graph";
    public static final String DIRECTION_TAG = "edgedefault";
    public static final String NODE_TAG = "node";
    public static final String DATA_TAG = "data";
    public static final String DEFAULT_TAG = "default";
    public static final String ID_TAG = "id";
    public static final String EDGE_TAG = "edge";
    public static final String EDGE_SRC_TAG = "source";
    public static final String EDGE_DST_TAG = "target";
    public static final String KEY_TAG = "key";
    public static final String KEY_NAME_TAG = "attr.name";
    public static final String KEY_TYPE_TAG = "attr.type";
    public static final String NAME_TYPE_DELIMITER = ",";
    public static final String KEY_FOR_TAG = "for";

    @Override
    public String getType() {
        return DataAccessPluginCoreType.EXTEND;
    }

    @Override
    public int getPosition() {
        return 100;
    }

    @Override
    public String getDescription() {
        return "Select a GraphML File and build a network extending out from the selected graph nodes";
    }

    @Override
    public PluginParameters createParameters() {
        final PluginParameters params = new PluginParameters();

        /**
         * The GraphML file to read from
         */
        final PluginParameter<FileParameterValue> file = FileParameterType.build(FILE_PARAMETER_ID);
        FileParameterType.setFileFilters(file, new FileChooser.ExtensionFilter("GraphML files", "*.graphml"));
        FileParameterType.setKind(file, FileParameterType.FileParameterKind.OPEN);
        file.setName("GraphML File");
        file.setDescription("File to extract graph from");
        params.addParameter(file);

        /**
         * A boolean option for whether to hop on incoming transactions
         */
        final PluginParameter<BooleanParameterValue> in = BooleanParameterType.build(INCOMING_PARAMETER_ID);
        in.setName("Hop On Incoming Transactions");
        in.setDescription("Returns nodes adjacent on Incoming Transactions");
        in.setBooleanValue(true);
        params.addParameter(in);

        /**
         * A boolean option for whether to hop on outgoing transactions
         */
        final PluginParameter<BooleanParameterValue> out = BooleanParameterType.build(OUTGOING_PARAMETER_ID);
        out.setName("Hop On Outgoing Transactions");
        out.setDescription("Returns nodes adjacent on Outgoing Transactions");
        out.setBooleanValue(true);
        params.addParameter(out);

        return params;
    }

    @Override
    protected RecordStore query(final RecordStore query, final PluginInteraction interaction, final PluginParameters parameters) throws InterruptedException, PluginException {
        final RecordStore nodeRecords = new GraphRecordStore();
        final RecordStore edgeRecords = new GraphRecordStore();

        interaction.setProgress(0, 0, "Hopping...", true);
        /**
         * Initialize variables
         */
        final String filename = parameters.getParameters().get(FILE_PARAMETER_ID).getStringValue();
        final boolean incoming = parameters.getParameters().get(INCOMING_PARAMETER_ID).getBooleanValue();
        final boolean outgoing = parameters.getParameters().get(OUTGOING_PARAMETER_ID).getBooleanValue();

        InputStream in = null;
        final HashMap<String,String> nodeAttributes = new HashMap<>();
        final HashMap<String,String> transactionAttributes = new HashMap<>();
        final HashMap<String,String> defaultAttributes = new HashMap<>();

        boolean undirected = false;

        if (incoming || outgoing) {
            final List<String> labels = query.getAll(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER);

            if (labels.isEmpty()) {
                interaction.notify(PluginNotificationLevel.WARNING, "Please select nodes to query in GraphML file");
            }
            else {
                try {
                    // Open file and loop through lines
                    in = new FileInputStream(filename);

                    final XmlUtilities xml = new XmlUtilities();
                    final Document document = xml.read(in, true);
                    final Element documentElement = document.getDocumentElement();

                    /**
                     * Read attribute keys
                     */
                    NodeList keys = documentElement.getElementsByTagName(KEY_TAG);
                    for (int index = 0; index < keys.getLength(); index++) {
                        final Node key = keys.item(index);
                        final NamedNodeMap attributes = key.getAttributes();
                        final String id = attributes.getNamedItem(ID_TAG).getNodeValue();
                        final String name = attributes.getNamedItem(KEY_NAME_TAG).getNodeValue() +
                                NAME_TYPE_DELIMITER +
                                attributes.getNamedItem(KEY_TYPE_TAG).getNodeValue();
                        final String type = attributes.getNamedItem(KEY_FOR_TAG).getNodeValue();

                        if (type.equals(NODE_TAG)) {
                            nodeAttributes.put(id, name);
                        }
                        else {
                            transactionAttributes.put(id, name);
                        }
                        /**
                         * Check for default values
                         */
                        if (key.hasChildNodes()) {
                            NodeList children = key.getChildNodes();
                            for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                                final Node childNode = children.item(childIndex);
                                if (childNode != null && childNode.getNodeName().equals(DEFAULT_TAG)) {
                                    defaultAttributes.put(id, childNode.getTextContent());
                                }
                            }
                        }
                    }

                    /**
                     * Look for graphs
                     */
                    final NodeList graphs = documentElement.getElementsByTagName(GRAPH_TAG);
                    for (int index = 0; index < graphs.getLength(); index++) {
                       final Node graph = graphs.item(index);
                       final NamedNodeMap graph_attributes = graph.getAttributes();
                       final String direction = graph_attributes.getNamedItem(DIRECTION_TAG).getNodeValue();
                       final HashSet<String> toComplete = new HashSet<>();
                       if (direction.equals("undirected")) {
                           undirected = true;
                       }
                       if (graph.hasChildNodes()) {
                            final NodeList children = graph.getChildNodes();
                            for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                                final Node childNode = children.item(childIndex);
                                if (childNode != null) {
                                    switch (childNode.getNodeName()) {
                                        /**
                                         * Find all edges that are relevant
                                         */
                                        case EDGE_TAG:
                                            {
                                                final NamedNodeMap attributes = childNode.getAttributes();
                                                final String id = attributes.getNamedItem(ID_TAG).getNodeValue();
                                                final String source = attributes.getNamedItem(EDGE_SRC_TAG).getNodeValue();
                                                final String target = attributes.getNamedItem(EDGE_DST_TAG).getNodeValue();
                                                if ((incoming && labels.contains(target)) || (outgoing && labels.contains(source))) {
                                                    toComplete.add(source);
                                                    toComplete.add(target);
                                                    edgeRecords.add();
                                                    edgeRecords.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, source);
                                                    edgeRecords.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, "Unknown");
                                                    edgeRecords.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, target);
                                                    edgeRecords.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, "Unknown");
                                                    edgeRecords.set(GraphRecordStoreUtilities.TRANSACTION + VisualConcept.TransactionAttribute.IDENTIFIER, id);
                                                    edgeRecords.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, filename);
                                                    if (undirected) {
                                                        edgeRecords.set(GraphRecordStoreUtilities.TRANSACTION + VisualConcept.TransactionAttribute.DIRECTED, false);
                                                    }
                                                    for (final String key : transactionAttributes.keySet()) {
                                                        if (defaultAttributes.containsKey(key)) {
                                                            final String value = defaultAttributes.get(key);
                                                            final String attr = transactionAttributes.get(key);
                                                            final String attr_name = attr.split(NAME_TYPE_DELIMITER)[0];
                                                            final String attr_type = attr.split(NAME_TYPE_DELIMITER)[1];
                                                            GraphMLUtilities.addAttribute(edgeRecords, GraphRecordStoreUtilities.TRANSACTION, attr_type, attr_name, value);
                                                        }
                                                    }
                                                    if (childNode.hasChildNodes()) {
                                                        GraphMLUtilities.addAttributes(childNode, transactionAttributes, edgeRecords, GraphRecordStoreUtilities.TRANSACTION);
                                                    }
                                                }
                                            break;
                                        }
                                        default:
                                            break;
                                    }
                                }
                            }
                            /**
                             * Now go back to enrich the nodes
                             */
                            for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                                final Node childNode = children.item(childIndex);
                                if (childNode != null) {
                                    switch (childNode.getNodeName()) {
                                        case NODE_TAG:
                                            {
                                            final NamedNodeMap attributes = childNode.getAttributes();
                                            final String id = attributes.getNamedItem(ID_TAG).getNodeValue();
                                            if (toComplete.contains(id)) {
                                                nodeRecords.add();
                                                nodeRecords.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, id);
                                                nodeRecords.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, "Unknown");
                                                nodeRecords.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.SOURCE, filename);
                                                for (final String key : nodeAttributes.keySet()) {
                                                    if (defaultAttributes.containsKey(key)) {
                                                        final String value = defaultAttributes.get(key);
                                                        final String attr = nodeAttributes.get(key);
                                                        final String attr_name = attr.split(NAME_TYPE_DELIMITER)[0];
                                                        final String attr_type = attr.split(NAME_TYPE_DELIMITER)[1];
                                                        GraphMLUtilities.addAttribute(nodeRecords, GraphRecordStoreUtilities.SOURCE, attr_type, attr_name, value);
                                                    }
                                                }
                                                if (childNode.hasChildNodes()) {
                                                    GraphMLUtilities.addAttributes(childNode, nodeAttributes, nodeRecords, GraphRecordStoreUtilities.SOURCE);
                                                }
                                            }
                                            break;
                                        }
                                        default:
                                            break;
                                    }
                                }
                            }
                        }
                    }
                } catch (final FileNotFoundException ex) {
                    interaction.notify(PluginNotificationLevel.ERROR, "File " + filename + " not found");
                } catch (final TransformerException ex) {
                    Exceptions.printStackTrace(ex);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (final IOException ex) {
                            interaction.notify(PluginNotificationLevel.ERROR, "Error reading file: " + filename);
                        }
                    }
                }
            }
        }
        final RecordStore result = new GraphRecordStore();
        result.add(nodeRecords);
        result.add(edgeRecords);
        result.add(nodeRecords);

        interaction.setProgress(1, 0, "Completed successfully - added " + result.size() + " entities.", true);
        return result;
    }
}
