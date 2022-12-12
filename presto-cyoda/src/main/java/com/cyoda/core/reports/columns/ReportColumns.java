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

package com.cyoda.core.reports.columns;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import java.util.List;
import java.util.Map;

/**
 * Container object used only for storing list of ReportColumn, to be able to serialize it with JodaBeanTypeCodec
 */
public class ReportColumns {

    private final List<ReportColumn> columns;

    @JsonCreator
    public ReportColumns(
            @JsonProperty("columns")
            List<ReportColumn> columns
    ) {
        this.columns = columns;
    }

    @JsonProperty("columns")
    public List<ReportColumn> getColumns() {
        return columns;
    }
}
