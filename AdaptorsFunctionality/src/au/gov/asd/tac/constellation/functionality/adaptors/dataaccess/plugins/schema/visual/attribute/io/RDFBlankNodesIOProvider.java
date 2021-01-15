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
package au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.schema.visual.attribute.io;

import au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.schema.visual.attribute.RDFBlankNodesAttributeDescription;
import au.gov.asd.tac.constellation.graph.Attribute;
import au.gov.asd.tac.constellation.graph.GraphReadMethods;
import au.gov.asd.tac.constellation.graph.GraphWriteMethods;
import au.gov.asd.tac.constellation.graph.attribute.io.AbstractGraphIOProvider;
import au.gov.asd.tac.constellation.graph.attribute.io.GraphByteReader;
import au.gov.asd.tac.constellation.graph.attribute.io.GraphByteWriter;
import au.gov.asd.tac.constellation.utilities.datastructure.ImmutableObjectCache;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.rdf4j.model.Statement;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = AbstractGraphIOProvider.class)
public class RDFBlankNodesIOProvider extends AbstractGraphIOProvider {

    private static final String ATTRIBUTE_NAME = "blank_node";
//    private static final String SUBJECT = "subject";
//    private static final String PREDICATE = "predicate";
//    private static final String OBJECT = "object";

    @Override
    public String getName() {
        return RDFBlankNodesAttributeDescription.ATTRIBUTE_NAME;
    }

    @Override
    public void readObject(final int attributeId, final int elementId, final JsonNode jnode, final GraphWriteMethods graph, final Map<Integer, Integer> vertexMap, final Map<Integer, Integer> transactionMap, final GraphByteReader byteReader, ImmutableObjectCache cache) throws IOException {
        final Set<Statement> bNodeStatements = new HashSet<>();
        if (!jnode.isNull() && jnode.isArray()) {
            for (int i = 0; i < jnode.size(); i++) {
                bNodeStatements.add((Statement) jnode.get(i).get(ATTRIBUTE_NAME));
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
            //final Set<org.eclipse.rdf4j.model.Statement> bNodeStatements = new HashSet<org.eclipse.rdf4j.model.Statement>();

            //Set<org.eclipse.rdf4j.model.Statement> bn = graph.getObjectValue(attr.getId(), elementId);
            Set<Statement> bn = graph.getObjectValue(attr.getId(), elementId);

            //bNodeStatements.addAll(bn);
            //final Set<Statement> bNodeStatements = (Set<Statement>) graph.getObjectValue(attr.getId(), elementId);
            // org.eclipse.rdf4j.sail.memory.model.MemStatement ss = ;
            if (bn == null) {
                jsonGenerator.writeNullField(attr.getName());
            } else {
                try {
                    jsonGenerator.writeArrayFieldStart(attr.getName());

//                for (Statement statement : bn) {
//                    jsonGenerator.writeStartObject();
//                    jsonGenerator.writeObjectField(ATTRIBUTE_NAME, statement);
//                    jsonGenerator.writeEndObject();
//                }
                    bn.stream().forEach((st) -> {

                        try {
                            jsonGenerator.writeStartObject();
                            jsonGenerator.writeObjectField(ATTRIBUTE_NAME, st);
                            jsonGenerator.writeEndObject();
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    });
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
                //------------------Try to convert to an object array and try to seperate object , predicate, subject
//                Object[] statementArray = bNodeStatements.toArray();
//                for (int i = 0; i < statementArray.length; i++) {
//                    Statement statement = (Statement) statementArray[i];
//
//                    jsonGenerator.writeStartObject();
//                    jsonGenerator.writeObjectField(ATTRIBUTE_NAME, statement);
//                    jsonGenerator.writeEndObject();
//
////                    final Resource subject = statement.getSubject();
////                    final IRI predicate = statement.getPredicate();
////                    final Value object = statement.getObject();
////                    jsonGenerator.writeObjectFieldStart(ATTRIBUTE_NAME);
////                    jsonGenerator.writeStringField(SUBJECT, subject.toString());
////                    jsonGenerator.writeStringField(PREDICATE, predicate.toString());
////                    jsonGenerator.writeStringField(OBJECT, object.toString());
////                    jsonGenerator.writeEndObject();
//                }
                //------------------

                jsonGenerator.writeEndArray();
            }
        }
    }
}
