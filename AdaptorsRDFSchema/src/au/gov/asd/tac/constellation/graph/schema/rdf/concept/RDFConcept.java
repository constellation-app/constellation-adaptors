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
import au.gov.asd.tac.constellation.graph.mergers.ConcatenatedSetGraphAttributeMerger;
import au.gov.asd.tac.constellation.graph.schema.analytic.attribute.VertexTypeAttributeDescription;
import au.gov.asd.tac.constellation.graph.schema.attribute.SchemaAttribute;
import au.gov.asd.tac.constellation.graph.schema.concept.SchemaConcept;
import au.gov.asd.tac.constellation.graph.schema.rdf.icon.RDFIconProvider;
import au.gov.asd.tac.constellation.graph.schema.type.SchemaVertexType;
import au.gov.asd.tac.constellation.utilities.color.ConstellationColor;
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
                .setAttributeMergerId(ConcatenatedSetGraphAttributeMerger.getDefault().getId()) //To append the blank nodes generated while running other plugins. Will it work?
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
                .setAttributeMergerId(ConcatenatedSetGraphAttributeMerger.getDefault().getId())//To append the new types inferred/generated while running other plugins
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

    public static class VertexType {

        public static final SchemaVertexType RDF_CLASS = new SchemaVertexType.Builder("RDF Class")
                .setDescription("An RDF Class")
                .setColor(ConstellationColor.CARROT)
                .setForegroundIcon(RDFIconProvider.RDF_CLASS)
                .build();
//        public static final SchemaVertexType RDFGRAPH = new SchemaVertexType.Builder(String.format("%s Graph", BrandingUtilities.APPLICATION_NAME))
//                .setDescription(String.format("A node representing a %s Graph", BrandingUtilities.APPLICATION_NAME))
//                .setColor(ConstellationColor.AZURE)
//                .setForegroundIcon(RDFIconProvider.RDFCLASS)
//                .build();
        public static final SchemaVertexType SONG = new SchemaVertexType.Builder("Song")
                .setDescription("A node representing a Song, eg. Helter Skelter")
                .setColor(ConstellationColor.OLIVE)
                .setForegroundIcon(RDFIconProvider.SONG)
                .build();
        public static final SchemaVertexType MUSIC_ALBUM = new SchemaVertexType.Builder("Music Album")
                .setDescription("A node representing a Music Album, eg. White Album")
                .setColor(ConstellationColor.BANANA)
                .setForegroundIcon(RDFIconProvider.MUSIC_ALBUM)
                .build();
        public static final SchemaVertexType RDF_TEST = new SchemaVertexType.Builder("RDF Test")
                .setDescription("A temp node type representing RDF types, eg. Resource, Property")
                .setColor(ConstellationColor.CYAN)
                .setForegroundIcon(RDFIconProvider.RDF_TEST)
                .build();
    }

    @Override
    public List<SchemaVertexType> getSchemaVertexTypes() {
        final List<SchemaVertexType> schemaVertexTypes = new ArrayList<>();
//        schemaVertexTypes.add(VertexType.RDFGRAPH);

        schemaVertexTypes.add(VertexType.RDF_CLASS);
        schemaVertexTypes.add(VertexType.SONG);
        schemaVertexTypes.add(VertexType.MUSIC_ALBUM);
        schemaVertexTypes.add(VertexType.RDF_TEST);
        return Collections.unmodifiableList(schemaVertexTypes);
    }

    @Override
    public SchemaVertexType getDefaultSchemaVertexType() {
        return SchemaVertexType.unknownType();
    }
//    public static class TransactionType {
//
//        public static final SchemaTransactionType COMMUNICATION = new SchemaTransactionType.Builder("Communication")
//                .setDescription("A transaction representing a communication between two entities, eg. a phone made a call to another phone")
//                .setColor(ConstellationColor.EMERALD)
//                .build();
//    }
//
//    @Override
//    public List<SchemaTransactionType> getSchemaTransactionTypes() {
//        final List<SchemaTransactionType> schemaTransactionTypes = new ArrayList<>();
//        schemaTransactionTypes.add(TransactionType.COMMUNICATION);
//        return Collections.unmodifiableList(schemaTransactionTypes);
//    }
//
//    @Override
//    public SchemaTransactionType getDefaultSchemaTransactionType() {
//        return SchemaTransactionType.unknownType();
//    }
}
