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
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.base.SailStore;

/**
 *
 * @author scorpius77
 */
public class ConstellationSailStore implements SailStore {

    private static final ValueFactory VF = SimpleValueFactory.getInstance();

    private final ConstellationSailSource explictSource;
    private final ConstellationSailSource implicitSource;

    public ConstellationSailStore() {
        this.explictSource = new ConstellationSailSource(new TreeModel());
        this.implicitSource = new ConstellationSailSource(new TreeModel());
    }

    @Override
    public ValueFactory getValueFactory() {
        return VF;
    }

    /**
     * TODO as required for performance.
     *
     * @return
     */
    @Override
    public EvaluationStatistics getEvaluationStatistics() {
        return new EvaluationStatistics();
    }

    /**
     * The actual database of triples.
     *
     * @return
     */
    @Override
    public SailSource getExplicitSailSource() {
        return explictSource;
    }

    /**
     * The inferred database triples, derived from the data-set by RDFS or OWL.
     *
     * @return
     */
    @Override
    public SailSource getInferredSailSource() {
        return implicitSource;
    }

    @Override
    public void close() throws SailException {
    }

}
