package au.gov.asd.tac.constellation.functionality.adaptors.dataaccess.plugins.utilities;

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
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
/**
 *
 * @author canis_majoris
 */
public class GDELTDateTime {
    
    private static final String HEADER = "http://data.gdeltproject.org/gkg/";
    private static final String FOOTER = ".gkg.csv";
    private static final String ZIPPER = ".zip";
    
    private final int h;
    private final int m;
    private final int d;
    
    private final String day;
    public final String dt;
    public final String url;
    public final String file;
    
    public GDELTDateTime(LocalDate localDate) {
        this.h = localDate.get(ChronoField.YEAR);
        this.m = localDate.get(ChronoField.MONTH_OF_YEAR);
        this.d = localDate.get(ChronoField.DAY_OF_MONTH);
        
        this.day = String.format("%04d%02d%02d", h, m, d);
        this.dt = String.format("%04d-%02d-%02d 00:00:00.000Z", h, m, d);
        this.url = HEADER + day + FOOTER + ZIPPER;
        this.file = day + FOOTER;
    }
}
