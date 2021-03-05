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
package au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.hopping;

import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.DataAccessPluginAdaptorType;
import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.utilities.RDFUtilities;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStoreUtilities;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import au.gov.asd.tac.constellation.graph.schema.rdf.concept.RDFConcept;
import au.gov.asd.tac.constellation.plugins.Plugin;
import au.gov.asd.tac.constellation.plugins.PluginException;
import au.gov.asd.tac.constellation.plugins.PluginInfo;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.PluginType;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameter;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import au.gov.asd.tac.constellation.plugins.parameters.types.FileParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.FileParameterType.FileParameterValue;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.collections.map.MultiKeyMap;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({
    @ServiceProvider(service = DataAccessPlugin.class),
    @ServiceProvider(service = Plugin.class)})
@PluginInfo(pluginType = PluginType.IMPORT, tags = {"HOP"})
@NbBundle.Messages("QueryRDFDataSourcesPlugin=Query an RDF Data Store")
public class QueryRDFDataSourcesPlugin extends RecordStoreQueryPlugin implements DataAccessPlugin {

    private static final Logger LOGGER = Logger.getLogger(QueryRDFDataSourcesPlugin.class.getName());
    // parameters
    public static final String RDF_DATA_STORE_URI_PARAMETER_ID = PluginParameter.buildId(QueryRDFDataSourcesPlugin.class, "data_store_uri");
    private static final String SOURCE_RDFIDENTIFIER = GraphRecordStoreUtilities.SOURCE + RDFConcept.VertexAttribute.RDFIDENTIFIER;
    //private static final String SOURCE_IDENTIFIER = GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER;
    private static int layer_Mask = 5;

    final MultiKeyMap literalToValue = MultiKeyMap.decorate(new LinkedMap());
    final Map<String, String> subjectToType = new HashMap<>();
    final Map<String, String> bnodeToSubject = new HashMap<>();

//    @Override
//    protected void edit(GraphWriteMethods wg, PluginInteraction interaction, PluginParameters parameters) throws InterruptedException, PluginException {
//        super.edit(wg, interaction, parameters);
//        final int graphVertexCount = wg.getVertexCount();
//        for (int position = 0; position < graphVertexCount; position++) {
//            final int currentVertexId = wg.getVertex(position);
//            //Set the layer 2 for now
//            final int layerMaskAttributeId = LayersConcept.VertexAttribute.LAYER_MASK.ensure(wg);
//            wg.setStringValue(layerMaskAttributeId, currentVertexId, layer_Mask);
//        }
//        PluginExecution.withPlugin(VisualSchemaPluginRegistry.COMPLETE_SCHEMA).executeNow(wg);
//        PluginExecutor.startWith(InteractiveGraphPluginRegistry.RESET_VIEW).executeNow(wg);
//    }
    @Override
    protected RecordStore query(RecordStore query, PluginInteraction interaction, PluginParameters parameters) throws InterruptedException, PluginException {
        GraphRecordStore recordStore = new GraphRecordStore();
        String identifier = "";
        query.reset();
        while (query.next()) {
            identifier = query.get(SOURCE_RDFIDENTIFIER);
            if (identifier == null) {
                continue;
//                //Try to query with the IDENTIFIER
//                identifier = query.get(SOURCE_IDENTIFIER);
            }

            final String inputFilename = parameters.getParameters().get(RDF_DATA_STORE_URI_PARAMETER_ID).getStringValue();

            Repository repository = new SPARQLRepository(inputFilename);
            RepositoryConnection conn = repository.getConnection();
            try {
                StringBuilder qb = new StringBuilder();
                qb.append("DESCRIBE <");
                qb.append(identifier);
                qb.append(">");

                GraphQuery graphQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, qb.toString());

                try (GraphQueryResult queryResult = graphQuery.evaluate()) {
                    RDFUtilities.PopulateRecordStore(recordStore, queryResult, subjectToType, literalToValue, layer_Mask);

                } catch (RDF4JException e) {
                    LOGGER.log(Level.SEVERE, "An error occured: {0}", e);
                }

            } catch (RDF4JException e) {
                LOGGER.log(Level.SEVERE, "An error occured: {0}", e);
            } finally {
                conn.close();
            }
        }
        return recordStore;
    }

    @Override
    public String getType() {
        return DataAccessPluginAdaptorType.HOP;
    }

    @Override
    public int getPosition() {
        return 101;
    }

    @Override
    public String getDescription() {
        return "Query an RDF data store";
    }

    @Override
    public PluginParameters createParameters() {
        final PluginParameters params = new PluginParameters();

        final PluginParameter<FileParameterValue> inputDataStoreUriParameter = FileParameterType.build(RDF_DATA_STORE_URI_PARAMETER_ID);
        inputDataStoreUriParameter.setName("Data Store");
        inputDataStoreUriParameter.setDescription("RDF Data Store URI");
        inputDataStoreUriParameter.setStringValue("http://dbpedia.org/sparql");
        params.addParameter(inputDataStoreUriParameter);

        return params;
    }
}
