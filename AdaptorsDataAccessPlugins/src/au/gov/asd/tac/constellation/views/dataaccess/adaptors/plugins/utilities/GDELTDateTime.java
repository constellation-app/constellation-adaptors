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

import java.time.ZonedDateTime;

/**
 * This utility class assists formatting the GDELT timestamps for querying
 *
 * @author canis_majoris
 */
public class GDELTDateTime {

    private static final String HEADER = "http://data.gdeltproject.org/gkg/";
    private static final String FOOTER = ".gkg.csv";
    private static final String ZIPPER = ".zip";

    private final int y;
    private final int m;
    private final int d;

    private final String day;
    private final String date;
    private final String dt;
    private final String url;
    private final String file;

    public GDELTDateTime(final ZonedDateTime dateTime) {
        this.y = dateTime.getYear();
        this.m = dateTime.getMonthValue();
        this.d = dateTime.getDayOfMonth();

        this.day = String.format("%04d-%02d-%02d", y, m, d);
        this.date = String.format("%04d%02d%02d", y, m, d);
        this.dt = String.format("%04d-%02d-%02d 00:00:00.000Z", y, m, d);
        this.url = HEADER + date + FOOTER + ZIPPER;
        this.file = date + FOOTER;
    }

    public String getDay() {
        return day;
    }

    public String getDate() {
        return date;
    }

    public String getDt() {
        return dt;
    }

    public String getUrl() {
        return url;
    }

    public String getFile() {
        return file;
    }
}
