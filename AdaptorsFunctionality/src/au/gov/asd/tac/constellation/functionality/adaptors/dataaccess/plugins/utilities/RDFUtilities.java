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
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.GraphQueryResult;

public class RDFUtilities {

    final static Map<String, String> bnodeToSubject = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(ImportFromRDFPlugin.class.getName());
    private static final int layer_Mask = 3;

    public static void PopulateRecordStore(GraphRecordStore results, GraphQueryResult res, Map<String, String> subjectToType, int layerMask) {
        //final int layerMaskAttributeId = LayersConcept.VertexAttribute.LAYER_MASK.ensure(wg);
        while (res.hasNext()) {
            LOGGER.info("Processing next record...");

            final Statement st = res.next();
            final Resource subject = st.getSubject();
            final IRI predicate = st.getPredicate();
            final Value object = st.getObject();
            final Resource context = st.getContext();

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
                results.add();
                results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.RDFIDENTIFIER, subject.stringValue());
                results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, subjectName);
                results.set(GraphRecordStoreUtilities.SOURCE + predicateName, objectName);
                results.set(GraphRecordStoreUtilities.SOURCE + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));
            } else if ("type".equals(predicateName)) {
                results.add();
                results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.RDFIDENTIFIER, subject.stringValue());
                results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, subjectName);
                results.set(GraphRecordStoreUtilities.SOURCE + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));

                //If there are multiple types, add them CSV (E.g.: "ind:The_Beatles a music:Band, music:Artist ;")
                String value = objectName;
                if (subjectToType.containsKey(subjectName) && !value.isBlank()) {
                    value = subjectToType.get(subjectName) + ", " + value;
                }
                subjectToType.put(subjectName, value);

            } else if (objectIsIRI) {
                results.add();

                String s = subject.stringValue();
                String s2 = subject.toString();

                results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.RDFIDENTIFIER, subject.stringValue());
                results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, subjectName);
                results.set(GraphRecordStoreUtilities.SOURCE + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));

                if (StringUtils.isNotBlank(objectName)) {
                    results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.RDFIDENTIFIER, object.stringValue());
                    results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, objectName);
                    results.set(GraphRecordStoreUtilities.DESTINATION + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));

                    results.set(GraphRecordStoreUtilities.TRANSACTION + VisualConcept.TransactionAttribute.RDFIDENTIFIER, predicate.stringValue());
                    results.set(GraphRecordStoreUtilities.TRANSACTION + VisualConcept.TransactionAttribute.IDENTIFIER, predicateName);
                    results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.CORRELATION);
                    results.set(GraphRecordStoreUtilities.TRANSACTION + LayersConcept.VertexAttribute.LAYER_MASK, Integer.toString(layerMask));
                }
            } else {
                LOGGER.log(Level.WARNING, "Predicate: {0} not mapped.", predicateName);
            }
//                    }
        }
    }

}
