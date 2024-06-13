package com.cyoda.connector.client;

import com.cyoda.connector.client.reporting.stats.CyodaApiRequestStatsMonitor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class RestResponseInterceptor implements ClientHttpRequestInterceptor {

    private final CyodaApiRequestStatsMonitor apiRequestStatsMonitor;

    public RestResponseInterceptor(CyodaApiRequestStatsMonitor apiRequestStatsMonitor) {
        this.apiRequestStatsMonitor = apiRequestStatsMonitor;
    }

    @Override
    public ClientHttpResponse intercept(@Nonnull HttpRequest request,
                                        @Nonnull byte[] reqBody,
                                        ClientHttpRequestExecution ex) throws IOException {
            ClientHttpResponse response = ex.execute(request, reqBody);
            InputStreamReader isr = new InputStreamReader(
                    response.getBody(), StandardCharsets.UTF_8);
            String body = new BufferedReader(isr).lines()
                    .collect(Collectors.joining("\n"));
            apiRequestStatsMonitor.addResponse(request.getURI().toString(), response.getStatusCode() + ":" + body);
            return response;
        }


}
