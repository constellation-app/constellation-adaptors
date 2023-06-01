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
package au.gov.asd.tac.constellation.views.dataaccess.adaptors;

import au.gov.asd.tac.constellation.help.HelpPageProvider;
import au.gov.asd.tac.constellation.help.utilities.Generator;
import java.io.File;
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

    private static final String CODEBASE_NAME = "constellation-adaptors";
    private static final String sep = File.separator;
    private static String frontPath = ".." + sep + ".." + sep + ".." + sep + ".." + sep + CODEBASE_NAME + "\\build\\cluster\\modules\\";

    @Override
    public Map<String, String> getHelpMap() {
        final Map<String, String> map = new HashMap<>();
        if (Generator.getBaseDirectory().contains("\\AppData\\Local")) {
            frontPath = "";
        }
        
        final String adaptorsModulePath = ".." + sep + frontPath + sep + "ext" + sep + "docs" + sep + "AdaptorsDataAccessPlugins" + sep + "src" + sep + "au" + sep
                + "gov" + sep + "asd" + sep + "tac" + sep + "constellation" + sep + "views" + sep + "dataaccess" + sep + "adaptors" + sep;

        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.extend.ExtendFromPajekPlugin", adaptorsModulePath + "extend-from-pajek-file.md");
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.extend.ExtendFromGraphMLPlugin", adaptorsModulePath + "extend-from-graphml-file.md");
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.extend.ExtendFromGMLPlugin", adaptorsModulePath + "extend-from-gml-file.md");
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.enrichment.EnrichFromGraphMLPlugin", adaptorsModulePath + "enrich-from-graphml-file.md");
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.enrichment.EnrichFromGMLPlugin", adaptorsModulePath + "enrich-from-gml-file.md");

        return map;
    }

    @Override
    public String getHelpTOC() {
        final String adaptorsModulePath = frontPath + "ext" + sep + "docs" + sep + "AdaptorsDataAccessPlugins" + sep + "src" + sep + "au" + sep
                + "gov" + sep + "asd" + sep + "tac" + sep + "constellation" + sep + "views" + sep + "dataaccess" + sep + "adaptors" + sep + "adaptors-toc.xml";

        return adaptorsModulePath;
    }
}
