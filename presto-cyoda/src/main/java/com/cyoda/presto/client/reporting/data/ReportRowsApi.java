package com.cyoda.presto.client.reporting.data;

import com.cyoda.presto.CyodaSplit;

public interface ReportRowsApi {
    Iterable<RowHandle> getIterable(CyodaSplit split);
}
