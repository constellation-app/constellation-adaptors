/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.gov.asd.tac.constellations.dataaccess.adaptors.providers.gaffer;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.exception.SerialisationException;
import uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser;
import static uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser.DEFAULT_SERIALISER_CLASS_NAME;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.sketches.serialisation.json.SketchesJsonModules;

/**
 *
 * @author GCHQDeveloper601
 */
public class GafferConnector {

    public static final String JSON_SERIALISER_CLASS = JSONSerialiser.JSON_SERIALISER_CLASS_KEY;
    public static final String JSON_SERIALISER_MODULES = JSONSerialiser.JSON_SERIALISER_MODULES;
    public static final String STRICT_JSON = JSONSerialiser.STRICT_JSON;

    private HttpClient httpClient;

    public GafferConnector() {
        JSONSerialiser.update(DEFAULT_SERIALISER_CLASS_NAME, SketchesJsonModules.class.getCanonicalName(), Boolean.TRUE);
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2) // this is the default
                .build();
    }

    public List<Element> sendQueryToGaffer(String url, OperationChain opChain) throws SerialisationException, IOException, InterruptedException {
        var data = new String(JSONSerialiser.serialise(opChain, true, new String[0]));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url+"/rest/v2/graph/operations/execute"))
                .POST(HttpRequest.BodyPublishers.ofString(data)) // this is the default
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return JSONSerialiser.deserialise(response.body().getBytes(), new TypeReference<List<Element>>() {
        });

    }

}
