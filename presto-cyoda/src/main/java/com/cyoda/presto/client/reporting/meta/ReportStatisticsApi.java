package com.cyoda.presto.client.reporting.meta;

import com.cyoda.core.reports.DistributedReportInfoDto;
import com.cyoda.presto.client.reporting.FluxApiHandler;

public interface ReportStatisticsApi extends FluxApiHandler<ReportConfigKey, DistributedReportInfoDto> {
}
