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

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailSourceConnection;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.eclipse.rdf4j.sail.helpers.AbstractSail;
import org.eclipse.rdf4j.sail.helpers.DefaultSailChangedEvent;

/**
 *
 * @author scorpius77
 */
public class ConstellationSailConnection extends SailSourceConnection {

    private SailStore store;
    //private volatile DefaultSailChangedEvent sailChangedEvent;

    /**
     * Used for federated queries (i.e. queries on remote databases).
     *
     * @param sail
     * @param store
     * @param resolver
     */
    public ConstellationSailConnection(AbstractSail sail, SailStore store, FederatedServiceResolver resolver) {
        super(sail, store, resolver);
        this.store = store;
        //this.sailChangedEvent = new DefaultSailChangedEvent(sail);
    }

    /**
     * Used for local queries, thus requiring information about how to optimise
     * said query's search performance.
     *
     * @param sail
     * @param store
     * @param evalStratFactory
     */
    public ConstellationSailConnection(AbstractSail sail, SailStore store, EvaluationStrategyFactory evalStratFactory) {
        super(sail, store, evalStratFactory);
        this.store = store;
        //this.sailChangedEvent = new DefaultSailChangedEvent(sail);
    }

    @Override
    protected void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
        if (contexts.length > 0) {
            for (Resource context : contexts) {
                store.getExplicitSailSource().sink(IsolationLevels.NONE).approve(subj, pred, obj, context);
            }
        } else {
            store.getExplicitSailSource().sink(IsolationLevels.NONE).approve(subj, pred, obj, null);
        }
    }

    @Override
    protected void removeStatementsInternal(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
        if (contexts.length > 0) {
            for (Resource context : contexts) {
                store.getExplicitSailSource().sink(IsolationLevels.NONE).deprecate(subj, pred, obj, context);
            }
        } else {
            store.getExplicitSailSource().sink(IsolationLevels.NONE).deprecate(subj, pred, obj, null);
        }
    }

}
