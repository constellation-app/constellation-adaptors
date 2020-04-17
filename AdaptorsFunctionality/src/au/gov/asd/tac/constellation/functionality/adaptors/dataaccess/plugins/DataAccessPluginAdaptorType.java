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
package au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins;

import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPluginCoreType;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPluginType;
import java.util.ArrayList;
import java.util.List;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author canis_majoris
 */
@ServiceProvider(service = DataAccessPluginType.class)
public class DataAccessPluginAdaptorType extends DataAccessPluginCoreType {

    public static final String HOP = "Hop";
    public static final String ENRICHMENT = "Enrichment";

    @Override
    public List<PositionalDataAccessPluginType> getPluginTypeList() {
        final ArrayList<PositionalDataAccessPluginType> pluginTypeList = new ArrayList<>();
        pluginTypeList.add(new DataAccessPluginType.PositionalDataAccessPluginType(ENRICHMENT, 1400));
        pluginTypeList.add(new DataAccessPluginType.PositionalDataAccessPluginType(HOP, 1500));

        return pluginTypeList;
    }
}
