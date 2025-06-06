/*
 * Copyright 2010-2025 Australian Signals Directorate
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
package au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.enrichment;

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
import au.gov.asd.tac.constellation.utilities.file.FileExtensionConstants;
import au.gov.asd.tac.constellation.utilities.gui.NotifyDisplayer;
import au.gov.asd.tac.constellation.views.dataaccess.plugins.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.plugins.DataAccessPluginCoreType;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.stage.FileChooser;
import org.apache.commons.lang3.StringUtils;
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
@PluginInfo(pluginType = PluginType.SEARCH, tags = {"ENRICH"})
@Messages("EnrichFromGMLPlugin=Enrich From GML File")
public class EnrichFromGMLPlugin extends RecordStoreQueryPlugin implements DataAccessPlugin {

    private static final Logger LOGGER = Logger.getLogger(EnrichFromGMLPlugin.class.getName());

    // plugin parameters
    public static final String FILE_PARAMETER_ID = PluginParameter.buildId(EnrichFromGMLPlugin.class, "file");
    public static final String GRAPH_TAG = "graph";
    public static final String NODE_TAG = "node";
    public static final String START_TAG = "[";
    public static final String END_TAG = "]";

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
        return "Select a GML File and import it into your graph";
    }

    @Override
    public PluginParameters createParameters() {
        final PluginParameters params = new PluginParameters();

        // The GML file to read from
        final PluginParameter<FileParameterValue> file = FileParameterType.build(FILE_PARAMETER_ID);
        FileParameterType.setFileFilters(file, new FileChooser.ExtensionFilter("GML files", FileExtensionConstants.GML));
        FileParameterType.setKind(file, FileParameterType.FileParameterKind.OPEN);
        file.setName("GML File");
        file.setDescription("File to extract graph from");
        params.addParameter(file);

        return params;
    }

    @Override
    protected RecordStore query(final RecordStore query, final PluginInteraction interaction, final PluginParameters parameters) throws InterruptedException, PluginException {
        final RecordStore nodeRecords = new GraphRecordStore();

        interaction.setProgress(0, 0, "Enriching...", true);
        // Initialize variables
        final String filename = parameters.getParameters().get(FILE_PARAMETER_ID).getStringValue();

        BufferedReader in = null;
        String line;
        boolean node = false;
        boolean retrieveAttributes = false;

        final List<String> labels = query.getAll(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER);

        try {
            // Open file and loop through lines
            in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith(NODE_TAG)) {
                    node = true;
                } else if (line.startsWith(START_TAG)) {
                    //do nothing
                } else if (line.startsWith(END_TAG)) {
                    node = false;
                    retrieveAttributes = false;
                } else {
                    if (node) {
                        try {
                            // Read node data
                            final String key = line.split(" ")[0].trim();
                            final String value = line.split(" ")[1].trim().replace("\"", "");
                            if (key.equals("id")) {
                                if (labels.contains(value)) {
                                    retrieveAttributes = true;
                                    nodeRecords.add();
                                    nodeRecords.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.SOURCE, filename);
                                    nodeRecords.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, value);
                                }
                            } else {
                                if (retrieveAttributes) {
                                    nodeRecords.set(GraphRecordStoreUtilities.SOURCE + key, value);
                                }
                            }
                        } catch (final ArrayIndexOutOfBoundsException ex) {
                        }
                    }
                }
            }

        } catch (final FileNotFoundException ex) {
            final String errorMsg = StringUtils.isEmpty(filename) ? "File not specified" : "File not found: " + filename;
            interaction.notify(PluginNotificationLevel.ERROR, errorMsg);
            final Throwable fnfEx = new FileNotFoundException(NotifyDisplayer.BLOCK_POPUP_FLAG + errorMsg);
            fnfEx.setStackTrace(ex.getStackTrace());
            LOGGER.log(Level.SEVERE, fnfEx, () -> errorMsg);
        } catch (final IOException ex) {
            final String errorMsg = StringUtils.isEmpty(filename) ? "File not specified " : "Error reading file: " + filename;
            interaction.notify(PluginNotificationLevel.ERROR, errorMsg);
            final Throwable ioEx = new IOException(NotifyDisplayer.BLOCK_POPUP_FLAG + errorMsg);
            ioEx.setStackTrace(ex.getStackTrace());            
            LOGGER.log(Level.SEVERE, ioEx, () -> errorMsg);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException ex) {
                    final String errorMsg = StringUtils.isEmpty(filename) ? "File not specified " : "Error reading file: " + filename;
                    interaction.notify(PluginNotificationLevel.ERROR, errorMsg);
                    final Throwable ioEx = new IOException(NotifyDisplayer.BLOCK_POPUP_FLAG + errorMsg);
                    ioEx.setStackTrace(ex.getStackTrace());            
                    LOGGER.log(Level.SEVERE, ioEx, () -> errorMsg);
                }
            }
        }

        final RecordStore result = new GraphRecordStore();
        result.add(nodeRecords);

        interaction.setProgress(1, 0, "Completed successfully - added " + result.size() + " entities.", true);
        return result;
    }
}
