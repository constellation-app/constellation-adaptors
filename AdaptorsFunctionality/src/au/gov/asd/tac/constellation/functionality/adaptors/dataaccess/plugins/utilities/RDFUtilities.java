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
package au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.utilities;

import au.gov.asd.tac.constellation.graph.GraphElementType;
import au.gov.asd.tac.constellation.graph.GraphReadMethods;
import au.gov.asd.tac.constellation.graph.GraphWriteMethods;
import au.gov.asd.tac.constellation.graph.LayersConcept;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStoreUtilities;
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.AnalyticConcept;
import au.gov.asd.tac.constellation.graph.schema.rdf.RDFSchemaFactory;
import au.gov.asd.tac.constellation.graph.schema.rdf.concept.RDFConcept;
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;
import au.gov.asd.tac.constellation.utilities.text.SeparatorConstants;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.repository.RepositoryResult;

public class RDFUtilities {

    private static final Logger LOGGER = Logger.getLogger(RDFUtilities.class.getName());
    
    static final boolean VERBOSE = true;
    static final SimpleValueFactory FACTORY = SimpleValueFactory.getInstance();
    private final static String SEPARATOR_TERM = SeparatorConstants.COMMA;

    static final Map<String, Resource> bnodeToSubject = new HashMap<>();
    static final Map<String, String> constellationTypesMap = new HashMap<>(); //Might need to change this when Constellation Sail is used, as it directly calls 'processNextRecord'
    private static final int layer_Mask = 3;

    public static void PopulateRecordStore(final GraphRecordStore recordStore, final RepositoryResult<Statement> repositoryResult, 
            final MultiKeyMap literalToValue, final int layerMask) {
        // TODO- need to remove this if the bNodeStatements are added into the graph attribute by other plugins
        PopulateRecordStore(recordStore, repositoryResult, literalToValue, new HashSet<>(), layerMask);
    }

    public static void PopulateRecordStore(final GraphRecordStore recordStore, final GraphQueryResult res, 
            final MultiKeyMap literalToValue, final int layerMask) {
        // TODO- need to remove this if the bNodeStatements are added into the graph attribute by other plugins
        PopulateRecordStore(recordStore, res, literalToValue, new HashSet<>(), layerMask);
    }

    public static void PopulateRecordStore(final GraphRecordStore recordStore, final RepositoryResult<Statement> repositoryResult, 
            final MultiKeyMap literalToValue, final Set<Statement> bNodeStatements, 
            final int layerMask) {
        for (final Statement statement : repositoryResult) {
            processNextRecord(recordStore, statement, literalToValue, bNodeStatements, layerMask);
        }
    }

    public static void PopulateRecordStore(final GraphRecordStore recordStore, final GraphQueryResult res, 
            final MultiKeyMap literalToValue, final Set<Statement> bNodeStatements, 
            final int layerMask) {
        while (res.hasNext()) {
            processNextRecord(recordStore, res.next(), literalToValue, bNodeStatements, layerMask);
        }
    }

