/*
 * Copyright 2010-2020 Australian Signals Directorate
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
package au.gov.asd.tac.constellation.graph.schema.rdf.icon;

import au.gov.asd.tac.constellation.utilities.icon.ConstellationIcon;
import au.gov.asd.tac.constellation.utilities.icon.ConstellationIconProvider;
import au.gov.asd.tac.constellation.utilities.icon.FileIconData;
import java.util.ArrayList;
import java.util.List;
import org.openide.util.lookup.ServiceProvider;

/**
 * RDF specific icons
 *
 */
@ServiceProvider(service = ConstellationIconProvider.class)
public class RDFIconProvider implements ConstellationIconProvider {

    private static final String CODE_NAME_BASE = "au.gov.asd.tac.constellation.utilities";

    private static final String CLASS_CATEGORY = "Class";
    private static final String DOCUMENT_CATEGORY = "Document";
    private static final String INTERNET_CATEGORY = "Internet";
    private static final String MISCELLANEOUS_CATEGORY = "Miscellaneous";

    public static final ConstellationIcon RDFCLASS = new ConstellationIcon.Builder("RDFClass", new FileIconData("modules/ext/icons/rdfclass.png", CODE_NAME_BASE))
            .addCategory(CLASS_CATEGORY)
            .build();
//    public static final ConstellationIcon PERSON = new ConstellationIcon.Builder("Person", new FileIconData("modules/ext/icons/person.png", CODE_NAME_BASE))
//            .addCategory(DOCUMENT_CATEGORY)
//            .build();

    @Override
    public List<ConstellationIcon> getIcons() {
        List<ConstellationIcon> rdfIcons = new ArrayList<>();

        rdfIcons.add(RDFCLASS);
        //analyticIcons.add(PERSON);

        return rdfIcons;
    }
}
