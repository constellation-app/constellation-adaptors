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

import org.eclipse.rdf4j.IsolationLevel;
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
        this.model = model;
    }

    @Override
    public void prepare() throws SailException {
    }

    @Override
    public void flush() throws SailException {
    }

    @Override
    public void setNamespace(String prefix, String name) throws SailException {
    }

    @Override
    public void removeNamespace(String prefix) throws SailException {
    }

    @Override
    public void clearNamespaces() throws SailException {
    }

    @Override
    public void clear(Resource... contexts) throws SailException {
    }

    @Override
    public void observe(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
    }

    @Override
    public void approve(Resource subj, IRI pred, Value obj, Resource ctx) throws SailException {
        System.out.println("Adding: " + subj + " " + pred + " " + obj + " " + ctx);
        model.add(subj, pred, obj, ctx);
    }

    @Override
    public void deprecate(Statement statement) throws SailException {
        // Remove the data from Constellation!
        System.out.println("Removing: " + statement);
        model.remove(statement.getSubject(), statement.getPredicate(), statement.getObject());
        // TODO: need to notify the graph that records need to be removed
    }

    @Override
    public void close() throws SailException {
    }

}
