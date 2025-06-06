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
package au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.utilities;

import java.util.Arrays;
import java.util.List;

/**
 * A list of entity types used for GDELT Importing
 *
 * @author canis-majoris-42
 */
public enum GDELTEntityTypes {
    Person,
    Organisation,
    Theme,
    Location,
    Source,
    URL;

    public static List<String> getValues() {
        return Arrays.asList(
                Arrays.stream(GDELTEntityTypes.values()) // create stream of enum values
                        .map(e -> e.toString()) // convert enum stream to String stream
                        .toArray(String[]::new)
        );
    }
}
