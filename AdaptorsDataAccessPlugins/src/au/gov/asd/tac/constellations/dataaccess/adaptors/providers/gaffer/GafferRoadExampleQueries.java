/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.gov.asd.tac.constellations.dataaccess.adaptors.providers.gaffer;

import au.gov.asd.tac.constellation.graph.processing.GraphRecordStoreUtilities;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
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
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;

/**
 *
 * @author GCHQDeveloper601
 */
public class GafferRoadExampleQueries {
    
    private OperationChain getAdjacent(List<String> queryIds){
                GetAdjacentIds adj = new GetAdjacentIds.Builder().input(queryIds).build();
        GetElements elms = new GetElements.Builder().build();
        OperationChain<CloseableIterable<? extends Element>> opChains = new OperationChain.Builder().first(adj).then(elms).build();
        return opChains;
    }
    
    public void queryGafferForAdjacent(String URL,List<String> queryIds, RecordStore recordStore){
        GafferConnector connector = new GafferConnector();
        try {
            List<Element> results = connector.sendQueryToGaffer(URL, getAdjacent(queryIds));
            results.forEach(result->addResultsToRecordStore(result, recordStore));
        } catch (IOException | InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }
        
    }

    private void addResultsToRecordStore(Element element, RecordStore recordStore) {
        recordStore.add();
        if(element instanceof Edge){
            Edge e =(Edge) element;
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, e.getSource());
            recordStore.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, e.getDestination());
            recordStore.set(GraphRecordStoreUtilities.TRANSACTION + VisualConcept.TransactionAttribute.DIRECTED , e.getDirectedType());
            
        }else  if(element instanceof Entity){
            Entity e =(Entity) element;
            recordStore.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, e.getVertex());
            recordStore.set(GraphRecordStoreUtilities.SOURCE + "COUNT", e.getProperty("count"));
        }
    }
    
}
