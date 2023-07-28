package com.cyoda.presto.client.reporting.calls;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.auth.AuthService;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.reporting.BaseReportsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigKey;
import com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle;
import com.cyoda.presto.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.presto.logging.SupplierLogger;
import com.google.common.collect.ImmutableList;
import io.trino.spi.StandardErrorCode;
import io.trino.spi.TrinoException;
import io.trino.spi.type.TypeManager;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.reporting.meta.ReportDefinitionHandle.REPORT_ID_COLUMN;

public class RunReportApiHandler extends BaseReportsApiHandler {

    public static final String RUN_REPORT_ENDPOINT = "/api/platform-api/reporting/pre";

    protected static final SupplierLogger LOG = SupplierLogger.get(RunReportApiHandler.class);

    private final UriTemplate uriTemplate;
    @Inject
    protected RunReportApiHandler(CyodaConfig config, RestTemplateCustomizer restTemplateCustomizer, AuthService authService, CyodaApiRequestStatsMonitor requestStatsMonitor) {
        super(config, restTemplateCustomizer, LOG, authService, requestStatsMonitor);
        uriTemplate = setupUriTemplate();
    }


    public String runReport(AuthContext authContext, ReportConfigKey reportConfigKey) {
        String reportConfigId = reportConfigKey.configId();
        Map<String, Object> expansion = Collections.singletonMap("gridConfig", reportConfigId);
        URI templatedUri = uriTemplate.expand(expansion);

        Date callDate = new Date();
        Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
        traverson.setRestOperations(restTemplateCustomizer.getRestTemplate(authContext));
        ResponseEntity<String> response;
        try {
            response = traverson.follow().toEntity(String.class);
            LOG.info("CALLING run report, response: " + response);
            registerApiCall(reportConfigKey.queryId(), callDate, templatedUri.toString(), expansion);
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, "retrieveCollection", e, templatedUri);
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
        builder.add(TemplateVariable.pathVariable("gridConfig"));
        builder.add();

        TemplateVariables vars = new TemplateVariables(builder.build());
        return UriTemplate.of(uri.toASCIIString()).with(vars);
    }
}
