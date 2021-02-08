package au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.enrichment;

/*
 * Copyright 2010-2019 Australian Signals Directorate
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
import au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.utilities.GraphMLUtilities;
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
import au.gov.asd.tac.constellation.plugins.parameters.types.FileParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.FileParameterType.FileParameterValue;
import au.gov.asd.tac.constellation.utilities.xml.XmlUtilities;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPluginCoreType;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
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
 * This plugin reads the existing selection on the graph and retrieves attributes for selected nodes.
 *
 * @author canis_majoris
 */
@ServiceProviders({
    @ServiceProvider(service = DataAccessPlugin.class),
    @ServiceProvider(service = Plugin.class)})
@PluginInfo(pluginType = PluginType.IMPORT, tags = {"IMPORT", "ENRICHMENT"})
@Messages("EnrichFromGraphMLPlugin=Enrich From GraphML File")
public class EnrichFromGraphMLPlugin extends RecordStoreQueryPlugin implements DataAccessPlugin {

    // plugin parameters
    public static final String FILE_PARAMETER_ID = PluginParameter.buildId(EnrichFromGraphMLPlugin.class, "file");
    public static final String GRAPHML_TAG = "graphml";
    public static final String NODE_TAG = "node";
    public static final String DATA_TAG = "data";
    public static final String DEFAULT_TAG = "default";
    public static final String ID_TAG = "id";
    public static final String KEY_TAG = "key";
    public static final String KEY_NAME_TAG = "attr.name";
    public static final String KEY_TYPE_TAG = "attr.type";
    public static final String NAME_TYPE_DELIMITER = ",";
    public static final String KEY_FOR_TAG = "for";

    @Override
    public String getType() {
        return DataAccessPluginCoreType.ENRICHMENT;
    }

    @Override
    public int getPosition() {
        return 100;
    }

    @Override
    public String getDescription() {
        return "Select a GraphML File and add attributes to selected graph nodes";
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
        file.setDescription("File to extract attributes from");
        params.addParameter(file);

        return params;
    }

    @Override
    protected RecordStore query(final RecordStore query, final PluginInteraction interaction, final PluginParameters parameters) throws InterruptedException, PluginException {
        final RecordStore nodeRecords = new GraphRecordStore();

        interaction.setProgress(0, 0, "Enriching...", true);
        /**
         * Initialize variables
         */
        final String filename = parameters.getParameters().get(FILE_PARAMETER_ID).getStringValue();
        
        InputStream in = null;
        HashMap<String,String> nodeAttributes = new HashMap<>();
        HashMap<String,String> defaultAttributes = new HashMap<>();
                
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
                NodeList nodes = documentElement.getElementsByTagName(NODE_TAG);
                for (int index = 0; index < nodes.getLength(); index++) {
                   final Node n = nodes.item(index); 
                   final NamedNodeMap attributes = n.getAttributes();
                    final String id = attributes.getNamedItem(ID_TAG).getNodeValue();
                    if (labels.contains(id)) {
                        nodeRecords.add();
                        nodeRecords.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, id);
                        nodeRecords.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, "Unknown");
                        nodeRecords.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.SOURCE, filename);
                        for (String key : nodeAttributes.keySet()) {
                            if (defaultAttributes.containsKey(key)) {
                                final String value = defaultAttributes.get(key);
                                final String attr = nodeAttributes.get(key);
                                final String attr_name = attr.split(NAME_TYPE_DELIMITER)[0];
                                final String attr_type = attr.split(NAME_TYPE_DELIMITER)[1];
                                GraphMLUtilities.addAttribute(nodeRecords, GraphRecordStoreUtilities.SOURCE, attr_type, attr_name, value);
                            }
                        }       
                        if (n.hasChildNodes()) {
                            GraphMLUtilities.addAttributes(n, nodeAttributes, nodeRecords, GraphRecordStoreUtilities.SOURCE);
                        } 
                    }
                }   
            } catch (FileNotFoundException ex) {
                interaction.notify(PluginNotificationLevel.ERROR, "File " + filename + " not found");
            } catch (TransformerException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        interaction.notify(PluginNotificationLevel.ERROR, "Error reading file: " + filename);
                    } 
                }
            }
        }
        
        final RecordStore result = new GraphRecordStore();
        result.add(nodeRecords);
        
        interaction.setProgress(1, 0, "Completed successfully - added " + result.size() + " entities.", true);
        return result;
    }
}
