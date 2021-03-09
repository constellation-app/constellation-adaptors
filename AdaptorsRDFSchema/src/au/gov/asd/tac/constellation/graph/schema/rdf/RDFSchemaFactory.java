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
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.AnalyticConcept;
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.TemporalConcept;
import au.gov.asd.tac.constellation.graph.schema.analytic.utilities.VertexDominanceCalculator;
import au.gov.asd.tac.constellation.graph.schema.attribute.SchemaAttribute;
import au.gov.asd.tac.constellation.graph.schema.concept.SchemaConcept;
import au.gov.asd.tac.constellation.graph.schema.rdf.concept.RDFConcept;
import au.gov.asd.tac.constellation.graph.schema.type.SchemaTransactionType;
import au.gov.asd.tac.constellation.graph.schema.type.SchemaTransactionTypeUtilities;
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
import org.openide.util.Lookup;
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

    final public static Map<String, String> constellationVertexRDFTypes = new HashMap<>();
    final public static Map<String, String> constellationTransactionRDFTypes = new HashMap<>();

    static {
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.TELEPHONE_IDENTIFIER.getName(), "http://www.constellation-app.com/ns#nodetypetelephoneidentifier");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.EMAIL_ADDRESS.getName(), "http://www.constellation-app.com/ns#nodetypeemail");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.USER_NAME.getName(), "http://www.constellation-app.com/ns#nodetypeusername");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.ONLINE_IDENTIFIER.getName(), "http://www.constellation-app.com/ns#nodetypeonlineidentifier");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.URL.getName(), "http://www.constellation-app.com/ns#nodetypeurl");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.HOST_NAME.getName(), "http://www.constellation-app.com/ns#nodetypehostname");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.ONLINE_LOCATION.getName(), "http://www.constellation-app.com/ns#nodetypeonlinelocation");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.MACHINE_IDENTIFIER.getName(), "http://www.constellation-app.com/ns#nodetypemachineidentifier");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.IPV6.getName(), "http://www.constellation-app.com/ns#nodetypeipv6");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.IPV4.getName(), "http://www.constellation-app.com/ns#nodetypeipv4");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.NETWORK_IDENTIFIER.getName(), "http://www.constellation-app.com/ns#nodetypenetworkidentifier");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.PERSON.getName(), "http://www.constellation-app.com/ns#nodetypeperson");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.ORGANISATION.getName(), "http://www.constellation-app.com/ns#nodetypeorganisation");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.DOCUMENT.getName(), "http://www.constellation-app.com/ns#nodetypedocument");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.GEOHASH.getName(), "http://www.constellation-app.com/ns#nodetypegeohash");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.MGRS.getName(), "http://www.constellation-app.com/ns#nodetypemgrs");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.COUNTRY.getName(), "http://www.constellation-app.com/ns#nodetypecountry");
        constellationVertexRDFTypes.put(AnalyticConcept.VertexType.LOCATION.getName(), "http://www.constellation-app.com/ns#nodetypelocation");
    }

    static {
        constellationTransactionRDFTypes.put(AnalyticConcept.TransactionType.COMMUNICATION.getName(), "http://www.constellation-app.com/ns#transactiontypecommunication");
        constellationTransactionRDFTypes.put(AnalyticConcept.TransactionType.CORRELATION.getName(), "http://www.constellation-app.com/ns#transactiontypecorrelation");
        constellationTransactionRDFTypes.put(AnalyticConcept.TransactionType.LOCATION.getName(), "http://www.constellation-app.com/ns#transactiontypelocation");
        constellationTransactionRDFTypes.put(AnalyticConcept.TransactionType.NETWORK.getName(), "http://www.constellation-app.com/ns#transactiontypenetwork");
        constellationTransactionRDFTypes.put(AnalyticConcept.TransactionType.RELATIONSHIP.getName(), "http://www.constellation-app.com/ns#transactiontyperelationship");
        constellationTransactionRDFTypes.put(AnalyticConcept.TransactionType.BEHAVIOUR.getName(), "http://www.constellation-app.com/ns#transactiontypebehaviour");
        constellationTransactionRDFTypes.put(AnalyticConcept.TransactionType.SIMILARITY.getName(), "http://www.constellation-app.com/ns#transactiontypesimilarity");
        constellationTransactionRDFTypes.put(AnalyticConcept.TransactionType.CREATED.getName(), "http://www.constellation-app.com/ns#transactiontypecreated");
        constellationTransactionRDFTypes.put(AnalyticConcept.TransactionType.REFERENCED.getName(), "http://www.constellation-app.com/ns#transactiontypereferenced");
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
            final int vertexTypeAttribute = AnalyticConcept.VertexAttribute.TYPE.ensure(graph);
            final int vertexRDFTypesAttribute = RDFConcept.VertexAttribute.CONSTELLATIONRDFTYPES.ensure(graph);
            final int vertexLabelAttribute = VisualConcept.VertexAttribute.LABEL.ensure(graph);

            SchemaVertexType type = graph.getObjectValue(vertexTypeAttribute, vertexId);

            //SchemaVertexType constellationrdfType = graph.getObjectValue(vertexRDFTypesAttribute, vertexId);
            final String constellationrdfType = graph.getStringValue(vertexRDFTypesAttribute, vertexId);


            if (constellationrdfType != null) {
                type = resolveVertexType(constellationrdfType);

                if (type != null && type != SchemaVertexTypeUtilities.getDefaultType() && !type.equals(graph.getObjectValue(vertexTypeAttribute, vertexId))) {
                    graph.setObjectValue(vertexTypeAttribute, vertexId, type);
                }
            }

            final String label = graph.getStringValue(vertexLabelAttribute, vertexId).split("<")[0];
            super.completeVertex(graph, vertexId);
            final String typeLabel = new StringBuilder("<")
                                .append(type != null ? type.getName() : "Unknown")
                                .append(">")
                                .toString();
            // restore value overwritten by super
            graph.setStringValue(vertexLabelAttribute, vertexId, label + typeLabel);
            for (int i = 0; i < graph.getAttributeCount(GraphElementType.VERTEX); i++) {
                final int attribute = graph.getAttribute(GraphElementType.VERTEX, i);
                if (graph.getAttributeName(attribute).contains("prefLabel")) {
                    final String prefLabel = graph.getStringValue(attribute, vertexId);
                    if (prefLabel != null) {
                        // use prefLabel instead
                        graph.setStringValue(vertexLabelAttribute, vertexId, prefLabel + typeLabel);
                    }
                    break;
                }
            }
        }

        @Override
        public SchemaVertexType resolveVertexType(final String constellationrdfType) {
            // read file
            //
            // creating schemavertextypes
            //
            LOGGER.log(Level.INFO, "TYPE: {0}", constellationrdfType);
            
            final VertexDominanceCalculator domCalc = Lookup.getDefault().lookup(VertexDominanceCalculator.class);
            final List<SchemaVertexType> types = domCalc.getTypePriority();
            for (final SchemaVertexType type : types) {
                if (constellationrdfType.contains(constellationVertexRDFTypes.get(type.getName()))) {
                    return type;
                }
            }
            return SchemaVertexTypeUtilities.getDefaultType();
        }
        
        @Override
        public void newTransaction(final GraphWriteMethods graph, final int transactionId) {
            super.newTransaction(graph, transactionId);

            graph.validateKey(GraphElementType.TRANSACTION, transactionId, false);
            completeTransaction(graph, transactionId);
        }

        @Override
        public void completeTransaction(final GraphWriteMethods graph, final int transactionId) {
            final int transactionTypeAttribute = AnalyticConcept.TransactionAttribute.TYPE.ensure(graph);
            final int transactionRDFTypesAttribute = RDFConcept.TransactionAttribute.CONSTELLATIONRDFTYPES.ensure(graph);

            SchemaTransactionType type = graph.getObjectValue(transactionTypeAttribute, transactionId);

            final String constellationrdfType = graph.getStringValue(transactionRDFTypesAttribute, transactionId);

            if (constellationrdfType != null) {
                type = resolveTransactionType(constellationrdfType);

                if (type != null && type != SchemaTransactionTypeUtilities.getDefaultType() && !type.equals(graph.getObjectValue(transactionTypeAttribute, transactionId))) {
                    graph.setObjectValue(transactionTypeAttribute, transactionId, type);
                }
            }
            super.completeTransaction(graph, transactionId);

        }

        @Override
        public SchemaTransactionType resolveTransactionType(final String constellationrdfType) {

            if (constellationrdfType.contains(constellationTransactionRDFTypes.get(AnalyticConcept.TransactionType.COMMUNICATION.getName()))) {
                return AnalyticConcept.TransactionType.COMMUNICATION;
            } else if (constellationrdfType.contains(constellationTransactionRDFTypes.get(AnalyticConcept.TransactionType.CORRELATION.getName()))) {
                return AnalyticConcept.TransactionType.CORRELATION;
            } else if (constellationrdfType.contains(constellationTransactionRDFTypes.get(AnalyticConcept.TransactionType.LOCATION.getName()))) {
                return AnalyticConcept.TransactionType.LOCATION;
            } else if (constellationrdfType.contains(constellationTransactionRDFTypes.get(AnalyticConcept.TransactionType.NETWORK.getName()))) {
                return AnalyticConcept.TransactionType.NETWORK;
            } else if (constellationrdfType.contains(constellationTransactionRDFTypes.get(AnalyticConcept.TransactionType.RELATIONSHIP.getName()))) {
                return AnalyticConcept.TransactionType.RELATIONSHIP;
            } else if (constellationrdfType.contains(constellationTransactionRDFTypes.get(AnalyticConcept.TransactionType.BEHAVIOUR.getName()))) {
                return AnalyticConcept.TransactionType.BEHAVIOUR;
            } else if (constellationrdfType.contains(constellationTransactionRDFTypes.get(AnalyticConcept.TransactionType.SIMILARITY.getName()))) {
                return AnalyticConcept.TransactionType.SIMILARITY;
            } else if (constellationrdfType.contains(constellationTransactionRDFTypes.get(AnalyticConcept.TransactionType.CREATED.getName()))) {
                return AnalyticConcept.TransactionType.CREATED;
            } else if (constellationrdfType.contains(constellationTransactionRDFTypes.get(AnalyticConcept.TransactionType.REFERENCED.getName()))) {
                return AnalyticConcept.TransactionType.REFERENCED;
            }

            return SchemaTransactionTypeUtilities.getDefaultType();
        }

        @Override
        public int getVertexAliasAttribute(final GraphReadMethods graph) {
            return VisualConcept.VertexAttribute.LABEL.get(graph);
        }
    }
}
