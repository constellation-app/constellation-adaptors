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
package au.gov.asd.tac.constellations.dataaccess.adaptors.au.gov.asd.tac.constellations.dataaccess.adaptors.providers.gaffer.example;

import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Helper class to generate the queries to send to Gaffer for the example Road
 * Traffic Data Source
 *
 * @author GCHQDeveloper601
 */
public enum GafferSimpleQueryTypes {

    GetOneHop("Get One Hop") {
        @Override
        public void performQuery(final List<String> queryIds, final RecordStore recordStore) {
            gafferSimpleQuery.queryForOneHop(queryIds, recordStore);
        }
    }, GetTwoHop("Get Two Hop") {
        @Override
        public void performQuery(final List<String> queryIds, final RecordStore recordStore) {
            gafferSimpleQuery.queryForTwoHop(queryIds, recordStore);
        }
    };

    private static GafferSimpleQuery gafferSimpleQuery = new GafferSimpleQuery();
    private static final Map<String, GafferSimpleQueryTypes> BY_LABEL = new HashMap<>();

    static {
        for (GafferSimpleQueryTypes value : values()) {
            BY_LABEL.put(value.label, value);
        }
    }

    final private String label;

    String getLabel() {
        return this.label;
    }

    GafferSimpleQueryTypes(final String label) {
        this.label = label;
    }

    static Stream<GafferSimpleQueryTypes> stream() {
        return Stream.of(GafferSimpleQueryTypes.values());
    }

    static GafferSimpleQueryTypes valueOfLabel(final String label) {
        return BY_LABEL.get(label);
    }

    abstract void performQuery(final List<String> queryIds, final RecordStore recordStore);

    void setUrl(final String url) {
        gafferSimpleQuery.setUrl(url);
    }
}
