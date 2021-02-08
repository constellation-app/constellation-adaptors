package au.gov.asd.tac.constellation.views.dataaccess.adaptors.plugins.utilities;

import java.time.ZonedDateTime;

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
 * This utility class assists formatting the GDELT timestamps for querying
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
    
    public GDELTDateTime(ZonedDateTime date) {
        this.h = date.getYear();
        this.m = date.getMonthValue();
        this.d = date.getDayOfMonth();
        
        this.day = String.format("%04d%02d%02d", h, m, d);
        this.dt = String.format("%04d-%02d-%02d 00:00:00.000Z", h, m, d);
        this.url = HEADER + day + FOOTER + ZIPPER;
        this.file = day + FOOTER;
    }
}
