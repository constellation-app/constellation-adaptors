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
package au.gov.asd.tac.constellation.graph.schema.rdf.attribute;

import au.gov.asd.tac.constellation.graph.attribute.AttributeDescription;
import au.gov.asd.tac.constellation.graph.attribute.ObjectAttributeDescription;
import java.util.HashSet;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = AttributeDescription.class)
public final class RDFBlankNodesAttributeDescription extends ObjectAttributeDescription {

    public static final String ATTRIBUTE_NAME = "rdf_blank_nodes";
    public static final int ATTRIBUTE_VERSION = 1;

    public RDFBlankNodesAttributeDescription() {
        super(ATTRIBUTE_NAME);
    }

    @Override
    protected Object convertFromObject(Object object) {
        if (object instanceof String) {
            return new HashSet<>();
        }
        return super.convertFromObject(object);
    }

    @Override
    public int getVersion() {
        return ATTRIBUTE_VERSION;
    }
}
