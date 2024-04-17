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
package au.gov.asd.tac.constellation.views.mapview.adaptors.providers;

import au.gov.asd.tac.constellation.views.mapview.providers.MapProvider;
import de.fhpotsdam.unfolding.core.Coordinate;
import org.openide.util.lookup.ServiceProvider;
import processing.core.PImage;

/**
 * OpenStreetMap map.
 *
 * @author cygnus_x-1
 */
@ServiceProvider(service = MapProvider.class, position = Integer.MAX_VALUE - 2)
public class OpenStreetMapProvider extends MapProvider {

    @Override
    public String getName() {
        return "OpenStreetMap";
    }

    @Override
    public int zoomLevels() {
        return 20;
    }

    @Override
    public PImage getTile(final Coordinate coordinate) {
        return null;
    }

    @Override
    public String[] getTileUrls(final Coordinate coordinate) {
        // TODO: supply a special user agent string or this will be blocked
        final String url = String.format(
                "https://tile.openstreetmap.org/%s.png",
                getZoomString(coordinate));
        return new String[]{url};
    }
}
