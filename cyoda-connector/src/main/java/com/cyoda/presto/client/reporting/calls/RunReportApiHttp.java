package com.cyoda.presto.client.reporting.calls;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.auth.AuthService;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.reporting.BaseReportsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigKey;
import com.cyoda.presto.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.presto.logging.SupplierLogger;
import com.google.common.collect.ImmutableList;
import io.trino.spi.StandardErrorCode;
import io.trino.spi.TrinoException;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

public class RunReportApiHttp extends BaseReportsApiHandler implements RunReportApi {

    public static final String RUN_REPORT_ENDPOINT = "/api/platform-api/reporting/pre";

    protected static final SupplierLogger LOG = SupplierLogger.get(RunReportApiHttp.class);

    private final UriTemplate uriTemplate;
    @Inject
    protected RunReportApiHttp(CyodaConfig config, RestTemplateCustomizer restTemplateCustomizer, AuthService authService, CyodaApiRequestStatsMonitor requestStatsMonitor) {
        super(config, restTemplateCustomizer, LOG, authService, requestStatsMonitor);
        uriTemplate = setupUriTemplate();
    }


    @Override
    public String runReport(String queryId, AuthContext authContext, ReportConfigKey reportConfigKey) {
        String reportConfigId = reportConfigKey.configId();
        Map<String, Object> expansion = Collections.singletonMap("gridConfig", reportConfigId);
        URI templatedUri = uriTemplate.expand(expansion);

        Date callDate = new Date();
        RestTemplate restTemplate = restTemplateCustomizer.getRestTemplate(authContext);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(templatedUri, HttpMethod.POST, HttpEntity.EMPTY, String.class);
            LOG.info("CALLed run report, response: " + response);
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, e, templatedUri);
        } finally {
            registerApiCall(reportConfigKey.queryId(), callDate, templatedUri.toString(), expansion);
        }
        return response.toString();
    }
    private UriTemplate setupUriTemplate() {

        URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(RUN_REPORT_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new TrinoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, e);
        }
        final ImmutableList.Builder<TemplateVariable> builder = ImmutableList.builder();
        builder.add(TemplateVariable.requestParameter("gridConfig"));
        builder.add();

        TemplateVariables vars = new TemplateVariables(builder.build());
        return UriTemplate.of(uri.toASCIIString()).with(vars);
    }
}
