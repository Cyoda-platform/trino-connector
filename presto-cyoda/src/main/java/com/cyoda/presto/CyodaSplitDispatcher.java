package com.cyoda.presto;

import com.cyoda.presto.logging.SupplierLogger;
import io.trino.metadata.InternalNode;
import io.trino.metadata.InternalNodeManager;
import io.trino.metadata.NodeState;
import io.trino.spi.HostAddress;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CyodaSplitDispatcher {

    protected static final SupplierLogger LOG = SupplierLogger.get(CyodaSplitDispatcher.class);

    private final InternalNodeManager internalNodeManager;

    private URI[] nodeBuckets;

    public CyodaSplitDispatcher(InternalNodeManager internalNodeManager) {
        this.internalNodeManager = internalNodeManager;
        setupBuckets();
        internalNodeManager.addNodeChangeListener(allNodes -> {
            LOG.warn("Node list changed. Restructuring cache buckets");
            setupBuckets();
        });
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
        nodeBuckets = internalNodeManager.getNodes(NodeState.ACTIVE).stream().map(InternalNode::getInternalUri).toList().toArray(new URI[]{});
    }
}
