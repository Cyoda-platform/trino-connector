package com.cyoda.presto.client.reporting.stats;

import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.reporting.data.RowsRequestKey;
import com.cyoda.presto.logging.SupplierLogger;
import io.trino.spi.connector.Constraint;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class RowRequestStatsHandler {

    private static final SupplierLogger LOG = SupplierLogger.get(RowRequestStatsHandler.class);

    private final List<RowRequestStats> requestStats = new ArrayList<>();

    @Inject
    public RowRequestStatsHandler(){};

    public RowRequestStats registerCall(RowsRequestKey requestKey){
        RowRequestStats stats = new RowRequestStats(requestKey, new ArrayList<>());
        requestStats.add(stats);
        return stats;
    }
}
