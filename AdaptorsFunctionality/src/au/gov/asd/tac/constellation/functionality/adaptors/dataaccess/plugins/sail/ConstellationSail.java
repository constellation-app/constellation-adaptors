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

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
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
public class ConstellationSail extends AbstractNotifyingSail implements FederatedServiceResolverClient {

    private SailStore store;

    private FederatedServiceResolver serviceResolver;

    @Override
    protected void initializeInternal() throws SailException {
        super.initializeInternal();

        // Do more stuff
        this.store = new ConstellationSailStore();
    }

    @Override
    protected ConstellationSailConnection getConnectionInternal() throws SailException {
        return new ConstellationSailConnection(this, store, serviceResolver);
    }
    
    @Override
    protected void shutDownInternal() throws SailException {
        // Not sure what's required?
    }

    @Override
    public boolean isWritable() throws SailException {
        // I assume this is always true?
        return true;
    }

    @Override
    public ValueFactory getValueFactory() {
        //return VF;
        return store.getValueFactory();
    }

    @Override
    public void addSailChangedListener(SailChangedListener sl) {
    }

    @Override
    public void removeSailChangedListener(SailChangedListener sl) {
    }

    @Override
    public void setFederatedServiceResolver(FederatedServiceResolver fsr) {
        this.serviceResolver = fsr;
    }

}
