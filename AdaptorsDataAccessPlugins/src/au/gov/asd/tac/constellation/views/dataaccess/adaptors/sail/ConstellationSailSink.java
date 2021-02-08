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

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailSink;

/**
 *
 * @author scorpius77
 */
public class ConstellationSailSink implements SailSink {

    private final Model model;

    public ConstellationSailSink(IsolationLevel level, final Model model) {
        assert level == IsolationLevels.NONE;
        this.model = model;
    }

    /**
     * Checks if this SailSink is consistent with the isolation level it was
     * created with.
     *
     * @throws SailException
     */
    @Override
    public void prepare() throws SailException {
        // Do nothing. We are not currently handling concurrency / transactions.
    }

    /**
     * Once this method returns successfully, changes that were made to this
     * SailSink will be visible to subsequent
     * SailSource.dataset(org.eclipse.rdf4j.IsolationLevel).
     *
     * @throws SailException
     */
    @Override
    public void flush() throws SailException {
        // Do nothing. We are not currently handling concurrency / transactions.
    }

    /**
     * Sets the prefix for a namespace.
     *
     * @param prefix
     * @param name
     * @throws SailException
     */
    @Override
    public void setNamespace(String prefix, String name) throws SailException {
        model.setNamespace(prefix, name);
    }

    /**
     * Removes a namespace declaration by removing the association between a
     * prefix and a namespace name.
     *
     * @param prefix
     * @throws SailException
     */
    @Override
    public void removeNamespace(String prefix) throws SailException {
        model.removeNamespace(prefix);
    }

    /**
     * Removes all namespace declarations from this SailSource.
     *
     * @throws SailException
     */
    @Override
    public void clearNamespaces() throws SailException {
        model.getNamespaces().stream().forEach((namespace) -> {
            model.removeNamespace(namespace.getPrefix());
        });
    }

    /**
     * Removes all statements from the specified/all contexts.
     *
     * @param contexts
     * @throws SailException
     */
    @Override
    public void clear(Resource... contexts) throws SailException {
        model.clear(contexts);
    }

    /**
     * Called to indicate matching statements have been observed and must not
     * change their state until after this SailSink is committed, if this was
     * opened in an isolation level compatible with
     * IsolationLevels.SERIALIZABLE.
     *
     * @param subj
     * @param pred
     * @param obj
     * @param contexts
     * @throws SailException
     */
    @Override
    public void observe(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
        // Do nothing. We are not currently handling concurrency / transactions.
    }

    /**
     * Adds a statement to the store.
     *
     * @param subj
     * @param pred
     * @param obj
     * @param ctx
     * @throws SailException
     */
    @Override
    public void approve(Resource subj, IRI pred, Value obj, Resource ctx) throws SailException {
        System.out.println("Adding: " + subj + " " + pred + " " + obj + " " + ctx);
        model.add(subj, pred, obj, ctx);
        //LOGGER.log(Level.INFO, "Model size is {0}", model.size());
    }

    /**
     * Removes a statement.
     *
     * @param statement
     * @throws SailException
     */
    @Override
    public void deprecate(Statement statement) throws SailException {
        // Remove the data from Constellation!
        System.out.println("Removing: " + statement);
        model.remove(statement.getSubject(), statement.getPredicate(), statement.getObject(), statement.getContext());
        //LOGGER.log(Level.INFO, "Model size is {0}", model.size());
        // TODO: need to notify the graph that records need to be removed
    }

    /**
     * Close.
     * @throws SailException 
     */
    @Override
    public void close() throws SailException {
        // Nothing required
    }

}
