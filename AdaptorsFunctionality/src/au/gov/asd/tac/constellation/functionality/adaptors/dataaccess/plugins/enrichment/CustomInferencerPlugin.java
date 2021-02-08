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
import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.utilities.RDFUtilities;
import au.gov.asd.tac.constellation.graph.GraphWriteMethods;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.plugins.Plugin;
import au.gov.asd.tac.constellation.plugins.PluginException;
import au.gov.asd.tac.constellation.plugins.PluginInfo;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.PluginType;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameter;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import au.gov.asd.tac.constellation.plugins.parameters.types.FileParameterType;
import au.gov.asd.tac.constellation.plugins.parameters.types.FileParameterType.FileParameterValue;
import au.gov.asd.tac.constellation.plugins.templates.SimpleEditPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.inferencer.fc.CustomGraphQueryInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({
    @ServiceProvider(service = DataAccessPlugin.class),
    @ServiceProvider(service = Plugin.class)})
@PluginInfo(pluginType = PluginType.IMPORT, tags = {"ENRICH"})
@NbBundle.Messages("CustomInferencerPlugin=Custom Inferencing")
public class CustomInferencerPlugin extends SimpleEditPlugin implements DataAccessPlugin {

    private static final Logger LOGGER = Logger.getLogger(CustomInferencerPlugin.class.getName());
    // parameters
    public static final String RULE_QUERY_PARAMETER_ID = PluginParameter.buildId(CustomInferencerPlugin.class, "rule_query");
    public static final String MATCH_QUERY_PARAMETER_ID = PluginParameter.buildId(CustomInferencerPlugin.class, "match_query");

    private static int layer_Mask = 9;

    final Map<String, String> subjectToType = new HashMap<>();
    final Map<String, String> bnodeToSubject = new HashMap<>();

    @Override
    public void edit(GraphWriteMethods graph, final PluginInteraction interaction, final PluginParameters parameters) throws InterruptedException, PluginException {
        final GraphRecordStore results = new GraphRecordStore();
        
        //populate model from query or the whole graph as required
        Model model = RDFUtilities.getGraphModel(graph);

        LOGGER.info("Apply the custom inferencing rule...");

        final String rule = parameters.getParameters().get(RULE_QUERY_PARAMETER_ID).getStringValue();
        final String match = parameters.getParameters().get(MATCH_QUERY_PARAMETER_ID).getStringValue();

        //  apply the inferenceing rule
//        System.out.println("5) apply the inferenceing rule");
//        String pre = "PREFIX : <http://foo.org/bar#>\n";
//        String  = pre + "CONSTRUCT { ?p :relatesTo :Cryptography } WHERE "
//                + "{ { :Bob ?p :Alice } UNION { :Alice ?p :Bob } }";
//        String match = pre + "CONSTRUCT { ?p :relatesTo :Cryptography } "
//                + "WHERE { ?p :relatesTo :Cryptography }";
        final Repository repo = new SailRepository(new CustomGraphQueryInferencer(new MemoryStore(), QueryLanguage.SPARQL, rule, match));

        try (RepositoryConnection conn = repo.getConnection()) {
            conn.add(model);

            try (RepositoryResult<Statement> repositoryResult = conn.getStatements(null, null, null);) {
                RDFUtilities.PopulateRecordStore(results, repositoryResult, subjectToType, layer_Mask);
            }

        } finally {
            repo.shutDown();
        }
    }

    @Override
    public String getType() {
        return DataAccessPluginAdaptorType.ENRICH;
    }

    @Override
    public int getPosition() {
        return 103;
    }

    @Override
    public String getDescription() {
        return "Apply Custom Inferencing";
    }

    @Override
    public PluginParameters createParameters() {
        final PluginParameters params = new PluginParameters();

        final PluginParameter<FileParameterValue> inputRuleQueryParameter = FileParameterType.build(RULE_QUERY_PARAMETER_ID);
        inputRuleQueryParameter.setName("Rule Query");
        inputRuleQueryParameter.setDescription("Rule SPARQL Query");
        inputRuleQueryParameter.setStringValue("PREFIX : <http://foo.org/bar#>\n"
                + "CONSTRUCT { ?p :relatesTo :Cryptography } WHERE "
                + "{ { :Bob ?p :Alice } UNION { :Alice ?p :Bob } }");
        params.addParameter(inputRuleQueryParameter);

        final PluginParameter<FileParameterValue> inputMatchQueryParameter = FileParameterType.build(MATCH_QUERY_PARAMETER_ID);
        inputMatchQueryParameter.setName("Match Query");
        inputMatchQueryParameter.setDescription("Match SPARQL Query");
        inputMatchQueryParameter.setStringValue("PREFIX : <http://foo.org/bar#>\n"
                + "CONSTRUCT { ?p :relatesTo :Cryptography } "
                + "WHERE { ?p :relatesTo :Cryptography }");
        params.addParameter(inputMatchQueryParameter);

        return params;
    }
}
