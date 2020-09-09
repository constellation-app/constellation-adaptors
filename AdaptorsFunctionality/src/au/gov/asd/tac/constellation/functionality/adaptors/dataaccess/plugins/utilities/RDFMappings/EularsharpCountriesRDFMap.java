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
package au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.utilities.RDFMappings;

import au.gov.asd.tac.constellation.graph.processing.GraphRecordStoreUtilities;
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.SpatialConcept;
import java.util.Map;

/**
 *
 * @author Nova
 */
public class EularsharpCountriesRDFMap extends AbstractRDFMap {
    
    private static final EularsharpCountriesRDFMap instance = new EularsharpCountriesRDFMap();
    
    private EularsharpCountriesRDFMap() {
        this.NAME = "EularsharpCountries";
        System.out.println("Singleton is Instantiated."); 
    }
    
    public static synchronized EularsharpCountriesRDFMap getInstance() {
        return instance;
    }

    @Override
    public Map<String, String> getPredicateMap() {
        Map<String, String> prefixes = getPrefixes();
        
        Map<String, String> predicateMap = RDFMapStorage.getMap("default").getPredicateMap();
        predicateMap.put(prefixes.get("foaf") + "name", GraphRecordStoreUtilities.SOURCE + SpatialConcept.VertexAttribute.COUNTRY);
        return predicateMap;
    }

    @Override
    public Map<String, String> getPrefixes() {
        Map<String, String> prefixes = RDFMapStorage.getMap("default").getPrefixes();
        prefixes.put("country", "http://eulersharp.sourceforge.net/2003/03swap/countries#");
        prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
        prefixes.put("jur", "http://sweet.jpl.nasa.gov/2.3/humanJurisdiction.owl#");
        prefixes.put("dce", "http://purl.org/dc/elements/1.1/");
        prefixes.put("dct", "http://purl.org/dc/terms/");
        return prefixes;
    }
      
}
