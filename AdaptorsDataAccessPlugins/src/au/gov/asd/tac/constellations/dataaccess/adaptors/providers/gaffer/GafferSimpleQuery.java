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

    private String url;
    private GafferConnector connector ;

    public GafferSimpleQuery (){
    //NOOP
    }
    
    public void setUrl(String url){
        this.url = url;
        connector=new GafferConnector(url);
    }
    
    protected void setGafferConnectorService(GafferConnector gafferConnector){
        connector=gafferConnector;
    }
    
    /**
     * This function will only return the details of the Elements being queries
     * without getting any hops
     *
     * @param queryIds The name value of {@link uk.gov.gchq.gaffer.data.element.Entity)
     * @param recordStore The record store to load the results into
     */
    public void queryForDetails(List<String> queryIds, RecordStore recordStore) {
        //This query is not quite right yet so is not yet enabled.
        GetElements elms = new GetElements.Builder().input(queryIds).build();
        OperationChain<CloseableIterable<? extends Element>> opChain = new OperationChain.Builder().first(elms).build();
        fetchResults(opChain, recordStore);
    }

    /**
     * This function will return 1 hop from the seed being queried
     *
     * @param queryIds The name value of {@link uk.gov.gchq.gaffer.data.element.Entity)
     * @param recordStore The record store to load the results into
     */
    public void queryForOneHop(List<String> queryIds, RecordStore recordStore) { 
        var opChain =buildOneHopChain(queryIds);
        fetchResults(opChain, recordStore);
    }

    /**
     * This function will return 2 hop from the seed being queried
     *
     * @param queryIds The name value of {@link uk.gov.gchq.gaffer.data.element.Entity)
     * @param recordStore The record store to load the results into
     */
    public void queryForTwoHop(List<String> queryIds, RecordStore recordStore) {
        var opChain = buildTwoHopChain(queryIds);
        fetchResults(opChain, recordStore);
    }

    protected void fetchResults(OperationChain opChain, RecordStore recordStore) {
        try {
            List<Element> results = connector.sendQueryToGaffer(opChain);
            results.forEach(result -> addResultsToRecordStore(result, recordStore));
        } catch (IOException | InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public void addResultsToRecordStore(Element element, RecordStore recordStore) {
        recordStore.add();
        if (element instanceof Edge) {
            Edge e = (Edge) element;
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, e.getSource());
            recordStore.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, e.getDestination());
            recordStore.set(GraphRecordStoreUtilities.TRANSACTION + VisualConcept.TransactionAttribute.DIRECTED, e.getDirectedType());
        } else if (element instanceof Entity) {
            Entity e = (Entity) element;
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, e.getVertex());
            recordStore.set(GraphRecordStoreUtilities.SOURCE + "COUNT", e.getProperty("count"));
            e.getProperties().keySet().forEach(key -> recordStore.set(GraphRecordStoreUtilities.SOURCE + key.toUpperCase(), e.getProperties().get(key)));
        }
    }

    protected OperationChain buildOneHopChain(List<String> queryIds) {
        GetElements elms = new GetElements.Builder().input(queryIds).build();
        return new OperationChain.Builder().first(elms).build();
    }
    
    protected OperationChain buildTwoHopChain(List<String> queryIds) {
            return new OperationChain.Builder()
                .first(new GetAdjacentIds.Builder().input(queryIds).build())
                .then(new GetElements.Builder().build())
                .build();
       }

}
