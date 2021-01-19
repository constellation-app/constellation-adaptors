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
package au.gov.asd.tac.constellation.graph.schema.rdf.concept;

import au.gov.asd.tac.constellation.graph.GraphElementType;
import au.gov.asd.tac.constellation.graph.attribute.StringAttributeDescription;
import au.gov.asd.tac.constellation.graph.schema.analytic.attribute.VertexTypeAttributeDescription;
import au.gov.asd.tac.constellation.graph.schema.attribute.SchemaAttribute;
import au.gov.asd.tac.constellation.graph.schema.concept.SchemaConcept;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author arcturus
 */
@ServiceProvider(service = SchemaConcept.class)
public class RDFConcept extends SchemaConcept {

    @Override
    public String getName() {
        return "RDF";
    }

    @Override
    public Set<Class<? extends SchemaConcept>> getParents() {
        final Set<Class<? extends SchemaConcept>> parentSet = new HashSet<>();
        parentSet.add(SchemaConcept.class);
        return Collections.unmodifiableSet(parentSet);
    }

    public static class GraphAttribute {

        public static final SchemaAttribute RDF_BLANK_NODES = new SchemaAttribute.Builder(GraphElementType.GRAPH, "rdf_blank_nodes", "rdf_blank_nodes")
                .setDescription("RDF Blank Nodes")
                .create()
                .build();
    }

    public static class VertexAttribute {

        public static final SchemaAttribute RDFIDENTIFIER = new SchemaAttribute.Builder(GraphElementType.VERTEX, StringAttributeDescription.ATTRIBUTE_NAME, "rdf_identifier")
                .setDescription("The RDF identifier of the node")
                .create()
                .build();

        public static final SchemaAttribute RDFTYPES = new SchemaAttribute.Builder(GraphElementType.VERTEX, VertexTypeAttributeDescription.ATTRIBUTE_NAME, "RDF_types")
                .setDescription("The RDF types of the node")
                .create()
                .build();
    }

    @Override
    public Collection<SchemaAttribute> getSchemaAttributes() {
        final List<SchemaAttribute> schemaAttributes = new ArrayList<>();
        schemaAttributes.add(GraphAttribute.RDF_BLANK_NODES);
        schemaAttributes.add(VertexAttribute.RDFIDENTIFIER);
        schemaAttributes.add(VertexAttribute.RDFTYPES);
        return Collections.unmodifiableCollection(schemaAttributes);
    }

}
