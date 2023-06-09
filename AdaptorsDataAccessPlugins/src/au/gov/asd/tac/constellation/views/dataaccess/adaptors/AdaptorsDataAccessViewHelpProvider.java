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

    private static final String SEP = File.separator;

    @Override
    public Map<String, String> getHelpMap() {
        final Map<String, String> map = new HashMap<>();
        
        
        final String adaptorsModulePath = ".." + SEP + getFrontPath() + "ext" + SEP + "docs" + SEP + "AdaptorsDataAccessPlugins" + SEP + "src" + SEP + "au" + SEP
                + "gov" + SEP + "asd" + SEP + "tac" + SEP + "constellation" + SEP + "views" + SEP + "dataaccess" + SEP + "adaptors" + SEP;

        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.extend.ExtendFromPajekPlugin", adaptorsModulePath + "extend-from-pajek-file.md");
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.extend.ExtendFromGraphMLPlugin", adaptorsModulePath + "extend-from-graphml-file.md");
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.extend.ExtendFromGMLPlugin", adaptorsModulePath + "extend-from-gml-file.md");
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.enrichment.EnrichFromGraphMLPlugin", adaptorsModulePath + "enrich-from-graphml-file.md");
        map.put("au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.enrichment.EnrichFromGMLPlugin", adaptorsModulePath + "enrich-from-gml-file.md");

        return map;
    }

    @Override
    public String getHelpTOC() {
        final String adaptorsModulePath = getFrontPath() + "ext" + SEP + "docs" + SEP + "AdaptorsDataAccessPlugins" + SEP + "src" + SEP + "au" + SEP
                + "gov" + SEP + "asd" + SEP + "tac" + SEP + "constellation" + SEP + "views" + SEP + "dataaccess" + SEP + "adaptors" + SEP + "adaptors-toc.xml";

        return adaptorsModulePath;
    }
    
    private String getFrontPath() {
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
