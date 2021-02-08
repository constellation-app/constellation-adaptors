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
package au.gov.asd.tac.constellation.views.dataaccess.adaptors.sail;

import au.gov.asd.tac.constellation.graph.Graph;
import au.gov.asd.tac.constellation.graph.ReadableGraph;
import au.gov.asd.tac.constellation.graph.WritableGraph;
import au.gov.asd.tac.constellation.graph.manager.GraphManager;
import au.gov.asd.tac.constellation.graph.manager.GraphManagerListener;
import au.gov.asd.tac.constellation.graph.monitor.GraphChangeEvent;
import au.gov.asd.tac.constellation.graph.monitor.GraphChangeListener;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStoreUtilities;
import au.gov.asd.tac.constellation.views.dataaccess.adaptors.utilities.RDFUtilities;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.sail.SailChangedListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.eclipse.rdf4j.sail.helpers.AbstractNotifyingSail;
import org.openide.util.Exceptions;

/**
 *
 * @author scorpius77
 */
public class ConstellationSail extends AbstractNotifyingSail implements FederatedServiceResolverClient, GraphManagerListener, GraphChangeListener {

    private static final Logger LOGGER = Logger.getLogger(ConstellationSail.class.getName());

    private Graph graph;
    private SailStore store;
    private FederatedServiceResolver serviceResolver;
    private ConstellationSailConnection connection;

    public void addGraphManagerListener() {
        GraphManager.getDefault().addGraphManagerListener(ConstellationSail.this);
    }

    @Override
    protected void initializeInternal() throws SailException {
        super.initializeInternal();

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
        if (store != null) {
            store.close();
        }
    }

    @Override
    public boolean isWritable() throws SailException {
        // I assume this is always true?
        //return true;
        return connection.isOpen();
    }

    @Override
    public ValueFactory getValueFactory() {
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
        new Thread(() -> {
            // TODO: only process the graph if something has changed that is of use, selection is not useful but a data structure or property modification should require the graph to be processed again.
            final Graph graph = constellationEvent.getGraph();

            // update the model with changes to the graph
            final ReadableGraph readableGraph = graph.getReadableGraph();
            try {
                final Model graphModel = RDFUtilities.getGraphModel(readableGraph);
                graphModel.getStatements(null, null, null).forEach(statement -> {
                    store.getExplicitSailSource().sink(getDefaultIsolationLevel()).approve(statement);
                });
            } finally {
                readableGraph.release();
            }

            // TODO: handle node or link delete scenario
//        // RDF model to Consty Graph
//        GraphRecordStore recordStore = new GraphRecordStore();
//        RDFUtilities.processNextRecord(recordStore, statement, new HashMap<>(), 0);
//
//        WritableGraph writableGraph = null;
//        try {
//            writableGraph = graph.getWritableGraph("RDF Update", true);
//            GraphRecordStoreUtilities.addRecordStoreToGraph(writableGraph, recordStore, true, true, null);
//        } catch (InterruptedException ex) {
//            Exceptions.printStackTrace(ex);
//        } finally {
//            if (writableGraph != null) {
//                writableGraph.commit();
//            }
//        }
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
            this.graph = graph;
        }).start();
    }

    /**
     * Generate a graph of the in-memory RDF model and add it to the graph on
     * demand.
     */
    @Deprecated
    public void writeModelToGraph() {
        final GraphRecordStore recordStore = new GraphRecordStore();

        store.getExplicitSailSource().dataset(getDefaultIsolationLevel()).getStatements(null, null, null).stream().forEach((statement) -> {
            RDFUtilities.processNextRecord(recordStore, statement, new HashMap<>(), new HashSet<>(), 0);
        });

        WritableGraph writableGraph = null;
        try {
            writableGraph = graph.getWritableGraph("Apply RDF model to the graph", true);
            GraphRecordStoreUtilities.addRecordStoreToGraph(writableGraph, recordStore, true, true, null);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (writableGraph != null) {
                writableGraph.commit();
            }
        }
    }

    /**
     * Return the RDF in-memory model
     *
     * @return An unmodifiable copy of the in-memory model
     */
    public Model getModel() {
        Model model = new TreeModel();
        store.getExplicitSailSource().dataset(getDefaultIsolationLevel()).getStatements(null, null, null)
                .stream().forEach((statement) -> {
                    model.add(statement);
                });
        return model.unmodifiable();
    }

    @Override
    public void graphOpened(Graph graph) {
        // covered by the newActiveGraph
    }

    @Override
    public void graphClosed(Graph graph) {
        if (this.graph != null) {
            this.graph.removeGraphChangeListener(this);
        }
        this.graph = null;
    }

    @Override
    public void newActiveGraph(Graph graph) {
        if (graph != null) {
            this.graph = graph;
            this.graph.addGraphChangeListener(this);
        }
    }

    /**
     * Manually append to the in-memory RDF model.
     *
     * Note this is experimental and could ideally be removed. The model should
     * ideally be updated by using the (@link RepositoryConnection).
     *
     * @param model RDF Model containing triples to add to the in-memory graph.
     */
    /*
    @Deprecated
    public void appendToModel(final Model model) {
        for (final Statement statement : model.getStatements(null, null, null)) {
            this.model.add(statement);
        }
    }
     */
    public void printVerboseModel() {
        Model model = getModel();
        LOGGER.log(Level.INFO, "Model size is {0}", model.size());
        model.getStatements(null, null, null).forEach((statement) -> {
            LOGGER.log(Level.INFO, "\tStatement is {0}", statement);
        });
    }

}
