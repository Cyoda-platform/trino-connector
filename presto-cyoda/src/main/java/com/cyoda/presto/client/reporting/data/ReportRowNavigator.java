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

package com.cyoda.presto.client.reporting.data;

import java.util.ArrayList;
import java.util.Collection;

import java.util.List;
import java.util.Map;

/**
 * Navigates through the CyodaColumnPath notation of a report row to get its value.
 */
public class ReportRowNavigator {

    private ReportRowNavigator() {
    }

    /**
     * Navigate through the CyodaColumnPath notation of a report row to get its value
     * @param cyodaColumpath from the DistributedReport result
     * @param reportRow (which is a HashMap)
     * @return the Object found at end of the path
     */
    public static Object getValue(String cyodaColumpath, Map<String, Object> reportRow) {
        Object result = reportRow.get(cyodaColumpath);
        if (result != null) return result;

        String[] path = removeClassNamesFromPath(cyodaColumpath).split("\\.");
        return getValue(path, 1, reportRow.get(path[0]));
    }

    private static Object getValue(String[] path, int cursor, Object object) {
        if (cursor >= path.length || object == null){
            return object;
        }
        String next = path[cursor++];
        if (next.startsWith("[")){ // an index
            String key = next.substring(1, next.length()-1);
            if (object instanceof List) {
                return handleList(path, cursor, (List<?>) object, key);
            } else if (object instanceof Map){
                return handleMap(path, cursor, (Map<?, ?>) object, key);
            } else {
                throw new IllegalArgumentException(String.format("Cannot apply index %s to an object (%s) at path %s",
                        next, object, pathToCurrent(path, cursor-2)));
            }
        } else { // a field
            if (object instanceof Map){
                return getValue(path, cursor, ((Map<String, Object>)object).get(next));
            } else {
                throw new IllegalArgumentException(String.format("Cannot apply path element %s to an object (%s) at path %s",
                        next, object, pathToCurrent(path, cursor-2)));
            }
        }
    }

    private static String pathToCurrent(String[] path, int cursor){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cursor; i++) {
            sb.append(path[i]).append(".");
        }
        sb.append(path[cursor]);
        return sb.toString();
    }

    private static Object handleList(String[] path, int cursor, List<?> list, String key) {
        if ("*".equals(key)){
            if (cursor >= path.length){
                return list;
            } else {
                return handleCollection(path, cursor, list);
            }
        } else {
            int index = Integer.parseInt(key);
            if (index >= list.size()) return null;
            return getValue(path, cursor, list.get(index));
        }
    }

    private static Object handleMap(String[] path, int cursor, Map<?, ?> map, String key) {
        if ("*".equals(key)){
            if (cursor >= path.length)
                return map;
            else
                return handleCollection(path, cursor, map.values());
        } else {
            return getValue(path, cursor, map.get(key));
        }
    }

    public static List<?> handleCollection(String[] path, int cursor, Collection<?> list){
        List<Object> res = new ArrayList<>();
        for (Object item : list){
            Object value = getValue(path, cursor, item);
            if (value instanceof Collection){
                res.addAll((Collection<?>) value);
            } else {
                res.add(value);
            }
        }
        return res;
    }

    public static String removeClassNamesFromPath(String source){
        return source.replaceAll("@[^.]+", "");
    }
}