    public static void processNextRecord(final GraphRecordStore recordStore, final Statement statement, 
            final MultiKeyMap literalToValue, final Set<Statement> bNodeStatements, 
            final int layerMask) {
        if (VERBOSE) {
            LOGGER.info("Processing next record...");
        }

        final Resource subject = statement.getSubject();
        final IRI predicate = statement.getPredicate();
        final Value object = statement.getObject();
        final Resource context = statement.getContext();
        Resource parentIRISubject = statement.getPredicate(); // TODO: is this needed as we already have predicate with the same value?

        if (VERBOSE) {
            LOGGER.log(Level.INFO, "Saw Subject: {0}, Predicate: {1}, Object: {2}, Context: {3}", new Object[]{subject, predicate, object, context});
        }

        boolean addAttributes = false;
        boolean objectIsIRI = false;
//        boolean objectIsBNode = false;

        // Add BNODES in a graph attribute
        if ((subject instanceof BNode) || (object instanceof BNode)) {
            bNodeStatements.add(statement);
        } else {

            // PROCESS: Subject
            // ----------------
            String subjectName = null;
            if (subject instanceof Literal) { //Currently the subject can only be a URI or blank node
                subjectName = ((Literal) subject).getLabel();
            } else if (subject instanceof IRI) {
                subjectName = ((IRI) subject).getLocalName();
//        } else if (subject instanceof BNode) {
//            subjectName = ((BNode) subject).stringValue();
//
//            if (bnodeToSubject.containsKey(subject.stringValue())) {
//                //subjectName = ((IRI) bnodeToSubject.get(subject.stringValue())).getLocalName();
//                parentIRISubject = bnodeToSubject.get(subject.stringValue());
//                subjectName = ((IRI) parentIRISubject).getLocalName();
//                addAttributes = true;
//            } else {
//                //TODO we'll skip these nodes for now, but need to handle/convert them somehow
//                LOGGER.log(Level.WARNING, "BNode subject type: {0}, dropping ( because connected to another BNode):  ", subjectName);//won't hit
//
//            }
            } else {
                if (VERBOSE) {
                    LOGGER.log(Level.WARNING, "Unknown subject type: {0}, dropping", subject);
                }

            }

            // PROCESS: Predicate
            // ----------------
            String predicateName = predicate.getLocalName();//predicate.stringValue()

            // PROCESS: Object
            // ----------------
            String objectName = null;
            if (object instanceof Literal) {
                objectName = ((Literal) object).getLabel();
                addAttributes = true;
            } else if (object instanceof IRI) {
                objectName = ((IRI) object).getLocalName();
                ((IRI) object).getNamespace();
                objectIsIRI = true;
//        } else if (object instanceof BNode) {
//            objectName = ((BNode) object).stringValue();
//            objectIsBNode = true;
//            LOGGER.log(Level.WARNING, "BNode object type: {0}, could be dropping, if no further Bnode with same string in the subject is found", objectName);
            } else {
                if (VERBOSE) {
                    LOGGER.log(Level.WARNING, "Unknown object type: {0}, dropping", object);
                }

            }

            if (VERBOSE) {
                LOGGER.log(Level.INFO, "Processing Subject: {0}, Predicate: {1}, Object: {2}, Context: {3}", new Object[]{subjectName, predicateName, objectName, context});
            }

//        if (objectIsBNode) {
//            if (subject instanceof IRI) {//Currently the subject can only be a URI or blank node- if they ever support Literals, need to change this
//                LOGGER.log(Level.WARNING, "RDF IDENTIFIER = ");
//                bnodeToSubject.put(object.stringValue(), subject);
//            } else if (subject instanceof BNode) {
//                //TODO we'll skip these nodes for now, but need to handle/convert them somehow
//                LOGGER.log(Level.WARNING, "RDF IDENTIFIER = ");
//                bnodeToSubject.put(object.stringValue(), parentIRISubject);//Disjointed BNodes doesn't have a valid parentIRISubject
//            } else {
//                LOGGER.log(Level.WARNING, "Invalid RDF IDENTIFIER for subject", subject.stringValue());//won't hit
//            }
//
//        } else

            // Populate the map with Consty Node Types from the mapping file
            // We might need to add this in a seperate function, when we read from a seperate mapping file
            final String objectStringLowerCase = StringUtils.lowerCase(object.stringValue());
            if ("subclassof".equalsIgnoreCase(predicateName) 
                    && RDFSchemaFactory.constellationRDFTypes.containsValue(objectStringLowerCase)) {
                constellationTypesMap.put(StringUtils.lowerCase(subject.stringValue()), objectStringLowerCase);
            }

            if (addAttributes || ("type".equals(predicateName) && subject instanceof BNode)) { // literal object values are added as Vertex properties
                if (VERBOSE) {
                    LOGGER.log(Level.INFO, "Adding Literal \"{0}\"", objectName);
                }

                recordStore.add();
                if (subject instanceof IRI) {//Currently the subject can only be a URI or blank node- if they ever support Literals, need to change this
                    recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, StringUtils.trim(subject.stringValue()).toLowerCase());
                } else if (subject instanceof BNode) {
                    //recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.RDFIDENTIFIER, parentIRISubject.stringValue());//or skip overwriting
                    if (VERBOSE) {
                        LOGGER.log(Level.WARNING, "Subject is a BNode. Add the triples in an attribute of " + parentIRISubject.stringValue());
                    }
                } else {
                    if (VERBOSE) {
                        LOGGER.log(Level.WARNING, " Invalid RDF IDENTIFIER for subject", subject.stringValue());
                    }
                }

                // ***************************** STORING PREDICATE LITERAL ***********************************
                // Handling the case of two entries for the same predicate.
                final String key1 = predicate.toString();
                final String key2 = StringUtils.trim((subject != null) ? subject.stringValue() : "").toLowerCase();

                String value = (object != null) ? object.toString() : "";
                if (literalToValue.containsKey(key1, key2)) {
                    value += SEPARATOR_TERM + (String) literalToValue.get(key1, key2);
                }
                literalToValue.put(key1, key2, value);

                recordStore.set(GraphRecordStoreUtilities.SOURCE + key1, value);
                recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.LABEL, subjectName);
                recordStore.set(GraphRecordStoreUtilities.SOURCE + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));

            } else if ("type".equals(StringUtils.lowerCase(predicateName))) {//TODO need to handle TYPE of BNODES seperately here
                recordStore.add();
                if (subject instanceof IRI) {
                    recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, StringUtils.trim(subject.stringValue()).toLowerCase());
                } else {//Subject is  a BNode
                    if (VERBOSE) {
                        LOGGER.log(Level.WARNING, "Subject is  a BNode. Added the triples above in an attribute of " + parentIRISubject.stringValue());
                    }//or skip overwriting-can it hit hrer?yes  TYPE of BNODES
                }
                recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.LABEL, subjectName);
                recordStore.set(GraphRecordStoreUtilities.SOURCE + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));
                
                // Set RDF Types
                // If there are multiple types, they'll be added as a CSV (by the ConcatenatedSetGraphAttributeMerger)
                // E.g.: "http://www.constellation-app.com/ns#person,http://www.w3.org/2000/01/rdf-schema#resource"
                recordStore.set(GraphRecordStoreUtilities.SOURCE + RDFConcept.VertexAttribute.RDFTYPES, objectStringLowerCase);

                // Set "Constellation RDF Types" based on "RDF Types" (If there are multiple types, they'll be added as a CSV)
                if (constellationTypesMap.containsKey(objectStringLowerCase)) {
                    recordStore.set(GraphRecordStoreUtilities.SOURCE + RDFConcept.VertexAttribute.CONSTELLATIONRDFTYPES, constellationTypesMap.get(objectStringLowerCase));
                }
            } else if (objectIsIRI) { //subject.stringValue().startsWith("http") &&  predicate.stringValue().startsWith("http")) {
                {
                    recordStore.add();
                }
                if (!(subject instanceof IRI && predicate instanceof IRI)) {
                    if (VERBOSE) {
                        LOGGER.log(Level.WARNING, "Invalid RDF IDENTIFIER. Subject: " + subject.stringValue() + " or Predicate: " + predicate.stringValue());
                    }
                }
                recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, StringUtils.trim(subject.stringValue()).toLowerCase());
                recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.LABEL, subjectName);
                recordStore.set(GraphRecordStoreUtilities.SOURCE + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));

                if (StringUtils.isNotBlank(objectName)) {
                    recordStore.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, StringUtils.trim(objectStringLowerCase));
                    recordStore.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.LABEL, objectName);
                    recordStore.set(GraphRecordStoreUtilities.DESTINATION + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));

                    recordStore.set(GraphRecordStoreUtilities.TRANSACTION + RDFConcept.TransactionAttribute.RDFIDENTIFIER, StringUtils.trim(predicate.stringValue()).toLowerCase());
                    recordStore.set(GraphRecordStoreUtilities.TRANSACTION + VisualConcept.TransactionAttribute.IDENTIFIER, predicateName);
                    recordStore.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.CORRELATION);//TODO FIX TYPE
                    recordStore.set(GraphRecordStoreUtilities.TRANSACTION + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));
                }
            } else {
                if (VERBOSE) {
                    LOGGER.log(Level.WARNING, "Predicate: {0} not mapped.", predicateName);
                }
            }
        }
    }

    public static Model getGraphModel(final GraphReadMethods graph) {
        final Model model = new LinkedHashModel();

        // statement: vertex X is a thing Y
        final int vxCount = graph.getVertexCount();
        for (int i = 0; i < vxCount; i++) {
            final int vertexId = graph.getVertex(i);
            addVertexToModel(graph, model, vertexId);
        }

        // statement: source vertex -> transaction type -> destination vertex
        final int txCount = graph.getTransactionCount();
        for (int i = 0; i < txCount; i++) {
            final int transactionId = graph.getTransaction(i);
            addTransactionToModel(graph, model, transactionId);
        }
        addBlankNodesToModel(graph, model);

        return model;
    }

    public static String getIRI(final String rdfIdentifier) {
        if (rdfIdentifier != null && !StringUtils.isBlank(rdfIdentifier) && rdfIdentifier.trim().startsWith("http")) {
            return rdfIdentifier;
        } else {
            return "http://consty.local#" + "/" + rdfIdentifier; // TODO: check if the RDF type is defined, if so then use it, otherwise is a http://consty.local#
        }
    }

    public static void addNodeAttributes(final GraphReadMethods graph, final Model model, final int vertexId) {
        final int vertexAttributeCount = graph.getAttributeCount(GraphElementType.VERTEX);
        final int vertexIdentifierAttributeId = VisualConcept.VertexAttribute.IDENTIFIER.get(graph);

        //Loops through all of the Node attributes and add the triples stored there, back into the model
        for (int vertexAttributePosition = 0; vertexAttributePosition < vertexAttributeCount; vertexAttributePosition++) {
            // Grab the subject, predicate and object from RDF_Identifier, AttributeName and AttributeValue respectively.
            // Create a statement from this, and store that back into the model.
            // Do not create a null literal object
            final int vertexAttributeId = graph.getAttribute(GraphElementType.VERTEX, vertexAttributePosition);
            final String vxAttributeName = graph.getAttributeName(vertexAttributeId);
            final String rdfObject = graph.getStringValue(vertexAttributeId, vertexId);

            // only make a new statement from the attributes whose name is an IRI - meaning it is the predicate.
            // Tried to "validate" the IRI using instanceof, also createIRI, but none validated it.
            // TODO: Find a suitable way to ensure the axAttributeName is an IRI. (and not Identifier/Label/Raw etc.)
            if (vxAttributeName.startsWith("http") && rdfObject != null) {
                final String rdfIdentifierSubject = graph.getStringValue(vertexIdentifierAttributeId, vertexId);
                final IRI subject = FACTORY.createIRI(rdfIdentifierSubject);
                final IRI predicate = FACTORY.createIRI(vxAttributeName);
                if (rdfObject.contains(SEPARATOR_TERM)) {
                    // TODO: This is potentially unsafe because it is not guaranteed that the object will not
                    // contain a comma as part of it's value. Possible solution is to use a different SEPARATOR_TERM,
                    // or implement multi-valued attributes.
                    for (final String csvItem : rdfObject.split(SEPARATOR_TERM)) {
                        final Literal object = FACTORY.createLiteral(csvItem);
                        model.add(FACTORY.createStatement(subject, predicate, object));
                    }
                } else {
                    final Literal object = FACTORY.createLiteral(rdfObject);
                    if (VERBOSE) {
                        LOGGER.log(Level.INFO, "Found IRI Attribute: {0}, {1}, {2}", new Object[]{rdfIdentifierSubject, vxAttributeName, rdfObject});
                    }
                    model.add(FACTORY.createStatement(subject, predicate, object));
                }

            }
        }
    }

    public static void addVertexToModel(final GraphReadMethods graph, Model model, int vertexId) {
        // vertex attributes
        final int vertexIdentifierAttributeId = VisualConcept.VertexAttribute.IDENTIFIER.get(graph);
        final int vertexRDFTypesAttributeId = RDFConcept.VertexAttribute.RDFTYPES.get(graph);
        //final int vertexSourceAttributeId = AnalyticConcept.VertexAttribute.SOURCE.get(graph);

        final String identifier = graph.getStringValue(vertexIdentifierAttributeId, vertexId);
        //final String source = graph.getStringValue(vertexSourceAttributeId, vertexId);
        final String rdfTypes = graph.getStringValue(vertexRDFTypesAttributeId, vertexId);

        final Resource subject = FACTORY.createIRI(getIRI(identifier));

        addNodeAttributes(graph, model, vertexId);
        //Iterate over multiple values in RDF_TYPE and add multiple entries to the RDF collection
        if (rdfTypes != null) {
            final String[] rdfTypesArray = Arrays.stream(rdfTypes.split(","))
                    .filter(value -> value != null && value.length() > 0)
                    .toArray(size -> new String[size]);

            //if (rdfTypesArray.length > 0) {
            for (int i = 0; i < rdfTypesArray.length; i++) {
                final Value object = FACTORY.createIRI(getIRI(rdfTypesArray[i])); // TODO: this will require a lookup to convert a Consty type to RDF type

                model.add(FACTORY.createStatement(subject, RDF.TYPE, object));
            }
        }
    }

    public static void addTransactionToModel(final GraphReadMethods graph, Model model, int transactionId) {
        // transaction attributes
        final int transactionRDFIdentifierAttributeId = RDFConcept.TransactionAttribute.RDFIDENTIFIER.get(graph);
        //final int transactionTypeAttributeId = AnalyticConcept.TransactionAttribute.TYPE.get(graph);
        //final int transactionSourceAttributeId = AnalyticConcept.TransactionAttribute.SOURCE.get(graph);

        // vertex attributes
        final int vertexIdentifierAttributeId = VisualConcept.VertexAttribute.IDENTIFIER.get(graph);
        //final int vertexSourceAttributeId = AnalyticConcept.VertexAttribute.SOURCE.get(graph);

        final int sourceVertexId = graph.getTransactionSourceVertex(transactionId);
        final int destinationVertexId = graph.getTransactionDestinationVertex(transactionId);

        final String sourceIdentifier = graph.getStringValue(vertexIdentifierAttributeId, sourceVertexId);
        //final String sourceSource = graph.getStringValue(vertexSourceAttributeId, sourceVertexId);
        final String destinationIdentifier = graph.getStringValue(vertexIdentifierAttributeId, destinationVertexId);
        //final String destinationSource = graph.getStringValue(vertexSourceAttributeId, destinationVertexId);
        //final String transactionSource = graph.getStringValue(transactionSourceAttributeId, transactionId);
        final String transactionRDFIdentifier = graph.getStringValue(transactionRDFIdentifierAttributeId, transactionId);
        //final String transactionType = graph.getStringValue(transactionTypeAttributeId, transactionId);

        final Resource subject = FACTORY.createIRI(getIRI(sourceIdentifier));
        final IRI predicate = FACTORY.createIRI(getIRI(transactionRDFIdentifier));
        final Value object = FACTORY.createIRI(getIRI(destinationIdentifier));

        model.add(FACTORY.createStatement(subject, predicate, object));
    }

    public static void addBlankNodesToModel(final GraphReadMethods graph, Model model) {
        final int rdfBlankNodesAttributeId = RDFConcept.GraphAttribute.RDF_BLANK_NODES.get(graph);
        final Set<Statement> bNodeStatements = graph.getObjectValue(rdfBlankNodesAttributeId, 0);

        if (bNodeStatements != null) {
            for (final Statement statement : bNodeStatements) {
                model.add(statement);
            }
        }
    }

    /**
     * Takes all entries from the literalToValue map and writes the contents to
     * the correct nodes.
     *
     * This was necessary because it did not seem possible to view previous
     * entries while iterating each statement to check for multiple entries for
     * each triple?.
     *
     * @param wg
     * @param literalToValue
     */
    public static void setLiteralValuesVertexAttribute(final GraphWriteMethods wg, final MultiKeyMap literalToValue) {
        final int vertexIdentifierAttributeId = VisualConcept.VertexAttribute.IDENTIFIER.ensure(wg);
        final int graphVertexCount = wg.getVertexCount();

        for (int position = 0; position < graphVertexCount; position++) {
            final int currentVertexId = wg.getVertex(position);
            final String RDFidentifier = wg.getStringValue(vertexIdentifierAttributeId, currentVertexId);

            literalToValue.forEach((key, value) -> {
                final String key2 = (String) ((MultiKey) key).getKey(1);
                if (RDFidentifier.equals(key2)) {
                    final String key1 = (String) ((MultiKey) key).getKey(0);
                    // create new attribute that is of the name in key1
                    final int newAttribute = wg.addAttribute(GraphElementType.VERTEX, "string", key1, "Auto-Generated RDF Predicate IRI", null, null);
                    wg.setStringValue(newAttribute, currentVertexId, (String) value);
                }
            });
        }
    }
}
