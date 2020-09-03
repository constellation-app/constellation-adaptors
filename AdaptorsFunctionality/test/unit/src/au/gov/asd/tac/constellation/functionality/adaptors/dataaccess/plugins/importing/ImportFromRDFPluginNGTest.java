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
package au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.importing;

import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import au.gov.asd.tac.constellation.plugins.PluginInteraction;
import au.gov.asd.tac.constellation.plugins.parameters.PluginParameters;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author arcturus
 */
public class ImportFromRDFPluginNGTest {

    public ImportFromRDFPluginNGTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of query method, of class ImportFromRDFPlugin.
     */
    @Test
    public void testQuery() throws Exception {
        final RecordStore query = new GraphRecordStore();
        PluginInteraction interaction = null;
        PluginParameters parameters = new PluginParameters();
        ImportFromRDFPlugin instance = new ImportFromRDFPlugin();
        RecordStore expResult = new GraphRecordStore();
        RecordStore result = instance.query(query, interaction, parameters);
        assertEquals(expResult, result);
    }

//    /**
//     * Test of getType method, of class ImportFromRDFPlugin.
//     */
//    @Test
//    public void testGetType() {
//        System.out.println("getType");
//        ImportFromRDFPlugin instance = new ImportFromRDFPlugin();
//        String expResult = "";
//        String result = instance.getType();
//        assertEquals(result, expResult);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPosition method, of class ImportFromRDFPlugin.
//     */
//    @Test
//    public void testGetPosition() {
//        System.out.println("getPosition");
//        ImportFromRDFPlugin instance = new ImportFromRDFPlugin();
//        int expResult = 0;
//        int result = instance.getPosition();
//        assertEquals(result, expResult);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

}
