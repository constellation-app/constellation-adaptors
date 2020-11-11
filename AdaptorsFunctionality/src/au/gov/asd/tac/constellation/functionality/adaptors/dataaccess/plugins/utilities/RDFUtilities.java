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
import au.gov.asd.tac.constellation.graph.GraphReadMethods;
import au.gov.asd.tac.constellation.graph.LayersConcept;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStoreUtilities;
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.AnalyticConcept;
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;
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

    final static Map<String, String> bnodeToSubject = new HashMap<>();
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

        LOGGER.log(Level.INFO, "Saw Subject: {0}, Predicate: {1}, Object: {2}, Context: {3}", new Object[]{subject, predicate, object, context});

        boolean addAttributes = false;
        boolean objectIsIRI = false;
        boolean objectIsBNode = false;

        // PROCESS: Subject
        // ----------------
        String subjectName = null;
        if (subject instanceof Literal) {
            subjectName = ((Literal) subject).getLabel();
        } else if (subject instanceof IRI) {
            subjectName = ((IRI) subject).getLocalName();
        } else if (subject instanceof BNode) {
            subjectName = ((BNode) subject).stringValue();
            subjectName = bnodeToSubject.get(subjectName);
            addAttributes = true;
            LOGGER.log(Level.WARNING, "BNode subject type: {0}, dropping", subjectName);
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
            LOGGER.log(Level.WARNING, "BNode object type: {0}, dropping", objectName);
        } else {
            LOGGER.log(Level.WARNING, "Unknown object type: {0}, dropping", object);
        }

        LOGGER.log(Level.INFO, "Processing Subject: {0}, Predicate: {1}, Object: {2}, Context: {3}", new Object[]{subjectName, predicateName, objectName, context});

        if (objectIsBNode) {
            bnodeToSubject.put(objectName, subjectName);
        } else if (addAttributes) { // literal object values are added as Vertex properties
            LOGGER.log(Level.INFO, "Adding Literal \"{0}\"", objectName);
            recordStore.add();
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.RDFIDENTIFIER, subject.stringValue());
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, subjectName);
            recordStore.set(GraphRecordStoreUtilities.SOURCE + predicateName, objectName);
            recordStore.set(GraphRecordStoreUtilities.SOURCE + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));
        } else if ("type".equals(predicateName)) {
            recordStore.add();
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.RDFIDENTIFIER, subject.stringValue());
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, subjectName);
            recordStore.set(GraphRecordStoreUtilities.SOURCE + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));

            //If there are multiple types, add them CSV (E.g.: "ind:The_Beatles a music:Band, music:Artist ;")
            String value = objectName;
            if (subjectToType.containsKey(subjectName) && !value.isBlank()) {
                value = subjectToType.get(subjectName) + ", " + value;
            }
            subjectToType.put(subjectName, value);

        } else if (objectIsIRI) {
            recordStore.add();
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.RDFIDENTIFIER, subject.stringValue());
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, subjectName);
            recordStore.set(GraphRecordStoreUtilities.SOURCE + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));

            if (StringUtils.isNotBlank(objectName)) {
                recordStore.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.RDFIDENTIFIER, object.stringValue());
                recordStore.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, objectName);
                recordStore.set(GraphRecordStoreUtilities.DESTINATION + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));

                recordStore.set(GraphRecordStoreUtilities.TRANSACTION + VisualConcept.TransactionAttribute.RDFIDENTIFIER, predicate.stringValue());
                recordStore.set(GraphRecordStoreUtilities.TRANSACTION + VisualConcept.TransactionAttribute.IDENTIFIER, predicateName);
                recordStore.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.CORRELATION);
                recordStore.set(GraphRecordStoreUtilities.TRANSACTION + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));
            }
        } else {
            LOGGER.log(Level.WARNING, "Predicate: {0} not mapped.", predicateName);
        }
    }

    public static Model getGraphModel(final GraphReadMethods graph) {
        final Model model = new LinkedHashModel();

        // vertex attributes
        final int vertexIdentifierAttributeId = VisualConcept.VertexAttribute.IDENTIFIER.get(graph);
        final int vertexTypeAttributeId = AnalyticConcept.VertexAttribute.TYPE.get(graph);
        final int vertexSourceAttributeId = AnalyticConcept.VertexAttribute.SOURCE.get(graph);

        // transaction attributes
        final int transactionIdentifierAttributeId = VisualConcept.TransactionAttribute.IDENTIFIER.get(graph);
        final int transactionTypeAttributeId = AnalyticConcept.TransactionAttribute.TYPE.get(graph);
        final int transactionSourceAttributeId = AnalyticConcept.TransactionAttribute.SOURCE.get(graph);

        // statement: vertex X is a thing Y
        final int vxCount = graph.getVertexCount();
        for (int i = 0; i < vxCount; i++) {
            final int vertexId = graph.getVertex(i);
            final String identifier = graph.getStringValue(vertexIdentifierAttributeId, vertexId);
            final String source = graph.getStringValue(vertexSourceAttributeId, vertexId);
            final String type = graph.getStringValue(vertexTypeAttributeId, vertexId);

            final Resource subject = SimpleValueFactory.getInstance().createIRI("http://" + source + "/" + identifier); // TODO: check if the RDF type is defined, if so then use it, otherwise is a http://consty.local#
            final Value object = SimpleValueFactory.getInstance().createIRI("http://" + source + "/" + type); // TODO: this will require a lookup to convert a Consty type to RDF type

            model.add(SimpleValueFactory.getInstance().createStatement(subject, RDF.TYPE, object));
        }

        // statement: source vertex -> transaction type -> destination vertex
        final int txCount = graph.getTransactionCount();
        for (int i = 0; i < txCount; i++) {
            final int transactionId = graph.getTransaction(i);
            final int sourceVertexId = graph.getTransactionSourceVertex(transactionId);
            final int destinationVertexId = graph.getTransactionDestinationVertex(transactionId);

            final String sourceIdentifier = graph.getStringValue(vertexIdentifierAttributeId, sourceVertexId);
            final String sourceSource = graph.getStringValue(vertexSourceAttributeId, sourceVertexId);
            final String destinationIdentifier = graph.getStringValue(vertexIdentifierAttributeId, destinationVertexId);
            final String destinationSource = graph.getStringValue(vertexSourceAttributeId, destinationVertexId);
            final String transactionSource = graph.getStringValue(transactionSourceAttributeId, transactionId);
            final String transactionType = graph.getStringValue(transactionTypeAttributeId, transactionId);

            final Resource subject = SimpleValueFactory.getInstance().createIRI("http://" + sourceSource + "/" + sourceIdentifier); // TODO: source node needs to be mapped to a proper RDF subject
            final IRI predicate = SimpleValueFactory.getInstance().createIRI("http://" + transactionSource + "/" +transactionType);
            final Value object = SimpleValueFactory.getInstance().createIRI("http://" + destinationSource + "/" + destinationIdentifier);

            model.add(SimpleValueFactory.getInstance().createStatement(subject, predicate, object));
        }

        return model;
    }

//    public static void setGraphModel(Model model) {
//        graphModel = model;
//    }
}
