/*
 * Copyright 2010-2021 Australian Signals Directorate
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
package au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.exception.SerialisationException;
import uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser;
import static uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser.DEFAULT_SERIALISER_CLASS_NAME;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.sketches.serialisation.json.SketchesJsonModules;

/**
 * Simple connector for sending queries to an Instance of Gaffer
 *
 * @author GCHQDeveloper601
 */
public class GafferConnector {

    public static final String JSON_SERIALISER_CLASS = JSONSerialiser.JSON_SERIALISER_CLASS_KEY;
    public static final String JSON_SERIALISER_MODULES = JSONSerialiser.JSON_SERIALISER_MODULES;
    public static final String STRICT_JSON = JSONSerialiser.STRICT_JSON;

    private static final String EXECUTE_ENDPOINT = "/rest/v2/graph/operations/execute";

    private String url;

    private final HttpClient httpClient;

    GafferConnector() {
        JSONSerialiser.update(DEFAULT_SERIALISER_CLASS_NAME, SketchesJsonModules.class.getCanonicalName(), Boolean.TRUE);
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2) // this is the default
                .build();
    }

    GafferConnector(final String url) {
        this();
        JSONSerialiser.update(DEFAULT_SERIALISER_CLASS_NAME, SketchesJsonModules.class.getCanonicalName(), Boolean.TRUE);
        this.url = url;
    }

    public List<Element> sendQueryToGaffer(final OperationChain opChain) throws SerialisationException, IOException, InterruptedException {
        final byte[] data = JSONSerialiser.serialise(opChain, true, new String[0]);
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + EXECUTE_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofByteArray(data)) // this is the default
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return Arrays.asList(JSONSerialiser.deserialise(response.body().getBytes(), Element[].class));
    }

}
