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
import au.gov.asd.tac.constellation.graph.LayersConcept;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStoreUtilities;
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.AnalyticConcept;
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    final static Map<String, Resource> bnodeToSubject = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(ImportFromRDFPlugin.class.getName());
    private static final int layer_Mask = 3;

    public static void PopulateRecordStore(GraphRecordStore recordStore, RepositoryResult<Statement> repositoryResult, Map<String, String> subjectToType, int layerMask) {
        for (Statement statement : repositoryResult) {
            processNextRecord(recordStore, statement, subjectToType, layerMask);
        }
    }

    public static void PopulateRecordStore(GraphRecordStore recordStore, GraphQueryResult res, Map<String, String> subjectToType, int layerMask) {
        while (res.hasNext()) {
            processNextRecord(recordStore, res.next(), subjectToType, layerMask);
        }
    }

    public static void processNextRecord(GraphRecordStore recordStore, Statement statement, Map<String, String> subjectToType, int layerMask) {
        LOGGER.info("Processing next record...");
        final Resource subject = statement.getSubject();
        final IRI predicate = statement.getPredicate();
        final Value object = statement.getObject();
        final Resource context = statement.getContext();
        Resource parentIRISubject = statement.getPredicate();

        LOGGER.log(Level.INFO, "Saw Subject: {0}, Predicate: {1}, Object: {2}, Context: {3}", new Object[]{subject, predicate, object, context});

        boolean addAttributes = false;
        boolean objectIsIRI = false;
        boolean objectIsBNode = false;

        // PROCESS: Subject
        // ----------------
        String subjectName = null;
        if (subject instanceof Literal) { //Currently the subject can only be a URI or blank node
            subjectName = ((Literal) subject).getLabel();
        } else if (subject instanceof IRI) {
            subjectName = ((IRI) subject).getLocalName();
        } else if (subject instanceof BNode) {
            subjectName = ((BNode) subject).stringValue();

            if (bnodeToSubject.containsKey(subject.stringValue())) {
                //subjectName = ((IRI) bnodeToSubject.get(subject.stringValue())).getLocalName();
                parentIRISubject = bnodeToSubject.get(subject.stringValue());
                subjectName = ((IRI) parentIRISubject).getLocalName();
                addAttributes = true;
            } else {
                //TODO we'll skip these nodes for now, but need to handle/convert them somehow
                LOGGER.log(Level.WARNING, "BNode subject type: {0}, dropping ( because connected to another BNode):  ", subjectName);//won't hit
                //Add them in a graph attribute
            }
        } else {
            LOGGER.log(Level.WARNING, "Unknown subject type: {0}, dropping", subject);
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
        } else if (object instanceof BNode) {
            objectName = ((BNode) object).stringValue();
            objectIsBNode = true;
            LOGGER.log(Level.WARNING, "BNode object type: {0}, could be dropping, if no further Bnode with same string in the subject is found", objectName);
        } else {
            LOGGER.log(Level.WARNING, "Unknown object type: {0}, dropping", object);
        }

        LOGGER.log(Level.INFO, "Processing Subject: {0}, Predicate: {1}, Object: {2}, Context: {3}", new Object[]{subjectName, predicateName, objectName, context});
        if (objectIsBNode) {
            if (subject instanceof IRI) {//Currently the subject can only be a URI or blank node- if they ever support Literals, need to change this
                LOGGER.log(Level.WARNING, "RDF IDENTIFIER = ");
                bnodeToSubject.put(object.stringValue(), subject);
            } else if (subject instanceof BNode) {
                //TODO we'll skip these nodes for now, but need to handle/convert them somehow
                LOGGER.log(Level.WARNING, "RDF IDENTIFIER = ");
                bnodeToSubject.put(object.stringValue(), parentIRISubject);//Disjointed BNodes doesn't have a valid parentIRISubject
            } else {
                LOGGER.log(Level.WARNING, "Invalid RDF IDENTIFIER for subject", subject.stringValue());//won't hit
            }

        } else if (addAttributes || ("type".equals(predicateName) && subject instanceof IRI)) { // literal object values are added as Vertex properties
            LOGGER.log(Level.INFO, "Adding Literal \"{0}\"", objectName);
            recordStore.add();
            if (subject instanceof IRI) {//Currently the subject can only be a URI or blank node- if they ever support Literals, need to change this
                recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.RDFIDENTIFIER, subject.stringValue());
            } else if (subject instanceof BNode) {
                //recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.RDFIDENTIFIER, parentIRISubject.stringValue());//or skip overwriting
                LOGGER.log(Level.WARNING, "Subject is a BNode. Add the triples in an attribute of " + parentIRISubject.stringValue());
            } else {
                LOGGER.log(Level.WARNING, " Invalid RDF IDENTIFIER for subject", subject.stringValue());
            }
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, subjectName);
            recordStore.set(GraphRecordStoreUtilities.SOURCE + predicateName, statement); //need to handle multiple attributes with the same predicatename
            recordStore.set(GraphRecordStoreUtilities.SOURCE + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));
        } else if ("type".equals(predicateName)) {//TODO need to handle TYPE of BNODES seperately here
            recordStore.add();
            if (subject instanceof IRI) {
                recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.RDFIDENTIFIER, subject.stringValue());
            } else {//Subject is  a BNode
                LOGGER.log(Level.WARNING, "Subject is  a BNode. Added the triples above in an attribute of " + parentIRISubject.stringValue());//or skip overwriting-can it hit hrer?yes  TYPE of BNODES
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
                LOGGER.log(Level.WARNING, "Invalid RDF IDENTIFIER. Subject: " + subject.stringValue() + " or Predicate: " + predicate.stringValue());
            }
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.RDFIDENTIFIER, subject.stringValue());
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, subjectName);
            recordStore.set(GraphRecordStoreUtilities.SOURCE + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));

            if (StringUtils.isNotBlank(objectName)) {
                recordStore.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.RDFIDENTIFIER, object.stringValue());
                recordStore.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, objectName);
                recordStore.set(GraphRecordStoreUtilities.DESTINATION + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));

                recordStore.set(GraphRecordStoreUtilities.TRANSACTION + VisualConcept.TransactionAttribute.RDFIDENTIFIER, predicate.stringValue());
                recordStore.set(GraphRecordStoreUtilities.TRANSACTION + VisualConcept.TransactionAttribute.IDENTIFIER, predicateName);
                recordStore.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.CORRELATION);//TODO FIX TYPE
                recordStore.set(GraphRecordStoreUtilities.TRANSACTION + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));
            }
        } else {
            LOGGER.log(Level.WARNING, "Predicate: {0} not mapped.", predicateName);
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

        return model;
    }

    public static String getIRI(final String rdfIdentifier) {
        if (rdfIdentifier.isBlank()) {
            LOGGER.log(Level.WARNING, "Null rdfIdentifier = BNodes");
            return null;
        } else if (rdfIdentifier.trim().startsWith("http")) {
            return rdfIdentifier;
        } else {
            return "http://consty.local#" + "/" + rdfIdentifier; // TODO: check if the RDF type is defined, if so then use it, otherwise is a http://consty.local#
        }
    }

    public static void addNodeAttributes(final GraphReadMethods graph, Model model, int vertexId) {
        //Loops through all of the Node attributes and add the tripples stored there
        final int vertexAttributeCount = graph.getAttributeCount(GraphElementType.VERTEX);
        for (int vertexAttributePosition = 0; vertexAttributePosition < vertexAttributeCount; vertexAttributePosition++) {
            final int vertexAttributeId = graph.getAttribute(GraphElementType.VERTEX, vertexAttributePosition);
            Statement s = graph.getObjectValue(vertexAttributeId, vertexId);
            model.add(s);
        }
    }

    public static void addVertexToModel(final GraphReadMethods graph, Model model, int vertexId) {
        // vertex attributes
        //final int vertexIdentifierAttributeId = VisualConcept.VertexAttribute.IDENTIFIER.get(graph);
        final int vertexRDFIdentifierAttributeId = VisualConcept.VertexAttribute.RDFIDENTIFIER.get(graph);
        final int vertexRDFTypesAttributeId = AnalyticConcept.VertexAttribute.RDFTYPES.get(graph);
        //final int vertexSourceAttributeId = AnalyticConcept.VertexAttribute.SOURCE.get(graph);

        //final String identifier = graph.getStringValue(vertexIdentifierAttributeId, vertexId);
        final String rdfIdentifier = graph.getStringValue(vertexRDFIdentifierAttributeId, vertexId);
        //final String source = graph.getStringValue(vertexSourceAttributeId, vertexId);
        final String rdfTypes = graph.getStringValue(vertexRDFTypesAttributeId, vertexId);

        final Resource subject = SimpleValueFactory.getInstance().createIRI(getIRI(rdfIdentifier));

        //Iterate over multiple values in RDF_TYPE and add multiple entries to the RDF collection
        if (rdfTypes != null) {
            final String[] rdfTypesArray = Arrays.stream(rdfTypes.split(","))
                    .filter(value
                            -> value != null && value.length() > 0
                    )
                    .toArray(size -> new String[size]);

            //if (rdfTypesArray.length > 0) {
            for (int i = 0; i < rdfTypesArray.length; i++) {
                final Value object = SimpleValueFactory.getInstance().createIRI(getIRI(rdfTypesArray[i])); // TODO: this will require a lookup to convert a Consty type to RDF type

                model.add(SimpleValueFactory.getInstance().createStatement(subject, RDF.TYPE, object));
            }
        }
    }

    public static void addTransactionToModel(final GraphReadMethods graph, Model model, int transactionId) {
        // transaction attributes
        final int transactionRDFIdentifierAttributeId = VisualConcept.TransactionAttribute.RDFIDENTIFIER.get(graph);
        //final int transactionTypeAttributeId = AnalyticConcept.TransactionAttribute.TYPE.get(graph);
        //final int transactionSourceAttributeId = AnalyticConcept.TransactionAttribute.SOURCE.get(graph);

        // vertex attributes
        final int vertexRDFIdentifierAttributeId = VisualConcept.VertexAttribute.RDFIDENTIFIER.get(graph);
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

        final Resource subject = SimpleValueFactory.getInstance().createIRI(getIRI(sourceRDFIdentifier));
        final IRI predicate = SimpleValueFactory.getInstance().createIRI(getIRI(transactionRDFIdentifier));
        final Value object = SimpleValueFactory.getInstance().createIRI(getIRI(destinationRDFIdentifier));

        model.add(SimpleValueFactory.getInstance().createStatement(subject, predicate, object));
    }

//    public static void setGraphModel(Model model) {
//        graphModel = model;
//    }
}
