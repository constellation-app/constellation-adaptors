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
import au.gov.asd.tac.constellation.graph.GraphWriteMethods;
import au.gov.asd.tac.constellation.graph.schema.Schema;
import au.gov.asd.tac.constellation.graph.schema.SchemaFactory;
import au.gov.asd.tac.constellation.graph.schema.analytic.AnalyticSchemaFactory;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    final public static Map<String, String> constellationRDFTypes = new HashMap<>();

    static {
        constellationRDFTypes.put(AnalyticConcept.VertexType.TELEPHONE_IDENTIFIER.getName(), "http://www.constellation-app.com/ns#nodetypetelephoneidentifier");
        constellationRDFTypes.put(AnalyticConcept.VertexType.EMAIL_ADDRESS.getName(), "http://www.constellation-app.com/ns#nodetypeemail");
        constellationRDFTypes.put(AnalyticConcept.VertexType.USER_NAME.getName(), "http://www.constellation-app.com/ns#nodetypeusername");
        constellationRDFTypes.put(AnalyticConcept.VertexType.ONLINE_IDENTIFIER.getName(), "http://www.constellation-app.com/ns#nodetypeonlineidentifier");
        constellationRDFTypes.put(AnalyticConcept.VertexType.URL.getName(), "http://www.constellation-app.com/ns#nodetypeurl");
        constellationRDFTypes.put(AnalyticConcept.VertexType.HOST_NAME.getName(), "http://www.constellation-app.com/ns#nodetypehostname");
        constellationRDFTypes.put(AnalyticConcept.VertexType.ONLINE_LOCATION.getName(), "http://www.constellation-app.com/ns#nodetypeonlinelocation");
        constellationRDFTypes.put(AnalyticConcept.VertexType.MACHINE_IDENTIFIER.getName(), "http://www.constellation-app.com/ns#nodetypemachineidentifier");
        constellationRDFTypes.put(AnalyticConcept.VertexType.IPV6.getName(), "http://www.constellation-app.com/ns#nodetypeipv6");
        constellationRDFTypes.put(AnalyticConcept.VertexType.IPV4.getName(), "http://www.constellation-app.com/ns#nodetypeipv4");
        constellationRDFTypes.put(AnalyticConcept.VertexType.NETWORK_IDENTIFIER.getName(), "http://www.constellation-app.com/ns#nodetypenetworkidentifier");
        constellationRDFTypes.put(AnalyticConcept.VertexType.PERSON.getName(), "http://www.constellation-app.com/ns#nodetypeperson");
        constellationRDFTypes.put(AnalyticConcept.VertexType.ORGANISATION.getName(), "http://www.constellation-app.com/ns#nodetypeorganisation");
        constellationRDFTypes.put(AnalyticConcept.VertexType.DOCUMENT.getName(), "http://www.constellation-app.com/ns#nodetypedocument");
        constellationRDFTypes.put(AnalyticConcept.VertexType.GEOHASH.getName(), "http://www.constellation-app.com/ns#nodetypegeohash");
        constellationRDFTypes.put(AnalyticConcept.VertexType.MGRS.getName(), "http://www.constellation-app.com/ns#nodetypemgrs");
        constellationRDFTypes.put(AnalyticConcept.VertexType.COUNTRY.getName(), "http://www.constellation-app.com/ns#nodetypecountry");
        constellationRDFTypes.put(AnalyticConcept.VertexType.LOCATION.getName(), "http://www.constellation-app.com/ns#nodetypelocation");
    }

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
                        VisualConcept.VertexAttribute.IDENTIFIER);
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
            
            RDFConcept.GraphAttribute.RDF_BLANK_NODES.ensure(graph);
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
            final int vertexTypeAttribute = AnalyticConcept.VertexAttribute.TYPE.ensure(graph);
            final int vertexRDFTypesAttribute = RDFConcept.VertexAttribute.CONSTELLATIONRDFTYPES.ensure(graph);
            final int vertexLabelAttribute = VisualConcept.VertexAttribute.LABEL.ensure(graph);

            SchemaVertexType type;// = graph.getObjectValue(vertexTypeAttribute, vertexId);

            //SchemaVertexType constellationrdfType = graph.getObjectValue(vertexRDFTypesAttribute, vertexId);
            final String constellationrdfType = graph.getStringValue(vertexRDFTypesAttribute, vertexId);


            if (constellationrdfType != null) {
                type = resolveVertexType(constellationrdfType);

                if (type != null && type != SchemaVertexTypeUtilities.getDefaultType() && !type.equals(graph.getObjectValue(vertexTypeAttribute, vertexId))) {
                    graph.setObjectValue(vertexTypeAttribute, vertexId, type);
                }
            }

            final String label = graph.getStringValue(vertexLabelAttribute, vertexId);
            super.completeVertex(graph, vertexId);
            // restore value overwritten by super
            graph.setStringValue(vertexLabelAttribute, vertexId, label);
            for (int i = 0; i < graph.getAttributeCount(GraphElementType.VERTEX); i++) {
                final int attribute = graph.getAttribute(GraphElementType.VERTEX, i);
                if (graph.getAttributeName(attribute).contains("prefLabel")) {
                    final String prefLabel = graph.getStringValue(attribute, vertexId);
                    if (prefLabel != null) {
                        // use prefLabel instead
                        graph.setStringValue(vertexLabelAttribute, vertexId, prefLabel);
                    }
                    break;
                }
            }
        }

        @Override
        public SchemaVertexType resolveVertexType(final String constellationrdfType) {
//            LOGGER.info("called RDF resolve type");

            // read file
            //
            // creating schemavertextypes
            //
//            LOGGER.info("called RDF resolve type");
            /**
             * TODO: Add logic here to look at the RDF type and figure out the
             * most appropriate Constellation type to use. We could use the
             * VertexDominanceCalculator or create an RDF version of it if
             * required.
             *
             */
            LOGGER.log(Level.INFO, "TYPE: {0}", constellationrdfType);

            if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.TELEPHONE_IDENTIFIER.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.TELEPHONE_IDENTIFIER.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.EMAIL_ADDRESS.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.EMAIL_ADDRESS.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.USER_NAME.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.USER_NAME.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.ONLINE_IDENTIFIER.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.ONLINE_IDENTIFIER.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.URL.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.URL.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.HOST_NAME.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.HOST_NAME.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.ONLINE_LOCATION.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.ONLINE_LOCATION.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.MACHINE_IDENTIFIER.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.MACHINE_IDENTIFIER.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.IPV6.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.IPV6.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.IPV4.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.IPV4.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.NETWORK_IDENTIFIER.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.NETWORK_IDENTIFIER.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.PERSON.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.PERSON.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.ORGANISATION.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.ORGANISATION.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.DOCUMENT.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.DOCUMENT.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.GEOHASH.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.GEOHASH.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.MGRS.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.MGRS.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.COUNTRY.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.COUNTRY.getName());
            } else if (constellationrdfType.contains(constellationRDFTypes.get(AnalyticConcept.VertexType.LOCATION.getName()))) {
                return SchemaVertexTypeUtilities.getType(AnalyticConcept.VertexType.LOCATION.getName());
            }

            return SchemaVertexTypeUtilities.getDefaultType();
        }
    }
}
