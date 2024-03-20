/*
 * Copyright 2010-2024 Australian Signals Directorate
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
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;
import au.gov.asd.tac.constellation.plugins.Plugin;
import au.gov.asd.tac.constellation.plugins.PluginException;
import au.gov.asd.tac.constellation.plugins.PluginInfo;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.PluginNotificationLevel;
import au.gov.asd.tac.constellation.plugins.PluginType;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameter;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import au.gov.asd.tac.constellation.plugins.parameters.types.IntegerParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.IntegerParameterType.IntegerParameterValue;
import au.gov.asd.tac.constellation.plugins.parameters.types.MultiChoiceParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.MultiChoiceParameterType.MultiChoiceParameterValue;
import au.gov.asd.tac.constellation.utilities.gui.NotifyDisplayer;
import au.gov.asd.tac.constellation.views.dataaccess.CoreGlobalParameters;
import au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.utilities.GDELTDateTime;
import au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.utilities.GDELTExtendingUtilities;
import au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.utilities.GDELTRelationshipTypes;
import au.gov.asd.tac.constellation.views.dataaccess.plugins.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.plugins.DataAccessPluginCoreType;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 * Read graph data from a GDELT file and add it to a graph.
 *
 * @author canis_majoris
 */
@ServiceProviders({
    @ServiceProvider(service = DataAccessPlugin.class),
    @ServiceProvider(service = Plugin.class)})
@PluginInfo(pluginType = PluginType.SEARCH, tags = {"EXTEND"})
@Messages("ExtendFromGDELTPlugin=Extend From GDELT Knowledge Graph")
public class ExtendFromGDELTPlugin extends RecordStoreQueryPlugin implements DataAccessPlugin {

    private static final Logger LOGGER = Logger.getLogger(ExtendFromGDELTPlugin.class.getName());
    
    // plugin parameters
    public static final String CHOICE_PARAMETER_ID = PluginParameter.buildId(ExtendFromGDELTPlugin.class, "choice");
    public static final String LIMIT_PARAMETER_ID = PluginParameter.buildId(ExtendFromGDELTPlugin.class, "limit");

    @Override
    public String getType() {
        return DataAccessPluginCoreType.EXTEND;
    }

    @Override
    public int getPosition() {
        return 500;
    }

    @Override
    public String getDescription() {
        return "Extend on selected graph entities to import from GDELT";
    }

    @Override
    public PluginParameters createParameters() {
        final PluginParameters params = new PluginParameters();

        final PluginParameter<MultiChoiceParameterValue> choices = MultiChoiceParameterType.build(CHOICE_PARAMETER_ID);
        choices.setName("Relationship Options");
        choices.setDescription("Choose which relationship types to extend");
        MultiChoiceParameterType.setOptions(choices, GDELTRelationshipTypes.getValues());
        final List<String> checked = new ArrayList<>();
        checked.add(GDELTRelationshipTypes.Person_Theme.toString());
        MultiChoiceParameterType.setChoices(choices, checked);
        params.addParameter(choices);

        final PluginParameter<IntegerParameterValue> limit = IntegerParameterType.build(LIMIT_PARAMETER_ID);
        limit.setName("Limit");
        limit.setDescription("Maximum number of results to import");
        IntegerParameterType.setMinimum(limit, 1);
        IntegerParameterType.setMaximum(limit, 50000);
        limit.setIntegerValue(20000);
        params.addParameter(limit);

        return params;
    }

    @Override
    protected RecordStore query(final RecordStore query, final PluginInteraction interaction, final PluginParameters parameters) throws InterruptedException, PluginException {

        interaction.setProgress(0, 0, "Hopping...", true);
        // Initialize variables
        final MultiChoiceParameterValue choices = parameters.getMultiChoiceValue(CHOICE_PARAMETER_ID);
        final List<String> options = choices.getChoices();
        final int limit = parameters.getIntegerValue(LIMIT_PARAMETER_ID);

        final ZonedDateTime[] startEnd = CoreGlobalParameters.DATETIME_RANGE_PARAMETER.getDateTimeRangeValue().getZonedStartEnd();
        final ZonedDateTime end = startEnd[1];

        final List<String> labels = query.getAll(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.LABEL);

        if (end != null) {
            try {
                final GDELTDateTime gdt = new GDELTDateTime(end);
                RecordStore results = GDELTExtendingUtilities.hopRelationships(gdt, options, limit, labels);
                
                LocalDate date = LocalDate.parse(gdt.getDay(), DateTimeFormatter.ISO_DATE);
                while (results == null) {
                    date = date.minusDays(1);
                    final ZonedDateTime dateTime = date.atStartOfDay(ZoneId.systemDefault());
                    final GDELTDateTime newGdt = new GDELTDateTime(dateTime);
                    results = GDELTExtendingUtilities.hopRelationships(newGdt, options, limit, labels);
                }
                
                interaction.setProgress(1, 0, "Completed successfully - added " + results.size() + " entities.", true);
                return results;
            } catch (final FileNotFoundException ex) {
                final String errorMsg = "File not found: " + ex.getLocalizedMessage();
                interaction.notify(PluginNotificationLevel.ERROR, errorMsg);
                final Throwable fnfEx = new FileNotFoundException(NotifyDisplayer.BLOCK_POPUP_FLAG + errorMsg);
                fnfEx.setStackTrace(ex.getStackTrace());
                LOGGER.log(Level.SEVERE, fnfEx, () -> errorMsg);
            } catch (final IOException ex) {
                final String errorMsg = "Error reading file: " + ex.getLocalizedMessage();
                interaction.notify(PluginNotificationLevel.ERROR, errorMsg);
                final Throwable ioEx = new IOException(NotifyDisplayer.BLOCK_POPUP_FLAG + errorMsg);
                ioEx.setStackTrace(ex.getStackTrace());
                LOGGER.log(Level.SEVERE, ioEx, () -> errorMsg);
            }
        }

        return new GraphRecordStore();
    }
}
