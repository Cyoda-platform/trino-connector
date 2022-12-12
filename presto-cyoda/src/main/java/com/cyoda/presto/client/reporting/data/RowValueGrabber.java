package com.cyoda.presto.client.reporting.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RowValueGrabber {

    int listDimensions = 0;
    RowGrabberNode rootNode;

    public RowValueGrabber(String columnPath) {
        String[] path = columnPath.split("\\.");
        rootNode = new MapGrabber(path[0]); //path[0] should not be an index
        RowGrabberNode lastNode = rootNode;
        for (int i = 1; i < path.length; i++) {
            String next = path[i];
            if (next.startsWith("[")) { // an index
                String key = next.substring(1, next.length() - 1);
                //TODO maps are not supported for now
                if ("*".equals(key)) {
                    listDimensions++;
                    lastNode.next = new FlatListNode();
                } else {
                    lastNode.next = new ListItemNode(Integer.parseInt(key));
                }
            } else { // a field
                lastNode.next = new MapGrabber(next);
            }
            lastNode = lastNode.next;
        }
    }

    public Object grab(Object source) {
        return flattenList(rootNode.grab(source), listDimensions);
    }

    private Object flattenList(Object grabbed, int listDimensions) {
        if (grabbed == null || listDimensions < 2)
            return grabbed;

        List<Object> list = new ArrayList<>();
        for (Object item : (List<Object>) grabbed) {
            if (item == null) continue;
            list.addAll((List<Object>) flattenList(item, listDimensions - 1));
        }
        return list;
    }

    private static abstract class RowGrabberNode {
        RowGrabberNode next = null;

        public abstract Object grab(Object source);

        Object processFollowingGrabs(Object grabbed) {
            if (next == null || grabbed == null) return grabbed;
            return next.grab(grabbed);
        }
    }

    private static class MapGrabber extends RowGrabberNode {
        private final String key;

        public MapGrabber(String key) {
            this.key = key;
        }

        @Override
        public Object grab(Object source) {
            return processFollowingGrabs(((Map<String, Object>) source).get(key));
        }
    }

    private class FlatListNode extends RowGrabberNode {
        @Override
        public Object grab(Object source) {
            if (next == null) return source;
            return ((List) source).stream().map(next::grab).collect(Collectors.toList());
        }
    }

    private static class ListItemNode extends RowGrabberNode {
        private final int idx;

        public ListItemNode(int idx) {
            this.idx = idx;
        }

        @Override
        public Object grab(Object source) {
            return processFollowingGrabs(((List) source).get(idx));
        }
    }
}
