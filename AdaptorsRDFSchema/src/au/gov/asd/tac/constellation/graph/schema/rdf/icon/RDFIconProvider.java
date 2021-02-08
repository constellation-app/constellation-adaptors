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

    private static final String CODE_NAME_BASE = "au.gov.asd.tac.constellation.graph.schema.rdf";

    private static final String CLASS_CATEGORY = "Class";
    private static final String MUSIC_CATEGORY = "Music";
    private static final String MISCELLANEOUS_CATEGORY = "Miscellaneous";

    public static final ConstellationIcon RDF_CLASS = new ConstellationIcon.Builder("RDF Class", new FileIconData("modules/ext/icons/cloud.png", CODE_NAME_BASE))
            .addCategory(CLASS_CATEGORY)
            .build();
    public static final ConstellationIcon SONG = new ConstellationIcon.Builder("Song", new FileIconData("modules/ext/icons/song.png", CODE_NAME_BASE))
            .addCategory(MUSIC_CATEGORY)
            .build();
    public static final ConstellationIcon MUSIC_ALBUM = new ConstellationIcon.Builder("Music Album", new FileIconData("modules/ext/icons/music_album.png", CODE_NAME_BASE))
            .addCategory(MUSIC_CATEGORY)
            .build();
    public static final ConstellationIcon RDF_TEST = new ConstellationIcon.Builder("RDF Test", new FileIconData("modules/ext/icons/star.png", CODE_NAME_BASE))
            .addCategory(CLASS_CATEGORY)
            .build();

    @Override
    public List<ConstellationIcon> getIcons() {
        List<ConstellationIcon> rdfIcons = new ArrayList<>();

        rdfIcons.add(RDF_CLASS);
        rdfIcons.add(SONG);
        rdfIcons.add(MUSIC_ALBUM);
        rdfIcons.add(RDF_TEST);

        return rdfIcons;
    }
}
