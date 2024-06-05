/*
 * Copyright 2010-2024 Australian Signals Directorate
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This class contains utilities functions for importing entities and
 * relationships from GDELT CSVs.
 *
 * @author canis_majoris
 */
public class GDELTImportingUtilities {

    private static final Logger LOGGER = Logger.getLogger(GDELTImportingUtilities.class.getName());

    public static RecordStore retrieveEntities(final GDELTDateTime gdt, final List<String> options, final int limit) throws IOException {

        RecordStore results = null;
        try (final ZipInputStream zis = new ZipInputStream(URI.create(gdt.getUrl()).toURL().openStream())) {
            
            final ZipEntry ze = zis.getNextEntry();
            if (ze.getName().equals(gdt.getFile())) {
                results = readEntities(limit, gdt.getDt(), options, ze, zis);
            }

        } catch (final FileNotFoundException ex) {
            final String errorMsg = "File not found " + ex.getLocalizedMessage();
            LOGGER.log(Level.WARNING, errorMsg);
        }

        return results;
    }

    public static RecordStore retrieveRelationships(final GDELTDateTime gdt, final List<String> options, final int limit) throws IOException {

        RecordStore results = null;
        try (final ZipInputStream zis = new ZipInputStream(URI.create(gdt.getUrl()).toURL().openStream())) {
            
            final ZipEntry ze = zis.getNextEntry();
            if (ze.getName().equals(gdt.getFile())) {
                results = readRelationships(limit, gdt.getDt(), options, ze, zis);
            }

        } catch (final FileNotFoundException ex) {
            final String errorMsg = "File not found " + ex.getLocalizedMessage();
            LOGGER.log(Level.WARNING, errorMsg);
        }

        return results;
    }

    public static RecordStore readEntities(final int limit, final String dt, final List<String> options, final ZipEntry ze, final ZipInputStream zis) throws IOException {
        final RecordStore results = new GraphRecordStore();
        int total = 0;
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(zis))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
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

                if (options.contains("Person")) {
                    for (int j = 0; j < persons.length; j++) {
                        total++;
                        if (total >= limit) {
                            break;
                        }
                        final String one = persons[j];

                        results.add();
                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, one);
                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.PERSON);
                    }
                }

                if (options.contains("Organisation")) {
                    for (int j = 0; j < organisations.length; j++) {
                        total++;
                        if (total >= limit) {
                            break;
                        }
                        final String one = organisations[j];

                        results.add();
                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, one);
                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.ORGANISATION);
                    }
                }
                if (options.contains("Theme")) {
                    for (int j = 0; j < themes.length; j++) {
                        total++;
                        if (total >= limit) {
                            break;
                        }
                        final String one = themes[j];

                        results.add();
                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, one);
                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.WORD);
                    }
                }

                if (options.contains("Location")) {
                    for (int j = 0; j < locations.length; j++) {
                        total++;
                        if (total >= limit) {
                            break;
                        }
                        final String[] locationInfo = locations[j].split("#");

                        results.add();
                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, locationInfo[1]);
                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.LOCATION);
                        results.set(GraphRecordStoreUtilities.SOURCE + SpatialConcept.VertexAttribute.COUNTRY, locationInfo[2]);
                        results.set(GraphRecordStoreUtilities.SOURCE + SpatialConcept.VertexAttribute.LATITUDE, locationInfo[4]);
                        results.set(GraphRecordStoreUtilities.SOURCE + SpatialConcept.VertexAttribute.LONGITUDE, locationInfo[5]);
                    }
                }

                if (options.contains("Source")) {
                    for (int j = 0; j < sources.length; j++) {
                        total++;
                        if (total >= limit) {
                            break;
                        }
                        final String one = sources[j];

                        results.add();
                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, one);
                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.DOCUMENT);
                    }
                }

                if (options.contains("URL")) {
                    for (int j = 0; j < sourceURLs.length; j++) {
                        total++;
                        if (total >= limit) {
                            break;
                        }
                        final String one = sourceURLs[j];

                        results.add();
                        results.set(GraphRecordStoreUtilities.SOURCE + VisualConcept.VertexAttribute.IDENTIFIER, one);
                        results.set(GraphRecordStoreUtilities.SOURCE + AnalyticConcept.VertexAttribute.TYPE, AnalyticConcept.VertexType.URL);
                    }
                }
            }
        }
        return results;
    }

    public static RecordStore readRelationships(final int limit, final String dt, final List<String> options, final ZipEntry ze, final ZipInputStream zis) throws IOException {
        final RecordStore results = new GraphRecordStore();
        int total = 0;
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(zis))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
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

                for (int i = 0; i < persons.length; i++) {
                    if (total >= limit) {
                        break;
                    }
                    if (options.contains("Person_Person")) {
                        for (int j = i + 1; j < persons.length; j++) {
                            total++;
                            if (total >= limit) {
                                break;
                            }
                            final String one = persons[i];
                            final String two = persons[j];

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

                    if (options.contains("Person_Organisation")) {
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
                    if (options.contains("Person_Theme")) {
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

                    if (options.contains("Person_Location")) {
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

                    if (options.contains("Person_Source")) {
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

                    if (options.contains("Person_URL")) {
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

                for (int i = 0; i < organisations.length; i++) {
                    if (total >= limit) {
                        break;
                    }
                    if (options.contains("Organisation_Organisation")) {
                        for (int j = i + 1; j < organisations.length; j++) {
                            total++;
                            if (total >= limit) {
                                break;
                            }
                            final String one = organisations[i];
                            final String two = organisations[j];

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

                    if (options.contains("Organisation_Theme")) {
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

                    if (options.contains("Organisation_Source")) {
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

                    if (options.contains("Organisation_URL")) {
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
        }
        return results;
    }
}
