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
package au.gov.asd.tac.constellations.dataaccess.adaptors.providers.gaffer;

import au.gov.asd.tac.constellation.graph.processing.GraphRecordStoreUtilities;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;


//import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;
import java.io.IOException;
import java.util.List;
import org.openide.util.Exceptions;

import uk.gov.gchq.gaffer.commonutil.iterable.CloseableIterable;
import uk.gov.gchq.gaffer.data.element.Edge;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.data.element.Entity;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.operation.impl.get.GetAdjacentIds;
import uk.gov.gchq.gaffer.operation.impl.get.GetElements;

/**
 *
 * @author GCHQDeveloper601
 */
public class GafferSimpleQuery {

    final private static GafferConnector connector = new GafferConnector();

    /**
     * This function will only return the details of the Elements being queries
     * without getting any hops
     *
     * @param url The URL of the Gaffer host to query
     * @param queryIds The name value of {@link uk.gov.gchq.gaffer.data.element.Entity)
     * @param recordStore The record store to load the results into
     */
    public void queryForDetails(String url, List<String> queryIds, RecordStore recordStore) {
        //This query is not quite right yet so is not yet enabled.
        GetElements elms = new GetElements.Builder().input(queryIds).build();
        OperationChain<CloseableIterable<? extends Element>> opChain = new OperationChain.Builder().first(elms).build();
        fetchResults(url, opChain, recordStore);
    }

    /**
     * This function will return 1 hop from the seed being queried
     *
     * @param url The URL of the Gaffer host to query
     * @param queryIds The name value of {@link uk.gov.gchq.gaffer.data.element.Entity)
     * @param recordStore The record store to load the results into
     */
    public void queryForOneHop(String url, List<String> queryIds, RecordStore recordStore) {
        GetElements elms = new GetElements.Builder().input(queryIds).build();
        OperationChain<CloseableIterable<? extends Element>> opChain = new OperationChain.Builder().first(elms).build();
        fetchResults(url, opChain, recordStore);
    }

    /**
     * This function will return 1 hop from the seed being queried
     *
     * @param url The URL of the Gaffer host to query
     * @param queryIds The name value of {@link uk.gov.gchq.gaffer.data.element.Entity)
     * @param recordStore The record store to load the results into
     */
    public void queryForTwoHop(String url, List<String> queryIds, RecordStore recordStore) {
        OperationChain<CloseableIterable<? extends Element>> opChain = new OperationChain.Builder()
                .first(new GetAdjacentIds.Builder().input(queryIds).build())
                .then(new GetElements.Builder().build())
                .build();
        fetchResults(url, opChain, recordStore);
    }

    private void fetchResults(String url, OperationChain opChain, RecordStore recordStore) {
        try {
            List<Element> results = connector.sendQueryToGaffer(url, opChain);
            results.forEach(result -> addResultsToRecordStore(result, recordStore));
        } catch (IOException | InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void addResultsToRecordStore(Element element, RecordStore recordStore) {
        recordStore.add();
        boolean type = element instanceof Edge;
        if (element instanceof Edge) {
            Edge e = (Edge) element;
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, e.getSource());
            recordStore.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, e.getDestination());
            recordStore.set(GraphRecordStoreUtilities.TRANSACTION + VisualConcept.TransactionAttribute.DIRECTED, e.getDirectedType());
            
        } else if (element instanceof Entity) {
            Entity e = (Entity) element;
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, e.getVertex());
            recordStore.set(GraphRecordStoreUtilities.SOURCE + "COUNT", e.getProperty("count"));
            e.getProperties().keySet().forEach(key -> recordStore.set(GraphRecordStoreUtilities.SOURCE + key.toUpperCase(),  e.getProperties().get(key)));
        }
    }

}
