package com.cyoda.presto;

import com.cyoda.presto.logging.SupplierLogger;
import com.google.common.collect.ImmutableList;
import io.trino.spi.HostAddress;
import io.trino.spi.Node;
import io.trino.spi.NodeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CyodaSplitDispatcher {

    protected static final SupplierLogger LOG = SupplierLogger.get(CyodaSplitDispatcher.class);

    private final NodeManager nodeManager;
    private final Node coordinator;

    private volatile List<Node> nodeBuckets;

    public CyodaSplitDispatcher(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        this.coordinator = nodeManager.getAllNodes().stream()
                .filter(Node::isCoordinator)
                .findAny()
                .orElseThrow();
    }

    private void dispatchToWorkers(CyodaSplit split){
        if (!split.getAddresses().isEmpty() || split.getReportConfigId() == null) return;
        refreshBuckets();
        int hash = Math.abs(Objects.hash(split.getReportConfigId(), split.getReportId(), split.getGroupJsonBase64(), split.getPage()));
        split.getAddresses().add(HostAddress.fromUri(nodeBuckets.get(hash % nodeBuckets.size()).getHttpUri()));
    }

    private void dispatchToCoordinator(CyodaSplit split){
        split.getAddresses().add(HostAddress.fromUri(coordinator.getHttpUri()));
    }

    public void dispatch(List<CyodaSplit> splits){
        for (CyodaSplit split : splits) {
            if (split.isAssignToCoordinator()){
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
