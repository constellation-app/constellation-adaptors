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
package au.gov.asd.tac.constellation.graph.schema.rdf;

import au.gov.asd.tac.constellation.graph.GraphElementType;
import au.gov.asd.tac.constellation.graph.GraphReadMethods;
import au.gov.asd.tac.constellation.graph.GraphWriteMethods;
import au.gov.asd.tac.constellation.graph.schema.Schema;
import au.gov.asd.tac.constellation.graph.schema.SchemaFactory;
import au.gov.asd.tac.constellation.graph.schema.analytic.AnalyticSchemaFactory;
import au.gov.asd.tac.constellation.graph.schema.analytic.attribute.objects.RawData;
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.AnalyticConcept;
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.TemporalConcept;
import au.gov.asd.tac.constellation.graph.schema.attribute.SchemaAttribute;
import au.gov.asd.tac.constellation.graph.schema.concept.SchemaConcept;
import au.gov.asd.tac.constellation.graph.schema.rdf.concept.RDFConcept;
import au.gov.asd.tac.constellation.graph.schema.type.SchemaVertexType;
import au.gov.asd.tac.constellation.graph.schema.type.SchemaVertexTypeUtilities;
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;
import au.gov.asd.tac.constellation.utilities.color.ConstellationColor;
import au.gov.asd.tac.constellation.utilities.icon.AnalyticIconProvider;
import au.gov.asd.tac.constellation.utilities.icon.ConstellationIcon;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author arcturus
 */
@ServiceProvider(service = SchemaFactory.class, position = Integer.MAX_VALUE - 3)
public class RDFSchemaFactory extends AnalyticSchemaFactory {

    // Note: changing this value will break backwards compatibility!
    public static final String RDF_SCHEMA_ID = "au.gov.asd.tac.constellation.graph.schema.RDFSchemaFactory";

    private static final ConstellationIcon ICON_SYMBOL = AnalyticIconProvider.GRAPH;
    private static final ConstellationColor ICON_COLOR = ConstellationColor.PURPLE;

    private static final Logger LOGGER = Logger.getLogger(RDFSchemaFactory.class.getName());

    @Override
    public String getName() {
        return RDF_SCHEMA_ID;
    }

    @Override
    public String getLabel() {
        return "RDF Graph";
    }

    @Override
    public String getDescription() {
        return "This schema provides support for RDF";
    }

    @Override
    public ConstellationIcon getIconSymbol() {
        return ICON_SYMBOL;
    }

    @Override
    public ConstellationColor getIconColor() {
        return ICON_COLOR;
    }

    @Override
    public Set<Class<? extends SchemaConcept>> getRegisteredConcepts() {
        final Set<Class<? extends SchemaConcept>> registeredConcepts = new HashSet<>();
        registeredConcepts.add(SchemaConcept.ConstellationViewsConcept.class);
        registeredConcepts.add(VisualConcept.class);
        registeredConcepts.add(AnalyticConcept.class);
        registeredConcepts.add(RDFConcept.class);
        return Collections.unmodifiableSet(registeredConcepts);
    }

    @Override
    public Schema createSchema() {
        return new RDFSchema(this);
    }

     @Override
    public List<SchemaAttribute> getKeyAttributes(final GraphElementType elementType) {
        final List<SchemaAttribute> keys;
        switch (elementType) {
            case VERTEX:
                keys = Arrays.asList(
                        RDFConcept.VertexAttribute.RDFIDENTIFIER);
                break;
            case TRANSACTION:
                keys = Arrays.asList(VisualConcept.TransactionAttribute.IDENTIFIER,
                        AnalyticConcept.TransactionAttribute.TYPE,
                        TemporalConcept.TransactionAttribute.DATETIME);
                break;
            default:
                keys = Collections.emptyList();
                break;
        }

        return Collections.unmodifiableList(keys);

    }

    protected class RDFSchema extends AnalyticSchema {

        public RDFSchema(final SchemaFactory factory) {
            super(factory);
        }

        @Override
        public void newGraph(final GraphWriteMethods graph) {
            super.newGraph(graph);
            ensureKeyAttributes(graph); // TODO: is this check required if its already done in super?
            
            final int rdfBlankNodesAttributeId = RDFConcept.GraphAttribute.RDF_BLANK_NODES.ensure(graph);
        }

