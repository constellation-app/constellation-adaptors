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
package au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.sail;

import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.utilities.RDFUtilities;
import au.gov.asd.tac.constellation.graph.Graph;
import au.gov.asd.tac.constellation.graph.monitor.GraphChangeEvent;
import au.gov.asd.tac.constellation.graph.monitor.GraphChangeListener;
import au.gov.asd.tac.constellation.graph.processing.HookRecordStoreCallback;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailChangedEvent;
import org.eclipse.rdf4j.sail.SailChangedListener;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.eclipse.rdf4j.sail.helpers.AbstractNotifyingSail;
import org.eclipse.rdf4j.sail.helpers.AbstractSail;

/**
 *
 * @author scorpius77
 */
public class ConstellationSail extends AbstractNotifyingSail implements FederatedServiceResolverClient, GraphChangeListener, HookRecordStoreCallback {

    private static final Logger LOGGER = Logger.getLogger(ConstellationSail.class.getName());

    private SailStore store;
    private FederatedServiceResolver serviceResolver;
    private ConstellationSailConnection connection;
    private Graph graph;

    @Override
    protected void initializeInternal() throws SailException {
        super.initializeInternal();

        // Do more stuff
        store = new ConstellationSailStore();
    }

    @Override
    protected ConstellationSailConnection getConnectionInternal() throws SailException {
        connection = new ConstellationSailConnection(this, store, serviceResolver);
        return connection;
    }

    @Override
    protected void shutDownInternal() throws SailException {
        // Not sure what's required?
        this.store.close();
    }

    @Override
    public boolean isWritable() throws SailException {
        // I assume this is always true?
        //return true;
        return getConnectionInternal().isOpen();
    }

    @Override
    public ValueFactory getValueFactory() {
        //return VF;
        return store.getValueFactory();
    }

    @Override
    public void addSailChangedListener(SailChangedListener sl) {
        super.addSailChangedListener(sl);
    }

    @Override
    public void removeSailChangedListener(SailChangedListener sl) {
        super.removeSailChangedListener(sl);
    }

    @Override
    public void setFederatedServiceResolver(FederatedServiceResolver fsr) {
        this.serviceResolver = fsr;
    }

    /**
     * Constellation's graph change listener.
     *
     * @param event
     */
    @Override
    public void graphChanged(GraphChangeEvent constellationEvent) {
        graph = constellationEvent.getGraph();
//        connection.clearInferred();
//        if (!inferred) {
//            connection.addStatement(subj, pred, obj, contexts);
//        } else {
//            connection.addInferredStatement(subj, pred, obj, contexts);
//        }
//        if (!inferred) {
//            connection.addStatement(subj, pred, obj, contexts);
//        } else {
//            connection.addInferredStatement(subj, pred, obj, contexts);
//        }
    }

    /**
     * Constellation's hook record store callback.
     *
     * @param recordStore
     */
    @Override
    public void onAdd(RecordStore recordStore) {
        // This shouldn't happen!
        if (graph == null) {
            LOGGER.severe("Graph object is null!");
            return;
        }

        // Convert a Constellation RecordStore into updates to the RDF4J model
        try {
            Model graphModel = RDFUtilities.getGraphModel(graph.getWritableGraph(this.toString(), true));
            graphModel.forEach(statement -> {
                connection.addStatement(statement.getSubject(), statement.getPredicate(), statement.getObject(), statement.getContext());
            });
        } catch (InterruptedException ex) {
            LOGGER.logp(Level.SEVERE, this.getClass().getName(), "onAdd", "Interrupted importing Constellation changes into RDF4J!", ex);
        }
    }

}
