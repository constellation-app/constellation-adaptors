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
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPluginCoreType;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import javafx.stage.FileChooser;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 * Read graph data from a GML file and add it to a graph.
 *
 * @author canis_majoris
 */
@ServiceProviders({
    @ServiceProvider(service = DataAccessPlugin.class),
    @ServiceProvider(service = Plugin.class)})
@PluginInfo(pluginType = PluginType.IMPORT, tags = {"IMPORT", "EXTEND"})
@Messages("ExtendFromGMLPlugin=Extend From GML File")
public class ExtendFromGMLPlugin extends RecordStoreQueryPlugin implements DataAccessPlugin {

    // plugin parameters
    public static final String FILE_PARAMETER_ID = PluginParameter.buildId(ExtendFromGMLPlugin.class, "file");
    public static final String INCOMING_PARAMETER_ID = PluginParameter.buildId(ExtendFromGMLPlugin.class, "in");
    public static final String OUTGOING_PARAMETER_ID = PluginParameter.buildId(ExtendFromGMLPlugin.class, "out");
    public static final String EDGE_TAG = "edge";
    public static final String START_TAG = "[";
    public static final String END_TAG = "]";

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
        return "Select a GML File and build a network extending out from the selected graph nodes";
    }

    @Override
    public PluginParameters createParameters() {
        final PluginParameters params = new PluginParameters();

        /**
         * The GML file to read from
         */
        final PluginParameter<FileParameterValue> file = FileParameterType.build(FILE_PARAMETER_ID);
        FileParameterType.setFileFilters(file, new FileChooser.ExtensionFilter("GML files", "*.gml"));
        FileParameterType.setKind(file, FileParameterType.FileParameterKind.OPEN);
        file.setName("GML File");
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
        final RecordStore edgeRecords = new GraphRecordStore();

        interaction.setProgress(0, 0, "Hopping...", true);
        /**
         * Initialize variables
         */
        final String filename = parameters.getParameters().get(FILE_PARAMETER_ID).getStringValue();
        final boolean incoming = parameters.getParameters().get(INCOMING_PARAMETER_ID).getBooleanValue();
        final boolean outgoing = parameters.getParameters().get(OUTGOING_PARAMETER_ID).getBooleanValue();
        
        BufferedReader in = null;
        String line;
        boolean edge = false;
        final HashMap<String, String> edgeKV = new HashMap<>();
        
        if (incoming || outgoing) {
            final List<String> labels = query.getAll(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER);
        
            try {
                // Open file and loop through lines
                in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith(EDGE_TAG)) {
                        edge = true;
                        edgeKV.clear();
                        edgeKV.put(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, filename);
                    }
                    else if (line.startsWith(START_TAG)) {
                        //do nothing
                    }
                    else if (line.startsWith(END_TAG)) {
                        edge = false;
                        if ((incoming && labels.contains(edgeKV.get(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER))) ||
                            (outgoing && labels.contains(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER))) {
                            edgeRecords.add();
                            edgeRecords.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, filename);
                            for (final String key : edgeKV.keySet()) {
                                edgeRecords.set(key, edgeKV.get(key));
                            }
                        }
                    }
                    else {
                        if (edge) {
                            try {
                                final String key = line.split(" ")[0].trim();
                                final String value = line.split(" ")[1].trim().replace("\"", "");
                                switch (key) {
                                    case "source":
                                        edgeKV.put(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, value);
                                        break;
                                    case "target":
                                        edgeKV.put(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, value);
                                        break;
                                    default:
                                        edgeKV.put(GraphRecordStoreUtilities.TRANSACTION + key, value);
                                        break;
                                }
                            }  catch (final ArrayIndexOutOfBoundsException ex) {
                            }
                        }
                    }  
                }

            } catch (final FileNotFoundException ex) {
                interaction.notify(PluginNotificationLevel.ERROR, "File " + filename + " not found");
            } catch (final IOException ex) {
                interaction.notify(PluginNotificationLevel.ERROR, "Error reading file: " + filename);
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
        
        final RecordStore result = new GraphRecordStore();
        result.add(edgeRecords);
        
        interaction.setProgress(1, 0, "Completed successfully - added " + result.size() + " entities.", true);
        return result;
    }
}
