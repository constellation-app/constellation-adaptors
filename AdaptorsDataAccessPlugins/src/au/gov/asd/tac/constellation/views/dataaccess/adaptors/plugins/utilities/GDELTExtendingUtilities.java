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
package au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.utilities;

import au.gov.asd.tac.constellation.graph.processing.GraphRecordStore;
import au.gov.asd.tac.constellation.graph.processing.GraphRecordStoreUtilities;
import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.AnalyticConcept;
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.SpatialConcept;
import au.gov.asd.tac.constellation.graph.schema.analytic.concept.TemporalConcept;
import au.gov.asd.tac.constellation.graph.schema.visual.concept.VisualConcept;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This class contains utilities functions for extending
 * from GDELT CSVs.
 * 
 * @author canis_majoris
 */
public class GDELTExtendingUtilities {
    
    public static RecordStore hopRelationships(GDELTDateTime gdt, List<String> options, int limit, List<String> labels) throws MalformedURLException, IOException {

        ZipInputStream zis = null;
        RecordStore results = null;
        try {
            zis = new ZipInputStream(new URL(gdt.url).openStream());
            
            final ZipEntry ze = zis.getNextEntry();
            if (ze.getName().equals(gdt.file)) {
                results = readRelationshipsToHop(limit, gdt.dt, options, ze, zis, labels);
            }
            
        } finally {
            if (zis != null) {
                zis.close();
            }
        }
        
        return results;
    }
    
