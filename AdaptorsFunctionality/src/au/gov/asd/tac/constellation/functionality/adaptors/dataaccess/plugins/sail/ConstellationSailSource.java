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
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailSink;
import org.eclipse.rdf4j.sail.base.SailSource;

/**
 *
 * @author scorpius77
 */
public class ConstellationSailSource implements SailSource {

    public ConstellationSailSource() {
    }

    @Override
    public SailSource fork() {
        return this;
    }

    @Override
    public SailSink sink(IsolationLevel il) throws SailException {
        return new ConstellationSailSink(il);
    }

    @Override
    public SailDataset dataset(IsolationLevel il) throws SailException {
        return new ConstellationSailDataset(il);
    }

    @Override
    public void prepare() throws SailException {
    }

    @Override
    public void flush() throws SailException {
    }

    @Override
    public void close() throws SailException {
    }
    
}
