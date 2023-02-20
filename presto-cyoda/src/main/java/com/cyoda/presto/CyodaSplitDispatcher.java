package com.cyoda.presto;

import com.cyoda.presto.logging.SupplierLogger;
import io.trino.spi.HostAddress;
import io.trino.spi.Node;
import io.trino.spi.NodeManager;

import java.net.URI;
import java.util.List;
import java.util.Objects;

public class CyodaSplitDispatcher {

    protected static final SupplierLogger LOG = SupplierLogger.get(CyodaSplitDispatcher.class);

    private final NodeManager nodeManager;
    private final Node coordinator;

    private URI[] nodeBuckets;

    public CyodaSplitDispatcher(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        this.coordinator = nodeManager.getAllNodes().stream()
                .filter(Node::isCoordinator)
                .findAny()
                .orElseThrow();
        setupBuckets();
//        nodeManager.addNodeChangeListener(allNodes -> {
//            LOG.warn("Node list changed. Restructuring cache buckets");
//            setupBuckets();
//        });
    }

    private void dispatchToWorkers(CyodaSplit split){
        if (!split.getAddresses().isEmpty() || split.getReportConfigId() == null) return;
        int hash = Objects.hash(split.getReportConfigId(), split.getReportId(), split.getGroupJsonBase64(), split.getPage());
        split.getAddresses().add(HostAddress.fromUri(nodeBuckets[hash % nodeBuckets.length]));
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

    private void setupBuckets(){
        nodeBuckets = nodeManager.getWorkerNodes().stream().map(Node::getHttpUri).toList().toArray(new URI[]{});
    }
}
