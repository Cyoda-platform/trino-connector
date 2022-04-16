package com.cyoda.presto.reports;

import com.cyoda.presto.CyodaTable;

import java.util.List;

public interface ReportTableProvider {
    List<CyodaTable> createTableList();
}
