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
package au.gov.asd.tac.constellations.dataaccess.adaptors.providers.gaffer;

import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser;
import static uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser.DEFAULT_SERIALISER_CLASS_NAME;
import uk.gov.gchq.gaffer.sketches.serialisation.json.SketchesJsonModules;

/**
 *
 * @author GCHQDeveloper601
 */
public class GafferSimpleQueryNGTest {

    public GafferSimpleQueryNGTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        JSONSerialiser.update(DEFAULT_SERIALISER_CLASS_NAME, SketchesJsonModules.class.getCanonicalName(), Boolean.TRUE);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of class ExtractFromContentPlugin.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testAddResultsToRecordStore() {
        GafferSimpleQuery simpleQuery = new GafferSimpleQuery();

        final RecordStore recordStore = new GraphRecordStore();
        List<Element> elements = null;

        elements = fetchElementsFromFile("resources/exampleGafferResponse.json");

        elements.forEach(e -> simpleQuery.addResultsToRecordStore(e, recordStore));
        assertEquals(recordStore.size(), 3);
    }

    private List<Element> fetchElementsFromFile(String path) {
        try {
            return JSONSerialiser.deserialise(Files.readAllBytes(new File(getClass().getResource(path).toURI()).toPath()), new TypeReference<List<Element>>() {
            });
        } catch (IOException | URISyntaxException ex) {
            return new ArrayList<>();
        }
    }
}
