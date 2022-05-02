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

import org.joda.beans.ImmutableBean;

public interface ReportColumn extends ImmutableBean {

    enum Type {
        SIMPLE_COLUMN, ALIAS/*, VALUATION*/
    }

    Type getType();

    /**
     * returns name of column.
     *  if its SIMPLE_COLUMN then it will be string representation of CyodaColumnPath(s)
     *  if its ALIASE, then its just alias name
     * @return name of column
     */
    String getName();

}
