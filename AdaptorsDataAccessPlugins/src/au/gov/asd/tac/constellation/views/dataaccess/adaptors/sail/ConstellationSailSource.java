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
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailSink;
import org.eclipse.rdf4j.sail.base.SailSource;

/**
 * We should implement this if we want to fork the Contellation graph, for
 * instance because we are version controlling the graph or we want to support
 * concurrency?
 *
 * @author scorpius77
 */
public class ConstellationSailSource implements SailSource {

    final Model model;
    final SailSink sink;
    final SailDataset dataset;

    public ConstellationSailSource(final Model model) {
        // TODO: Having the model here is not necessary in the long-run. We instead want to read from Constellation directly?
        this.model = new TreeModel(model);
        this.sink = new ConstellationSailSink(IsolationLevels.NONE, this.model);
        this.dataset = new ConstellationSailDataset(IsolationLevels.NONE, this.model);
    }

    @Override
    public SailSource fork() {
        return new ConstellationSailSource(this.model);
    }

    @Override
    public SailSink sink(IsolationLevel level) throws SailException {
        // Do nothing. We are not currently handling concurrency / transactions.
        return sink;
    }

    @Override
    public SailDataset dataset(IsolationLevel level) throws SailException {
        // Do nothing. We are not currently handling concurrency / transactions.
        return dataset;
    }

    @Override
    public void prepare() throws SailException {
        sink.prepare();
    }

    @Override
    public void flush() throws SailException {
        sink.flush();
    }

    @Override
    public void close() throws SailException {
        sink.close();
    }

}