        @Override
        public void newVertex(final GraphWriteMethods graph, final int vertexId) {
            super.newVertex(graph, vertexId);

            graph.validateKey(GraphElementType.VERTEX, vertexId, false);
            completeVertex(graph, vertexId);
        }

        @Override
        public void completeVertex(final GraphWriteMethods graph, final int vertexId) {
            LOGGER.info("called RDF completeVertex()");

            final int vertexIdentifierAttribute = VisualConcept.VertexAttribute.IDENTIFIER.ensure(graph);
            final int vertexRDFIdentifierAttribute = RDFConcept.VertexAttribute.RDFIDENTIFIER.ensure(graph);
            final int vertexTypeAttribute = AnalyticConcept.VertexAttribute.TYPE.ensure(graph);
            final int vertexRDFTypesAttribute = RDFConcept.VertexAttribute.RDFTYPES.ensure(graph);
            final int vertexRawAttribute = AnalyticConcept.VertexAttribute.RAW.ensure(graph);
            final int vertexLabelAttribute = VisualConcept.VertexAttribute.LABEL.ensure(graph);

            String identifier = graph.getStringValue(vertexIdentifierAttribute, vertexId);
            String rdfIdentifier = graph.getStringValue(vertexRDFIdentifierAttribute, vertexId);
            SchemaVertexType type = graph.getObjectValue(vertexTypeAttribute, vertexId);

            //SchemaVertexType rdfTypes = graph.getObjectValue(vertexRDFTypesAttribute, vertexId);
            final String rdfTypes = graph.getStringValue(vertexRDFTypesAttribute, vertexId);

            RawData raw = graph.getObjectValue(vertexRawAttribute, vertexId);
            String label = graph.getStringValue(vertexLabelAttribute, vertexId);

            if (rdfTypes != null) {

                //if (type == null || type.isIncomplete()) {
//                type = rdfTypes != null ? rdfTypes : SchemaVertexTypeUtilities.getDefaultType();
//                type = graph.getSchema().resolveVertexType(type.toString());
                    type = resolveVertexType(rdfTypes);
                //}

                if (type != null && type != SchemaVertexTypeUtilities.getDefaultType() && !type.equals(graph.getObjectValue(vertexTypeAttribute, vertexId))) {
                    graph.setObjectValue(vertexTypeAttribute, vertexId, type);
                }

            }
             super.completeVertex(graph, vertexId);
        }

        @Override
        public SchemaVertexType resolveVertexType(final String rdfTypes) {
//            LOGGER.info("called RDF resolve type");

            /**
             * TODO: Add logic here to look at the RDF type and figure out the
             * most appropriate Constellation type to use. We could use the
             * VertexDominanceCalculator or create an RDF version of it if
             * required.
             *
             */


            LOGGER.log(Level.INFO, "TYPE: {0}", rdfTypes);

            if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypetelephoneidentifier")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.TELEPHONE_IDENTIFIER.getName());
            } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypeemail")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.EMAIL_ADDRESS.getName());
                } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypeusername")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.USER_NAME.getName());
                } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypeonlineidentifier")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.ONLINE_IDENTIFIER.getName());
                } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypeurl")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.URL.getName());
                } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypehostname")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.HOST_NAME.getName());
                } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypeonlinelocation")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.ONLINE_LOCATION.getName());
                } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypemachineidentifier")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.MACHINE_IDENTIFIER.getName());
                } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypeipv6")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.IPV6.getName());
                } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypeipv4")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.IPV4.getName());
                } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypenetworkidentifier")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.NETWORK_IDENTIFIER.getName());
                } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypeperson")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.PERSON.getName());
                 } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypeorganisation")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.ORGANISATION.getName());
                } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypedocument")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.DOCUMENT.getName());
                } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypegeohash")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.GEOHASH.getName());
                } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypemgrs")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.MGRS.getName());
                } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypecountry")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.COUNTRY.getName());
                 } else if (rdfTypes.contains("http://www.constellation-app.com/ns#nodetypelocation")) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.LOCATION.getName());
                 }
          

            return SchemaVertexTypeUtilities.getDefaultType();
        }

        @Override
        public int getVertexAliasAttribute(final GraphReadMethods graph
        ) {
            return VisualConcept.VertexAttribute.LABEL.get(graph);
        }
    }
}
