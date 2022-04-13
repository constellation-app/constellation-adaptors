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
package au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.example;

import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStoreUtilities;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;
import au.gov.asd.tac.constellation.plugins.Plugin;
import au.gov.asd.tac.constellation.plugins.PluginException;
import au.gov.asd.tac.constellation.plugins.PluginInfo;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.PluginType;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameter;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import au.gov.asd.tac.constellation.plugins.parameters.types.ParameterValue;
import au.gov.asd.tac.constellation.plugins.parameters.types.SingleChoiceParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.SingleChoiceParameterType.SingleChoiceParameterValue;
import au.gov.asd.tac.constellation.plugins.parameters.types.StringParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.StringParameterValue;
import au.gov.asd.tac.constellation.views.dataaccess.adaptors.DataAccessPluginAdaptorType;
import au.gov.asd.tac.constellation.views.dataaccess.plugins.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 * Example plugin to connect to the example Gaffer Road TrafficData Source
 *
 * @author GCHQDeveloper601
 */
@ServiceProviders({
    @ServiceProvider(service = DataAccessPlugin.class),
    @ServiceProvider(service = Plugin.class)
})
@PluginInfo(pluginType = PluginType.SEARCH, tags = {"IMPORT", "SEARCH"})
@Messages("SimpleGafferProviderPlugin=Gaffer Simple Query Options")
public class SimpleGafferProviderPlugin extends RecordStoreQueryPlugin implements DataAccessPlugin {

    private static final String GAFFER_URL_LOCATION_PARAMETER_ID = PluginParameter.buildId(SimpleGafferProviderPlugin.class, "URL");
    private static final String GAFFER_QUERY_TYPE_PARAMETER_ID = PluginParameter.buildId(SimpleGafferProviderPlugin.class, "QUERY_TYPE");

    private static final String DEFAULT_GAFFER_URL = "http://localhost:8080";

    public SimpleGafferProviderPlugin() {

    }

    @Override
    public String getType() {
        return DataAccessPluginAdaptorType.EXAMPLE;
    }

    @Override
    public int getPosition() {
        return 10;
    }

    @Override
    public PluginParameters createParameters() {
        final PluginParameters params = new PluginParameters();

        final PluginParameter<StringParameterValue> gafferUrlLocation = StringParameterType.build(GAFFER_URL_LOCATION_PARAMETER_ID);
        gafferUrlLocation.setName("URL");
        gafferUrlLocation.setDescription("Gaffer instance Url");
        gafferUrlLocation.setStringValue(DEFAULT_GAFFER_URL);

        final PluginParameter<SingleChoiceParameterValue> queryOptions = SingleChoiceParameterType.build(GAFFER_QUERY_TYPE_PARAMETER_ID);
        queryOptions.setName("Queries");
        queryOptions.setDescription("Simple query type");
        //Add all GafferSimpleQueryTypes to perform the query
        SingleChoiceParameterType.setOptions(queryOptions, GafferSimpleQueryTypes.stream().map((GafferSimpleQueryTypes e) -> e.getLabel()).collect(Collectors.toList()));

        params.addParameter(gafferUrlLocation);
        params.addParameter(queryOptions);

        return params;
    }

    @Override
    protected RecordStore query(final RecordStore query, final PluginInteraction interaction, final PluginParameters parameters) throws InterruptedException, PluginException {
        query.reset();
        final String url = parameters.getStringValue(GAFFER_URL_LOCATION_PARAMETER_ID);
        final ParameterValue queryToRun = parameters.getSingleChoice(GAFFER_QUERY_TYPE_PARAMETER_ID);
        //Get the GafferSimpleQueryTypes to get the method to execute
        final GafferSimpleQueryTypes gafferSimpleQueryType = GafferSimpleQueryTypes.valueOfLabel((String) queryToRun.getObjectValue());
        final List<String> queryIds = new ArrayList<>();
        while (query.next()) {
            queryIds.add(query.get(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER));
        }
        final RecordStore results = new GraphRecordStore();
        gafferSimpleQueryType.setUrl(url);
        gafferSimpleQueryType.performQuery(queryIds, results);
        return results;
    }
}
