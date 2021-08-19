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
package au.gov.asd.tac.constellation.functionality.adaptors.whatsnew;

import au.gov.asd.tac.constellation.views.whatsnew.WhatsNewProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 * The {@link WhatsNewProvider} for the CONSTELLATION Adaptors distribution.
 *
 * @author cygnus_x-1
 */
@ServiceProvider(service = WhatsNewProvider.class)
public class AdaptorsWhatsNew extends WhatsNewProvider {

    @Override
    public String getResource() {
        return "whatsnew-adaptors.txt";
    }

    @Override
    public String getSection() {
        return "Adaptors";
    }
}