    public static RecordStore readRelationshipsToHop(int limit, String dt, List<String> options, ZipEntry ze, ZipInputStream zis, List<String> labels) throws IOException {
        final RecordStore results = new GraphRecordStore();
        int total = 0;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(zis));
            String line = br.readLine();
            while ((line =  br.readLine()) != null) {
                if (total >= limit) {
                    break;
                }
                
                final String[] fields = line.split("\t");
                // transaction attributes

                final String[] persons = fields[5].split(";");
                final String[] organisations = fields[6].split(";");
                final String[] themes = fields[3].split(";");
                final String[] locations = fields[4].split(";"); 
                final String[] sources = fields[9].split(";");
                final String[] sourceURLs = fields[10].split(";"); 
                
                final String tone = fields[7]; // 6 semi-colon delimited
                final String cameoEventIds = fields[8]; // semi-colon delimited
                
                for (final String label : labels) {
                    final String identifier = label.split("<")[0];
                    final String type = label.split("<")[1].split(">")[0];
                    
                    if (line.contains(identifier)) {
                        if (type.equals("Person")) {
                            for (int i = 0; i < persons.length; i++) {
                                if (!persons[i].equals(identifier)) {
                                    continue;
                                }
                                if (total >= limit) {
                                    break;
                                }
                                if (options.contains("Person - Person")) {
                                    for (int j = 0; j < persons.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = persons[i];
                                        final String two = persons[j];
                                        if (two.equals(identifier)) {
                                            continue;
                                        }

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.PERSON);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, two);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.PERSON);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.RELATIONSHIP);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }

                                if (options.contains("Person - Organisation")) {
                                    for (int j = 0; j < organisations.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = persons[i];
                                        final String two = organisations[j];

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.PERSON);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, two);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.ORGANISATION);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.CORRELATION);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }
                                if (options.contains("Person - Theme")) {
                                    for (int j = 0; j < themes.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = persons[i];
                                        final String two = themes[j];

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.PERSON);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, two);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.WORD);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.BEHAVIOUR);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }

                                if (options.contains("Person - Location")) {
                                    for (int j = 0; j < locations.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = persons[i];
                                        final String[] locationInfo = locations[j].split("#");

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.PERSON);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, locationInfo[1]);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.LOCATION);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + SpatialConcept.VertexAttribute.COUNTRY, locationInfo[2]);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.LOCATION);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }

                                if (options.contains("Person - Source")) {
                                    for (int j = 0; j < sources.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = persons[i];
                                        final String two = sources[j];

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, two);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.DOCUMENT);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.PERSON);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.REFERENCED);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }

                                if (options.contains("Person - URL")) {
                                    for (int j = 0; j < sourceURLs.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = persons[i];
                                        final String two = sourceURLs[j];

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, two);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.URL);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.PERSON);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.REFERENCED);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }
                            }
                        }
                        else if (type.equals("Organisation")) {
                            for (int i = 0; i < organisations.length; i++) {
                                if (!organisations[i].equals(identifier)) {
                                    continue;
                                }
                                if (total >= limit) {
                                    break;
                                }
                                if (options.contains("Organisation - Organisation")) {
                                    for (int j = 0; j < organisations.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = organisations[i];
                                        final String two = organisations[j];
                                        
                                        if (two.equals(identifier)) {
                                            continue;
                                        }

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.ORGANISATION);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, two);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.ORGANISATION);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.RELATIONSHIP);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }

                                if (options.contains("Organisation - Theme")) {
                                    for (int j = 0; j < themes.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = organisations[i];
                                        final String two = themes[j];

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.ORGANISATION);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, two);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.WORD);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.BEHAVIOUR);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }

                                if (options.contains("Organisation - Source")) {
                                    for (int j = 0; j < sources.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = organisations[i];
                                        final String two = sources[j];

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, two);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.DOCUMENT);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.ORGANISATION);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.REFERENCED);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }

                                if (options.contains("Organisation - URL")) {
                                    for (int j = 0; j < sourceURLs.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = organisations[i];
                                        final String two = sourceURLs[j];

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, two);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.URL);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.ORGANISATION);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.REFERENCED);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }
                            }
                        }
                        else if (type.equals("Word")) {
                            for (int i = 0; i < themes.length; i++) {
                                if (!themes[i].equals(identifier)) {
                                    continue;
                                }
                                if (total >= limit) {
                                    break;
                                }
                                
                                if (options.contains("Person - Theme")) {
                                    for (int j = 0; j < persons.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = persons[j];
                                        final String two = themes[i];

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.PERSON);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, two);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.WORD);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.BEHAVIOUR);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }

                                if (options.contains("Organisation - Theme")) {
                                    for (int j = 0; j < organisations.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = organisations[j];
                                        final String two = themes[i];

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.ORGANISATION);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, two);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.WORD);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.BEHAVIOUR);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }
                            }
                        }
                        else if (type.equals("Document")) {
                            for (int i = 0; i < sources.length; i++) {
                                if (!sources[i].equals(identifier)) {
                                    continue;
                                }
                                if (total >= limit) {
                                    break;
                                }
                                
                                if (options.contains("Person - Source")) {
                                    for (int j = 0; j < persons.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = persons[j];
                                        final String two = sources[i];

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, two);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.DOCUMENT);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.PERSON);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.REFERENCED);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }
                                if (options.contains("Organisation - Source")) {
                                    for (int j = 0; j < organisations.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = organisations[j];
                                        final String two = sources[i];

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, two);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.DOCUMENT);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.ORGANISATION);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.REFERENCED);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }
                            }
                        }
                        else if (type.equals("URL")) {
                            for (int i = 0; i < sourceURLs.length; i++) {
                                if (!sourceURLs[i].equals(identifier)) {
                                    continue;
                                }
                                if (total >= limit) {
                                    break;
                                }
                                
                                if (options.contains("Person - URL")) {
                                    for (int j = 0; j < persons.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = persons[j];
                                        final String two = sourceURLs[i];

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, two);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.URL);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.PERSON);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.REFERENCED);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }
                                if (options.contains("Organisation - URL")) {
                                    for (int j = 0; j < organisations.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = organisations[j];
                                        final String two = sourceURLs[i];

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, two);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.URL);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.ORGANISATION);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.REFERENCED);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }
                            }
                        }
                        else if (type.equals("Location")) {
                            for (int i = 0; i < locations.length; i++) {
                                if (!locations[i].split("#")[1].equals(identifier)) {
                                    continue;
                                }
                                if (total >= limit) {
                                    break;
                                }
                                
                                if (options.contains("Person - Location")) {
                                    for (int j = 0; j < persons.length; j++) {
                                        total++;
                                        if (total >= limit) {
                                            break;
                                        }
                                        final String one = persons[j];
                                        final String[] locationInfo = locations[i].split("#");

                                        results.add();
                                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, one);
                                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.PERSON);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + VisualConcept.VertexAttribute.IDENTIFIER, locationInfo[1]);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.LOCATION);
                                        results.set(GraphRecordStoreUtilities.DESTINATION + SpatialConcept.VertexAttribute.COUNTRY, locationInfo[2]);

                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.TYPE, AnalyticConcept.TransactionType.LOCATION);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + TemporalConcept.TransactionAttribute.DATETIME, dt);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + AnalyticConcept.TransactionAttribute.SOURCE, cameoEventIds);
                                        results.set(GraphRecordStoreUtilities.TRANSACTION + "Tone", tone);
                                    }
                                }
                            }
                        }
                        
                        
                    }
                }
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
        return results;
    }
}
