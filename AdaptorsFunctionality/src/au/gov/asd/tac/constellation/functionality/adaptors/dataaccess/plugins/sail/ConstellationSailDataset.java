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

import java.util.Iterator;
import java.util.logging.Logger;
import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailDataset;

/**
 *
 * @author scorpius77
 */
public class ConstellationSailDataset implements SailDataset {

    private final Model model;

    private static final Logger LOGGER = Logger.getLogger(ConstellationSailDataset.class.getName());

    public ConstellationSailDataset(IsolationLevel level, final Model model) {
        this.model = model;
    }

    @Override
    public void close() throws SailException {
    }

    @Override
    public CloseableIteration<? extends Namespace, SailException> getNamespaces() throws SailException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getNamespace(String prefix) throws SailException {
        return "";
    }

    @Override
    public CloseableIteration<? extends Resource, SailException> getContextIDs() throws SailException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public CloseableIteration<? extends Statement, SailException> getStatements(Resource rsrc, IRI iri, Value value, Resource... rsrcs) throws SailException {
        // get the actual statements
        final Iterator<Statement> iterator = model.getStatements(rsrc, iri, value, rsrcs).iterator();

        return new CloseableIteration<Statement, SailException>() {
            @Override
            public void close() throws SailException {
            }

            @Override
            public boolean hasNext() throws SailException {
                try {
                    return iterator.hasNext();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    // TODO: fix this concurrent mod exception
                    return false;
                }
            }

            @Override
            public Statement next() throws SailException {
                return iterator.next();
            }

            @Override
            public void remove() throws SailException {
                iterator.next();
            }
        };
    }

}
