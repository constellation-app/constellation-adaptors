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
package au.gov.asd.tac.constellation.graph.schema.rdf.attribute.io;

import au.gov.asd.tac.constellation.graph.Attribute;
import au.gov.asd.tac.constellation.graph.GraphReadMethods;
import au.gov.asd.tac.constellation.graph.GraphWriteMethods;
import au.gov.asd.tac.constellation.graph.attribute.io.AbstractGraphIOProvider;
import au.gov.asd.tac.constellation.graph.attribute.io.GraphByteReader;
import au.gov.asd.tac.constellation.graph.attribute.io.GraphByteWriter;
import au.gov.asd.tac.constellation.graph.schema.rdf.attribute.RDFBlankNodesAttributeDescription;
import au.gov.asd.tac.constellation.utilities.datastructure.ImmutableObjectCache;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = AbstractGraphIOProvider.class)
public class RDFBlankNodesIOProvider extends AbstractGraphIOProvider {

    private static final String SUBJECT = "subject";
    private static final String PREDICATE = "predicate";
    private static final String OBJECT = "object";

    @Override
    public String getName() {
        return RDFBlankNodesAttributeDescription.ATTRIBUTE_NAME;
    }

    @Override
    public void readObject(final int attributeId, final int elementId, final JsonNode jnode, final GraphWriteMethods graph, final Map<Integer, Integer> vertexMap, final Map<Integer, Integer> transactionMap, final GraphByteReader byteReader, ImmutableObjectCache cache) throws IOException {
        final Set<Statement> bNodeStatements = new HashSet<>();
        if (!jnode.isNull() && jnode.isObject()) {

            final ArrayNode blankNodesNode = (ArrayNode) jnode.get("blank_nodes");
            for (final JsonNode jn : blankNodesNode) {
                if (!jn.isNull()) {
                    // TODO: Verify casting of BNodes into IRIs below is okay. It seems to work. (BNodes are not IRIs but both are a Resource or a Value)
                    // Resource subject
                    // IRI predicate
                    // Value object
                    ValueFactory factory = SimpleValueFactory.getInstance();
                    IRI subject = factory.createIRI(jn.get(SUBJECT).asText());
                    IRI predicate = factory.createIRI(jn.get(PREDICATE).asText());
                    IRI object = factory.createIRI(jn.get(OBJECT).asText());
                    Statement statement = factory.createStatement(subject, predicate, object);
                    bNodeStatements.add(statement);
                }
            }
            graph.setObjectValue(attributeId, elementId, bNodeStatements);
        } else {
            final String attrVal = jnode.isNull() ? null : jnode.textValue();
            graph.setStringValue(attributeId, elementId, attrVal);
        }
    }

    @Override
    public void writeObject(final Attribute attr, final int elementId, final JsonGenerator jsonGenerator, final GraphReadMethods graph, final GraphByteWriter byteWriter, final boolean verbose) throws IOException {
        if (verbose || !graph.isDefaultValue(attr.getId(), elementId)) {
            Set<Statement> bn = graph.getObjectValue(attr.getId(), elementId);

            if (bn == null) {
                jsonGenerator.writeNullField(attr.getName());
            } else {
                jsonGenerator.writeObjectFieldStart(attr.getName());
                //Convert to an object array of object, predicate and subject
                jsonGenerator.writeArrayFieldStart("blank_nodes");
                for (Statement statement : bn) {
                    String ss = statement.getSubject().toString();
                    jsonGenerator.writeStartObject();
                    jsonGenerator.writeStringField(SUBJECT, ss);
                    jsonGenerator.writeStringField(PREDICATE, statement.getPredicate().toString());
                    jsonGenerator.writeStringField(OBJECT, statement.getObject().toString());
                    jsonGenerator.writeEndObject();
                }
                jsonGenerator.writeEndArray();
                jsonGenerator.writeEndObject();
            }
        }
    }
}
