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

import java.util.HashMap;
import java.util.Map;

public class GridConfigFieldsView {
    public static final String ID_COLUMN_NAME = "id";
    public static final String NAME_COLUMN_NAME = "reportName";
    public static final String DESCRIPTION_COLUMN_NAME = "description";
    public static final String TYPE_COLUMN_NAME = "type";
    public static final String USER_ID_COLUMN_NAME = "userId";
    public static final String CREATION_DATE_COLUMN_NAME = "creationDate";

    private final Map<String, String> gridConfigFields = new HashMap<>();

    public void addField(String fieldName, String fieldValue) {
        gridConfigFields.put(fieldName, fieldValue);
    }

    public Map<String, String> getGridConfigFields() {
        return gridConfigFields;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("gridConfigFields", gridConfigFields)
                .toString();
    }
}
