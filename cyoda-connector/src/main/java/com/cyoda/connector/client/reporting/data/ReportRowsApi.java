package com.cyoda.connector.client.reporting.data;

import com.cyoda.connector.CyodaSplit;

public interface ReportRowsApi {
    Iterable<RowHandle> getIterable(CyodaSplit split);
}
