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

import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.importing.ImportFromRDFPlugin;
import au.gov.asd.tac.constellation.graph.GraphElementType;
import au.gov.asd.tac.constellation.graph.GraphReadMethods;
import au.gov.asd.tac.constellation.graph.GraphWriteMethods;
import au.gov.asd.tac.constellation.graph.LayersConcept;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStoreUtilities;
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.AnalyticConcept;
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

    final static boolean VERBOSE = true;
    final static SimpleValueFactory FACTORY = SimpleValueFactory.getInstance();
    private final static String SEPARATOR_TERM = SeparatorConstants.COMMA;

    final static Map<String, Resource> bnodeToSubject = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(ImportFromRDFPlugin.class.getName());
    private static final int layer_Mask = 3;

    public static void PopulateRecordStore(GraphRecordStore recordStore, RepositoryResult<Statement> repositoryResult, Map<String, String> subjectToType, MultiKeyMap literalToValue, int layerMask) {
        // TODO- need to remove this if the bNodeStatements are added into the graph attribute by other plugins
        PopulateRecordStore(recordStore, repositoryResult, subjectToType, literalToValue, new HashSet<>(), layerMask);
    }

    public static void PopulateRecordStore(GraphRecordStore recordStore, GraphQueryResult res, Map<String, String> subjectToType, MultiKeyMap literalToValue, int layerMask) {
        // TODO- need to remove this if the bNodeStatements are added into the graph attribute by other plugins
        PopulateRecordStore(recordStore, res, subjectToType, literalToValue, new HashSet<>(), layerMask);
    }

    public static void PopulateRecordStore(GraphRecordStore recordStore, RepositoryResult<Statement> repositoryResult, Map<String, String> subjectToType, MultiKeyMap literalToValue, Set<Statement> bNodeStatements, int layerMask) {
        for (Statement statement : repositoryResult) {
            processNextRecord(recordStore, statement, subjectToType, literalToValue, bNodeStatements, layerMask);
        }
    }

    public static void PopulateRecordStore(GraphRecordStore recordStore, GraphQueryResult res, Map<String, String> subjectToType, MultiKeyMap literalToValue, Set<Statement> bNodeStatements, int layerMask) {
        while (res.hasNext()) {
            processNextRecord(recordStore, res.next(), subjectToType, literalToValue, bNodeStatements, layerMask);
        }
    }

    public static void processNextRecord(GraphRecordStore recordStore, Statement statement, Map<String, String> subjectToType, MultiKeyMap literalToValue, Set<Statement> bNodeStatements, int layerMask) {
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
            if (addAttributes || ("type".equals(predicateName) && subject instanceof BNode)) { // literal object values are added as Vertex properties
                if (VERBOSE) {
                    LOGGER.log(Level.INFO, "Adding Literal \"{0}\"", objectName);
                }

                recordStore.add();
                if (subject instanceof IRI) {//Currently the subject can only be a URI or blank node- if they ever support Literals, need to change this
                    recordStore.set(GraphRecordStoreUtilities.SOURCE + RDFConcept.VertexAttribute.RDFIDENTIFIER, StringUtils.trim(subject.stringValue()).toLowerCase());
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
                recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, subjectName);
                recordStore.set(GraphRecordStoreUtilities.SOURCE + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));

            } else if ("type".equals(predicateName)) {//TODO need to handle TYPE of BNODES seperately here
                recordStore.add();
                if (subject instanceof IRI) {
                    recordStore.set(GraphRecordStoreUtilities.SOURCE + RDFConcept.VertexAttribute.RDFIDENTIFIER, StringUtils.trim(subject.stringValue()).toLowerCase());
                } else {//Subject is  a BNode
                    if (VERBOSE) {
                        LOGGER.log(Level.WARNING, "Subject is  a BNode. Added the triples above in an attribute of " + parentIRISubject.stringValue());
                    }//or skip overwriting-can it hit hrer?yes  TYPE of BNODES
                }
                recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, subjectName);
                recordStore.set(GraphRecordStoreUtilities.SOURCE + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));

                //If there are multiple types, add them CSV (E.g.: "ind:The_Beatles a music:Band, music:Artist ;")
                String value = object.stringValue(); //objectName;
                if (subjectToType.containsKey(subjectName) && !value.isBlank()) {
                    value = subjectToType.get(subjectName) + ", " + value;
                }
                subjectToType.put(subjectName, value);

                //TODO Map the RDF Type in objectName to Consty type
            } else if (objectIsIRI) { //subject.stringValue().startsWith("http") &&  predicate.stringValue().startsWith("http")) {
                {
                    recordStore.add();
                }
                if (!(subject instanceof IRI && predicate instanceof IRI)) {
                    if (VERBOSE) {
                        LOGGER.log(Level.WARNING, "Invalid RDF IDENTIFIER. Subject: " + subject.stringValue() + " or Predicate: " + predicate.stringValue());
                    }
                }
                recordStore.set(GraphRecordStoreUtilities.SOURCE + RDFConcept.VertexAttribute.RDFIDENTIFIER, StringUtils.trim(subject.stringValue()).toLowerCase());
                recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, subjectName);
                recordStore.set(GraphRecordStoreUtilities.SOURCE + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));

                if (StringUtils.isNotBlank(objectName)) {
                    recordStore.set(GraphRecordStoreUtilities.DESTINATION + RDFConcept.VertexAttribute.RDFIDENTIFIER, StringUtils.trim(object.stringValue()).toLowerCase());
                    recordStore.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, objectName);
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
        final int vertexRDFIdentifierAttributeId = RDFConcept.VertexAttribute.RDFIDENTIFIER.get(graph);

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
                final String rdfIdentifierSubject = graph.getStringValue(vertexRDFIdentifierAttributeId, vertexId);
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
        //final int vertexIdentifierAttributeId = VisualConcept.VertexAttribute.IDENTIFIER.get(graph);
        final int vertexRDFIdentifierAttributeId = RDFConcept.VertexAttribute.RDFIDENTIFIER.get(graph);
        final int vertexRDFTypesAttributeId = RDFConcept.VertexAttribute.RDFTYPES.get(graph);
        //final int vertexSourceAttributeId = AnalyticConcept.VertexAttribute.SOURCE.get(graph);

        //final String identifier = graph.getStringValue(vertexIdentifierAttributeId, vertexId);
        final String rdfIdentifier = graph.getStringValue(vertexRDFIdentifierAttributeId, vertexId);
        //final String source = graph.getStringValue(vertexSourceAttributeId, vertexId);
        final String rdfTypes = graph.getStringValue(vertexRDFTypesAttributeId, vertexId);

        final Resource subject = FACTORY.createIRI(getIRI(rdfIdentifier));

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
        final int vertexRDFIdentifierAttributeId = RDFConcept.VertexAttribute.RDFIDENTIFIER.get(graph);
        //final int vertexSourceAttributeId = AnalyticConcept.VertexAttribute.SOURCE.get(graph);

        final int sourceVertexId = graph.getTransactionSourceVertex(transactionId);
        final int destinationVertexId = graph.getTransactionDestinationVertex(transactionId);

        final String sourceRDFIdentifier = graph.getStringValue(vertexRDFIdentifierAttributeId, sourceVertexId);
        //final String sourceSource = graph.getStringValue(vertexSourceAttributeId, sourceVertexId);
        final String destinationRDFIdentifier = graph.getStringValue(vertexRDFIdentifierAttributeId, destinationVertexId);
        //final String destinationSource = graph.getStringValue(vertexSourceAttributeId, destinationVertexId);
        //final String transactionSource = graph.getStringValue(transactionSourceAttributeId, transactionId);
        final String transactionRDFIdentifier = graph.getStringValue(transactionRDFIdentifierAttributeId, transactionId);
        //final String transactionType = graph.getStringValue(transactionTypeAttributeId, transactionId);

        final Resource subject = FACTORY.createIRI(getIRI(sourceRDFIdentifier));
        final IRI predicate = FACTORY.createIRI(getIRI(transactionRDFIdentifier));
        final Value object = FACTORY.createIRI(getIRI(destinationRDFIdentifier));

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
        final int vertexIdentifierAttributeId = RDFConcept.VertexAttribute.RDFIDENTIFIER.ensure(wg);
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

    public static void setRDFTypesVertexAttribute(GraphWriteMethods wg, final Map<String, String> subjectToType) {
        // Add the Vertex RDF_types attribute based on subjectToType map
        // Had to do this later to avoid duplicate nodes with "Unknown" Type.
        final int vertexIdentifierAttributeId = VisualConcept.VertexAttribute.IDENTIFIER.ensure(wg);
        final int vertexRDFTypeAttributeId = RDFConcept.VertexAttribute.RDFTYPES.ensure(wg);
        final int graphVertexCount = wg.getVertexCount();
        for (int position = 0; position < graphVertexCount; position++) {
            final int currentVertexId = wg.getVertex(position);
            final String identifier = wg.getStringValue(vertexIdentifierAttributeId, currentVertexId);
            if (subjectToType.containsKey(identifier)) {
                String value = subjectToType.get(identifier);

                //Set RDF Types
                wg.setStringValue(vertexRDFTypeAttributeId, currentVertexId, value);

            }
        }
    }
}
