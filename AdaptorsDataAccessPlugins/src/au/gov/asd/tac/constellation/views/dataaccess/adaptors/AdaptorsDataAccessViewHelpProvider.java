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
package au.gov.asd.tac.constellation.views.dataaccess.adaptors;

import au.gov.asd.tac.constellation.help.HelpPageProvider;
import au.gov.asd.tac.constellation.help.utilities.Generator;
import java.util.HashMap;
import java.util.Map;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author aldebaran30701
 */
@ServiceProvider(service = HelpPageProvider.class, position = 1005)
@NbBundle.Messages("AdaptorsDataAccessViewHelpProvider=Adaptors Data Access View Help Provider")
public class AdaptorsDataAccessViewHelpProvider extends HelpPageProvider {
    
    private static final String MODULE_PATH = getFrontPath() + "ext" + SEP + "docs" + SEP + "AdaptorsDataAccessPlugins" + SEP;

    @Override
    public Map<String, String> getHelpMap() {
        final Map<String, String> map = new HashMap<>();
        
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.extend.ExtendFromPajekPlugin", MODULE_PATH + "extend-from-pajek-file.md");
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.extend.ExtendFromGraphMLPlugin", MODULE_PATH + "extend-from-graphml-file.md");
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.extend.ExtendFromGMLPlugin", MODULE_PATH + "extend-from-gml-file.md");
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.extend.ExtendFromGDELTPlugin", MODULE_PATH + "extend-from-gdelt.md");
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.enrichment.EnrichFromGraphMLPlugin", MODULE_PATH + "enrich-from-graphml-file.md");
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.enrichment.EnrichFromGMLPlugin", MODULE_PATH + "enrich-from-gml-file.md");
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.importing.ImportEntitiesFromGDELTPlugin", MODULE_PATH + "import-entities-from-gdelt.md");
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.importing.ImportRelationshipsFromGDELTPlugin", MODULE_PATH + "import-relationships-from-gdelt.md");
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.example.SimpleGafferProviderPlugin", MODULE_PATH + "query-from-gaffer.md");
        return map;
    }

    @Override
    public String getHelpTOC() {
        return MODULE_PATH + "adaptors-toc.xml";
    }
    
    private static String getFrontPath() {
        // check where the application is being run from as the location of help pages is slightly between running from a release zip and running locally from netbeans
        final boolean isRunningLocally = Generator.getBaseDirectory().contains("build" + SEP + "cluster");
        final String codebaseName = "constellation-adaptors";
        
        final StringBuilder frontPathBuilder = new StringBuilder("..").append(SEP).append("..").append(SEP);
        if (isRunningLocally) {
            frontPathBuilder.append("..").append(SEP).append("..").append(SEP);
        }
        frontPathBuilder.append(codebaseName).append(SEP);
        if (isRunningLocally) {
            frontPathBuilder.append("build").append(SEP).append("cluster").append(SEP);
        }
        frontPathBuilder.append("modules").append(SEP);
        
        return frontPathBuilder.toString();
    }
}
