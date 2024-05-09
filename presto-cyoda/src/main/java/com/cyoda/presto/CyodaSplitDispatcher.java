package com.cyoda.presto;

import com.cyoda.presto.logging.SupplierLogger;
import com.google.common.collect.ImmutableList;
import io.trino.spi.HostAddress;
import io.trino.spi.Node;
import io.trino.spi.NodeManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CyodaSplitDispatcher {

    protected static final SupplierLogger LOG = SupplierLogger.get(CyodaSplitDispatcher.class);

    private final NodeManager nodeManager;
    private final Node coordinator;
    private volatile List<Node> nodeBuckets;
    // Map to track worker load
//    private final Map<Node, Integer> workerLoadMap = new ConcurrentHashMap<>();

    public CyodaSplitDispatcher(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        this.coordinator = nodeManager.getAllNodes().stream()
                .filter(Node::isCoordinator)
                .findAny()
                .orElseThrow();
        refreshBuckets(); // Initialize nodeBuckets and workerLoadMap
    }

    private void dispatchToWorkers(CyodaSplit split) {
        if (!split.getAddresses().isEmpty() || split.getCyodaTableMetaId() == null) return;
        if (split.getReportId() == null) return;
        refreshBuckets();
        Node dispatched;
        int weight;
//        if (split.getReportId() != null){
            int hash = Math.abs(Objects.hash(split.getCyodaTableMetaId(), split.getReportId(), split.getGroupJsonBase64(), split.getPage()));
            dispatched = nodeBuckets.get(hash % nodeBuckets.size());
//            weight = 1;
//        }
//        else {
//            dispatched = Collections.min(workerLoadMap.entrySet(), Map.Entry.comparingByValue()).getKey();
//            weight = split.getConstraint() != null && !split.getConstraint().isAll() ? 1 : 5;
//        }
//        // Update the load of the worker node
//        Integer currentLoad = workerLoadMap.getOrDefault(dispatched, 0);
//        workerLoadMap.put(dispatched, currentLoad + weight);
//
        split.getAddresses().add(HostAddress.fromUri(dispatched.getHttpUri()));
    }

    private void dispatchToCoordinator(CyodaSplit split){
        split.getAddresses().add(HostAddress.fromUri(coordinator.getHttpUri()));
    }

    public void dispatch(List<CyodaSplit> splits){
        for (CyodaSplit split : splits) {
            if (split.isAssignToCoordinator()) {
                dispatchToCoordinator(split);
            } else {
                dispatchToWorkers(split);
            }
        }
    }

    public void bucketRestructureEvent(List<Node> old, List<Node> niyu){
        //TODO cache invalidation
        LOG.warn("Node list changed, restructuring buckets:\n  old -- %s\n  new -- %s", old, niyu);
    }

    private boolean shouldRefresh(Set<Node> workers){
        return nodeBuckets == null
                || nodeBuckets.size() != workers.size()
                || !workers.containsAll(nodeBuckets);
    }

    private void refreshBuckets(){
        Set<Node> workers = nodeManager.getWorkerNodes();
        if (shouldRefresh(workers)) {
            synchronized (this){
                if (shouldRefresh(workers)) {
                    List<Node> newBuckets = new ImmutableList.Builder<Node>().addAll(workers).build();
                    bucketRestructureEvent(nodeBuckets, newBuckets);
                    nodeBuckets = newBuckets;
                }
            }
        }

    }
}
