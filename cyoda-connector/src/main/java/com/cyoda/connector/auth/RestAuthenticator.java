package com.cyoda.connector.auth;

import com.cyoda.connector.client.AuthRestTemplate;
import com.cyoda.connector.logging.SupplierLogger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.Map;

public class RestAuthenticator {
    protected static final HttpHeaders HEADERS = RestAuthenticator.standardHeader();
    private static final SupplierLogger LOG = SupplierLogger.get(RestAuthenticator.class);
    static {
        HEADERS.add("X-Requested-With", "XMLHttpRequest");
    }

    protected final AuthRestTemplate authRestTemplate;

    public RestAuthenticator(Map<String, String> config) {
        this.authRestTemplate = new AuthRestTemplate(config);
    }

    static HttpHeaders standardHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
