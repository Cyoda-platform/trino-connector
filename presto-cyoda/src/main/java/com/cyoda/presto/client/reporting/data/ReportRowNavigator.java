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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Navigates through the CyodaColumnPath notation of a report row to get its value.
 */
public class ReportRowNavigator {

    public static final String LIST_CONSTITUENTS_REGEX = "^(.+?)\\.\\[([0-9|*]+)]$";

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

        //temporary shortcut
        if (cyodaColumpath.endsWith("]")){
            int end = cyodaColumpath.lastIndexOf(".");
            if (end > 0){
                String key = cyodaColumpath.substring(0,end);
                result = reportRow.get(key);
                if (result != null) return result;
            }
        }

        Deque<DequeHandle> deque = new ArrayDeque<>();
        deque.add(new DequeHandle(reportRow,cyodaColumpath));

        while (!deque.isEmpty()) {
            DequeHandle pop = deque.pop();
            Map<String, Object> map = pop.map;
            String path = pop.path;
            int end = path.indexOf("@");
            int start = path.indexOf(".")+1;
            if ( end < start || start < 1 ) {
                start = 0;
            }
            if ( end < 0 ) {
                end=path.length();
                start=path.indexOf(".")+1;
            }
            String key = path.substring(start,end);
            if ( key.contains("#") || key.contains("@")) {
                throw new IllegalArgumentException("Corrupted Path "+cyodaColumpath);
            }
            if ( isListPath(key) ) {
                result = handleList(cyodaColumpath, map, path, end, key);
            } else {
                result = map.get(key);
                if (result != null && end < path.length()) {
                    String rest = path.substring(end + 1);
                    if (!(result instanceof Map)) {
                        throw new IllegalArgumentException("Unexpected end of traversal on " + cyodaColumpath);
                    }
                    //noinspection unchecked
                    deque.add(new DequeHandle((Map<String, Object>) result, rest));
                }
            }
        }
        return result;
    }

    private static Object handleList(String cyodaColumpath, Map<String, Object> map, String path, int end, String key) {
        Object result;
        String theKey = key.replaceFirst(LIST_CONSTITUENTS_REGEX, "$1");
        String theIndex = key.replaceFirst(LIST_CONSTITUENTS_REGEX,"$2");
        result = map.get(theKey);
        List<?> list = (List<?>) result;
        String rest = null;
        if (end < path.length()) {
            rest = path.substring(end + 1);
        }
        ImmutableList.Builder<Object> builder = ImmutableList.builder();
        if (theIndex.equals("*")) {
            if (rest == null){
                return list;
            } else {
                for (Object item : list) {
                    addItem(cyodaColumpath, rest, builder, item);
                }
            }
        } else {
            int index = Integer.parseInt(theIndex);
            Preconditions.checkArgument(list.size()>index,"The List at %s has %s elements, but the requested index is %s",key,list.size(),index);
            if (rest == null) {
                return list.get(index);
            } else {
                addItem(cyodaColumpath, rest, builder, list.get(index));
            }
        }
        return builder.build();
    }

    private static void addItem(String cyodaColumpath, String rest, ImmutableList.Builder<Object> builder, Object item) {
        if (!(item instanceof Map)) {
            throw new IllegalArgumentException("Unexpected end of traversal on " + cyodaColumpath);
        }

        @SuppressWarnings("unchecked")
        Object value = getValue(rest, (Map<String, Object>) item);

        if (value != null) {
            builder.add(value);
        }
    }

    private static boolean isListPath(String key) {
        return key.matches("^.+?\\.\\[[0-9|*]+]$");
    }

    private static class DequeHandle {
        private final Map<String, Object> map;
        private final String path;

        private DequeHandle(Map<String, Object> map, String path) {
            this.map = map;
            this.path = path;
        }
    }
}
