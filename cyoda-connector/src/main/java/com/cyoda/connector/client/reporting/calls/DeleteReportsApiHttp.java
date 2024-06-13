package com.cyoda.connector.client.reporting.calls;

import com.cyoda.connector.CyodaConfig;
import com.cyoda.connector.auth.AuthContext;
import com.cyoda.connector.auth.AuthService;
import com.cyoda.connector.client.RestTemplateCustomizer;
import com.cyoda.connector.client.reporting.BaseReportsApiHandler;
import com.cyoda.connector.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.connector.logging.SupplierLogger;
import com.google.common.collect.ImmutableList;
import io.trino.spi.StandardErrorCode;
import io.trino.spi.TrinoException;
import io.trino.spi.connector.ConnectorSession;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DeleteReportsApiHttp extends BaseReportsApiHandler implements DeleteReportsApi {

    public static final String RUN_REPORT_ENDPOINT = "/api/platform-api/reporting/definitions/";

    protected static final SupplierLogger LOG = SupplierLogger.get(DeleteReportsApiHttp.class);

    private final UriTemplate uriTemplate;
    private final AuthService auth;
    @Inject
    protected DeleteReportsApiHttp(CyodaConfig config, RestTemplateCustomizer restTemplateCustomizer, AuthService authService, CyodaApiRequestStatsMonitor requestStatsMonitor, AuthService auth) {
        super(config, restTemplateCustomizer, LOG, authService, requestStatsMonitor);
        this.auth = auth;
        uriTemplate = setupUriTemplate();
    }


    @Override
    public String deleteReports(ConnectorSession session, String configId) {
        AuthContext authContext = auth.fromSession(session);
        Map<String, Object> expansion = new HashMap<>();
        expansion.put("configId", configId);
        expansion.put("mode", "reports");
        URI templatedUri = uriTemplate.expand(expansion);

        Date callDate = new Date();
        RestTemplate restTemplate = restTemplateCustomizer.getRestTemplate(authContext);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(templatedUri, HttpMethod.DELETE, HttpEntity.EMPTY, String.class);
            LOG.info("CALLed run report, response: " + response);
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, e, templatedUri);
        } finally {
            registerApiCall(session.getQueryId(), callDate, templatedUri.toString(), expansion);
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
        builder.add(TemplateVariable.pathVariable("configId"));
        builder.add(TemplateVariable.requestParameter("mode"));
        builder.add();

        TemplateVariables vars = new TemplateVariables(builder.build());
        return UriTemplate.of(uri.toASCIIString()).with(vars);
    }
}
