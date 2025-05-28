/*
 * Copyright 2010-2025 Australian Signals Directorate
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

import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.openide.util.Exceptions;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser;
import static uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser.DEFAULT_SERIALISER_CLASS_NAME;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.sketches.serialisation.json.SketchesJsonModules;

/**
 *
 * @author GCHQDeveloper601
 */
public class GafferSimpleQueryNGTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        //JSONSerialiser.update(DEFAULT_SERIALISER_CLASS_NAME, SketchesJsonModules.class.getCanonicalName(), Boolean.TRUE);
        System.setProperty(JSONSerialiser.JSON_SERIALISER_MODULES, SketchesJsonModules.class.getName());
        JSONSerialiser.update();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() {
        JSONSerialiser.update(DEFAULT_SERIALISER_CLASS_NAME, SketchesJsonModules.class.getCanonicalName(), Boolean.TRUE);
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of class ExtractFromContentPlugin.
     *
     */
    @Test
    public void testAddResultsToRecordStore() {
        final GafferSimpleQuery simpleQuery = new GafferSimpleQuery();
        final RecordStore recordStore = new GraphRecordStore();
        final List<Element> elements = fetchElementsFromFile("resources/exampleGafferResponseOneHop.json");

        elements.forEach(e -> simpleQuery.addResultsToRecordStore(e, recordStore));

        assertEquals(recordStore.size(), 3);
    }

    @Test
    public void testQueryForOneHop() {
        final RecordStore recordStore = new GraphRecordStore();
        final GafferConnector connMock = mock(GafferConnector.class);
        final GafferSimpleQuery simpleQuery = new GafferSimpleQuery();

        final List<String> queryIds = Arrays.asList("M4");
        final OperationChain opChain = simpleQuery.buildOneHopChain(queryIds);

        final List<Element> elements = fetchElementsFromFile("resources/exampleGafferResponseOneHop.json");
        try {
            when(connMock.sendQueryToGaffer(opChain)).thenReturn(elements);
        } catch (final IOException | InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }
        simpleQuery.setGafferConnectorService(connMock);
        simpleQuery.fetchResults(opChain, recordStore);
        assertEquals(recordStore.size(), 3);
    }

    @Test
    public void testQueryForTwoHop() {
        final RecordStore recordStore = new GraphRecordStore();
        final GafferConnector connMock = mock(GafferConnector.class);
        final GafferSimpleQuery simpleQuery = new GafferSimpleQuery();

        final List<String> queryIds = Arrays.asList("M4");
        final OperationChain opChain = simpleQuery.buildTwoHopChain(queryIds);

        final List<Element> elements = fetchElementsFromFile("resources/exampleGafferResponseTwoHop.json");
        try {
            when(connMock.sendQueryToGaffer(opChain)).thenReturn(elements);
        } catch (final IOException | InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }
        simpleQuery.setGafferConnectorService(connMock);
        simpleQuery.fetchResults(opChain, recordStore);
        assertEquals(recordStore.size(), 4);
    }

    private List<Element> fetchElementsFromFile(final String path) {
        try {
            return Arrays.asList(JSONSerialiser.getMapper().readValue(Files.readAllBytes(new File(getClass().getResource(path).toURI()).toPath()), Element[].class));
        } catch (final IOException | URISyntaxException ex) {
            return new ArrayList<>();
        }
    }

}
