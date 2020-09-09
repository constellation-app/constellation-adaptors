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
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.AnalyticConcept;
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.SpatialConcept;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Nova
 */
public class DefaultRDFMap extends AbstractRDFMap {

    private final String TYPEPREDICATE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private static final DefaultRDFMap instance = new DefaultRDFMap();

    private DefaultRDFMap() {
        this.NAME = "Defulut";
        System.out.println("Singleton is Instantiated."); 
    }
    
    public static synchronized DefaultRDFMap getInstance()
    {
      return instance;
    }

    @Override
    public Map<String, String> getPrefixes() {
        HashMap<String, String> prefixes = new HashMap<>();
        prefixes.put("owl", "http://www.w3.org/2002/07/owl#");
        prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        prefixes.put("skos", "http://www.w3.org/2004/02/skos/core#");
        return prefixes;
    }

    @Override
    public Map<String, String> getPredicateMap() {
        Map<String, String> prefixes = getPrefixes();
        HashMap<String, String> predicateMap = new HashMap<>();
        predicateMap.put(prefixes.get("foaf") + "name", GraphRecordStoreUtilities.SOURCE + SpatialConcept.VertexAttribute.COUNTRY);
        predicateMap.put(TYPEPREDICATE, GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE);
        return predicateMap;
    }
}
