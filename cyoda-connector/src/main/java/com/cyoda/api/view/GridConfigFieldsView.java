/*
 * Copyright (C) 2022 Cyoda Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.cyoda.api.view;

import com.google.common.base.MoreObjects;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_CREATION_DATE_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_UPDATE_DATE_COLUMN;

public class GridConfigFieldsView {

    private final Map<String, String> gridConfigFields;

    public GridConfigFieldsView() {
        gridConfigFields = new HashMap<>();
    }
    public GridConfigFieldsView(Map<String, String> fields){
        gridConfigFields = fields;
    }

    public void addField(String fieldName, String fieldValue) {
        gridConfigFields.put(fieldName, fieldValue);
    }

    public Map<String, String> getGridConfigFields() {
        return gridConfigFields;
    }

    public String getId(){
        return gridConfigFields.get(REPORT_ID_COLUMN);
    }

    //no cache of dates needed since this class is one-use
    public String getCreationDate(){
        return gridConfigFields.get(REPORT_CREATION_DATE_COLUMN);
    }
    public String getUpdateDate(){
        return gridConfigFields.get(REPORT_UPDATE_DATE_COLUMN);
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("gridConfigFields", gridConfigFields)
                .toString();
    }
}
