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
package au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.example;

import au.gov.asd.tac.constellation.graph.processing.GraphRecordStoreUtilities;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;
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
 * Example Query for querying the Example Read Traffic Gaffer store
 *
 * @author GCHQDeveloper601
 */
public class GafferSimpleQuery {

    private GafferConnector connector;

    public GafferSimpleQuery() {
        //NOOP
    }

    public void setUrl(final String url) {
        connector = new GafferConnector(url);
    }

    protected void setGafferConnectorService(final GafferConnector gafferConnector) {
        connector = gafferConnector;
    }

    /**
     * This function will only return the details of the Elements being queries
     * without getting any hops
     *
     * @param queryIds The name value of {@link uk.gov.gchq.gaffer.data.element.Entity)
     * @param recordStore The record store to load the results into
     */
    public void queryForDetails(final List<String> queryIds, final RecordStore recordStore) {
        //This query is not quite right yet so is not yet enabled.
        final GetElements elms = new GetElements.Builder().input(queryIds).build();
        final OperationChain<CloseableIterable<? extends Element>> opChain = new OperationChain.Builder().first(elms).build();
        fetchResults(opChain, recordStore);
    }

    /**
     * This function will return 1 hop from the seed being queried
     *
     * @param queryIds The name value of {@link uk.gov.gchq.gaffer.data.element.Entity)
     * @param recordStore The record store to load the results into
     */
    public void queryForOneHop(final List<String> queryIds, final RecordStore recordStore) {
        final OperationChain opChain = buildOneHopChain(queryIds);
        fetchResults(opChain, recordStore);
    }

    /**
     * This function will return 2 hop from the seed being queried
     *
     * @param queryIds The name value of {@link uk.gov.gchq.gaffer.data.element.Entity)
     * @param recordStore The record store to load the results into
     */
    public void queryForTwoHop(final List<String> queryIds, final RecordStore recordStore) {
        final OperationChain opChain = buildTwoHopChain(queryIds);
        fetchResults(opChain, recordStore);
    }

    protected void fetchResults(final OperationChain opChain, final RecordStore recordStore) {
        try {
            final List<Element> results = connector.sendQueryToGaffer(opChain);
            results.forEach(result -> addResultsToRecordStore(result, recordStore));
        } catch (final IOException | InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public void addResultsToRecordStore(final Element element, final RecordStore recordStore) {
        recordStore.add();
        if (element instanceof Edge) {
            final Edge e = (Edge) element;
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, e.getSource());
            recordStore.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, e.getDestination());
            recordStore.set(GraphRecordStoreUtilities.TRANSACTION + VisualConcept.TransactionAttribute.DIRECTED, e.getDirectedType());
        } else if (element instanceof Entity) {
            final Entity e = (Entity) element;
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, e.getVertex());
            recordStore.set(GraphRecordStoreUtilities.SOURCE + "COUNT", e.getProperty("count"));
            e.getProperties().keySet().forEach(key -> recordStore.set(GraphRecordStoreUtilities.SOURCE + key.toUpperCase(), e.getProperties().get(key)));
        }
    }

    protected OperationChain buildOneHopChain(final List<String> queryIds) {
        final GetElements elms = new GetElements.Builder().input(queryIds).build();
        return new OperationChain.Builder().first(elms).build();
    }

    protected OperationChain buildTwoHopChain(final List<String> queryIds) {
        return new OperationChain.Builder()
                .first(new GetAdjacentIds.Builder().input(queryIds).build())
                .then(new GetElements.Builder().build())
                .build();
    }

}
