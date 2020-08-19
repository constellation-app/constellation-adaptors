package au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.importing;

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
import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.utilities.GDELTImportingUtilities;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import au.gov.asd.tac.constellation.plugins.Plugin;
import au.gov.asd.tac.constellation.plugins.PluginException;
import au.gov.asd.tac.constellation.plugins.PluginInfo;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.PluginType;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameter;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import au.gov.asd.tac.constellation.plugins.parameters.types.IntegerParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.LocalDateParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.MultiChoiceParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.MultiChoiceParameterType.MultiChoiceParameterValue;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPluginCoreType;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openide.util.Exceptions;
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
@PluginInfo(pluginType = PluginType.IMPORT, tags = {"IMPORT"})
@Messages("ImportEntitiesFromGDELTPlugin=Import Entities From GDELT Knowledge Graph")
public class ImportEntitiesFromGDELTPlugin extends RecordStoreQueryPlugin implements DataAccessPlugin {

    // plugin parameters

    @Override
    public String getType() {
        return DataAccessPluginCoreType.IMPORT;
    }

    @Override
    public int getPosition() {
        return 500;
    }
    
    public static final String LOCAL_DATE_PARAMETER_ID = PluginParameter.buildId(ImportEntitiesFromGDELTPlugin.class, "local_date");
    public static final String CHOICE_PARAMETER_ID = PluginParameter.buildId(ImportEntitiesFromGDELTPlugin.class, "choice");
    public static final String LIMIT_PARAMETER_ID = PluginParameter.buildId(ImportEntitiesFromGDELTPlugin.class, "limit");
    
    @Override
    public PluginParameters createParameters() {
        final PluginParameters params = new PluginParameters();

        /**
         * The date to read from
         */
        final PluginParameter<LocalDateParameterType.LocalDateParameterValue> date = LocalDateParameterType.build(LOCAL_DATE_PARAMETER_ID);
        date.setName("Date");
        date.setDescription("Pick a day to import");
        date.setLocalDateValue(LocalDate.of(2020, Month.JANUARY, 1));
        params.addParameter(date);
        
        final PluginParameter<MultiChoiceParameterValue> choices = MultiChoiceParameterType.build(CHOICE_PARAMETER_ID);
        choices.setName("Entity Options");
        choices.setDescription("Choose which entity types to be imported");
        MultiChoiceParameterType.setOptions(choices, Arrays.asList(
                "Person", 
                "Organisation", 
                "Theme", 
                "Location",
                "Source", 
                "URL"));
        final List<String> checked = new ArrayList<>();
        checked.add("Person");
        MultiChoiceParameterType.setChoices(choices, checked);
        params.addParameter(choices);
        
        final PluginParameter<IntegerParameterType.IntegerParameterValue> limit = IntegerParameterType.build(LIMIT_PARAMETER_ID);
        limit.setName("Limit");
        limit.setDescription("Maximum number of results to import");
        IntegerParameterType.setMinimum(limit, 1);
        IntegerParameterType.setMaximum(limit, 50000);
        limit.setIntegerValue(20000);
        params.addParameter(limit);
        
        return params;
        
    }
    
    @Override
    public String getDescription() {
        return "Import Entities from GDELT";
    }

    @Override
    protected RecordStore query(final RecordStore query, final PluginInteraction interaction, final PluginParameters parameters) throws InterruptedException, PluginException {

        interaction.setProgress(0, 0, "Importing...", true);
        /**
         * Initialize variables
         */
        final LocalDate localDate = parameters.getLocalDateValue(LOCAL_DATE_PARAMETER_ID);
        final MultiChoiceParameterValue choices = parameters.getMultiChoiceValue(CHOICE_PARAMETER_ID);
        final List<String> options = choices.getChoices();
        final int limit = parameters.getIntegerValue(LIMIT_PARAMETER_ID);
        if (localDate != null) {            
            try {
                final RecordStore results = GDELTImportingUtilities.retrieveEntities(localDate, options, limit);
                interaction.setProgress(1, 0, "Completed successfully - added " + results.size() + " entities.", true);
                return results;
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        return new GraphRecordStore();
    }
}
