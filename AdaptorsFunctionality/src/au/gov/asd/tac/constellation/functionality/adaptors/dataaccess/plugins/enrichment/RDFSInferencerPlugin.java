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
package au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.enrichment;

import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.DataAccessPluginAdaptorType;
import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.sail.ConstellationSail;
import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.utilities.RDFUtilities;
import au.gov.asd.tac.constellation.graph.Graph;
import au.gov.asd.tac.constellation.graph.GraphWriteMethods;
import au.gov.asd.tac.constellation.graph.ReadableGraph;
import au.gov.asd.tac.constellation.graph.interaction.InteractiveGraphPluginRegistry;
import au.gov.asd.tac.constellation.graph.manager.GraphManager;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import au.gov.asd.tac.constellation.graph.schema.rdf.concept.RDFConcept;
import au.gov.asd.tac.constellation.graph.schema.visual.VisualSchemaPluginRegistry;
import au.gov.asd.tac.constellation.plugins.Plugin;
import au.gov.asd.tac.constellation.plugins.PluginException;
import au.gov.asd.tac.constellation.plugins.PluginExecution;
import au.gov.asd.tac.constellation.plugins.PluginExecutor;
import au.gov.asd.tac.constellation.plugins.PluginInfo;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.PluginType;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.collections.map.MultiKeyMap;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.inferencer.fc.DedupingInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.DirectTypeHierarchyInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({
    @ServiceProvider(service = DataAccessPlugin.class),
    @ServiceProvider(service = Plugin.class)})
@PluginInfo(pluginType = PluginType.IMPORT, tags = {"ENRICH"})
@NbBundle.Messages("RDFSInferencerPlugin=RDFS Inferencing")
public class RDFSInferencerPlugin extends RecordStoreQueryPlugin implements DataAccessPlugin {

    private static final int layer_Mask = 9;
    final MultiKeyMap literalToValue = MultiKeyMap.decorate(new LinkedMap());
    private final Set<Statement> bNodeStatements = new HashSet<>();

    private static final Logger LOGGER = Logger.getLogger(RDFSInferencerPlugin.class.getName());

    @Override
    protected RecordStore query(RecordStore query, PluginInteraction interaction, PluginParameters parameters) throws InterruptedException, PluginException {

        final GraphRecordStore inferredRecordStore = new GraphRecordStore();
        final Graph graph = GraphManager.getDefault().getActiveGraph();

        final ReadableGraph readableGraph = graph.getReadableGraph();
        try {
            final Model model = RDFUtilities.getGraphModel(readableGraph);

            LOGGER.info("Apply the RDFS inferencing...");

            final ConstellationSail sail = new ConstellationSail();
            sail.initialize();
            sail.newActiveGraph(graph);

            final Repository repo = new SailRepository(
                    new DedupingInferencer(
                            new DirectTypeHierarchyInferencer(
                                    new SchemaCachingRDFSInferencer(sail, true)
                            )
                    )
            );

            try (RepositoryConnection conn = repo.getConnection()) {
                conn.add(model);

                // retrieve all RDFS infered statements
                try (RepositoryResult<Statement> repositoryResult = conn.getStatements(null, null, null);) {
                    RDFUtilities.PopulateRecordStore(inferredRecordStore, repositoryResult, literalToValue, bNodeStatements, layer_Mask);
                }
            }
        } finally {
            readableGraph.release();
        }

        return inferredRecordStore;
    }

    @Override
    public String getType() {
        return DataAccessPluginAdaptorType.ENRICH;
    }

    @Override
    public int getPosition() {
        return 104;
    }

    @Override
    public String getDescription() {
        return "Apply RDFS Inferencing";
    }

    @Override
    protected void edit(GraphWriteMethods wg, PluginInteraction interaction,
            PluginParameters parameters) throws InterruptedException, PluginException {
        super.edit(wg, interaction, parameters);

        RDFUtilities.setLiteralValuesVertexAttribute(wg, literalToValue);

        // Overwrite BNODES in the graph attribute with inferred data
        final int rdfBlankNodesAttributeId = RDFConcept.GraphAttribute.RDF_BLANK_NODES.ensure(wg);
        wg.setObjectValue(rdfBlankNodesAttributeId, 0, bNodeStatements);

        PluginExecution.withPlugin(VisualSchemaPluginRegistry.COMPLETE_SCHEMA).executeNow(wg);
        PluginExecutor.startWith(InteractiveGraphPluginRegistry.RESET_VIEW).executeNow(wg);
    }
}
