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

    private URI[] nodeBuckets;

    public CyodaSplitDispatcher(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        setupBuckets();
//        nodeManager.addNodeChangeListener(allNodes -> {
//            LOG.warn("Node list changed. Restructuring cache buckets");
//            setupBuckets();
//        });
    }

    private void appendNodeUriForSplit(CyodaSplit split){
        if (!split.getAddresses().isEmpty() || split.getReportConfigId() == null) return;
        int hash = Objects.hash(split.getReportConfigId(), split.getReportId(), split.getGroupJsonBase64(), split.getPage());
        split.getAddresses().add(HostAddress.fromUri(nodeBuckets[hash % nodeBuckets.length]));
    }

    public void dispatch(List<CyodaSplit> splits){
        splits.forEach(this::appendNodeUriForSplit);
    }

    private void setupBuckets(){
        nodeBuckets = nodeManager.getWorkerNodes().stream().map(Node::getHttpUri).toList().toArray(new URI[]{});
    }
}
