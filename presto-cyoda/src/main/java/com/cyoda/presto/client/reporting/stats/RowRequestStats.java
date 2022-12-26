package com.cyoda.presto.client.reporting.stats;

import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.reporting.data.RowsRequestKey;
import com.google.common.base.MoreObjects;

import java.util.List;

public record RowRequestStats(RowsRequestKey requestKey,
                              CompoundPredicateNode predicates,
                              List<RowPageRequestStats> pageRequests) {
    public void addPageRequest(RowPageRequestStats pageRequestStats){
        pageRequests.add(pageRequestStats);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("requestKey", requestKey)
                .add("predicates", predicates)
                .add("pages", pageRequests).toString();
    }
}
