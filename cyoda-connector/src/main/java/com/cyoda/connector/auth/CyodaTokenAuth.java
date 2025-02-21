package com.cyoda.connector.auth;


import io.trino.spi.security.HeaderAuthenticator;

import java.security.Principal;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class CyodaTokenAuth extends RestAuthenticator implements HeaderAuthenticator {
    private static final String ACCESS_TOKEN = "access_token";


    private final String testTokenUri;

    public CyodaTokenAuth(Map<String, String> config) {
        super(config);
        this.testTokenUri = requireNonNull(config.get("cyoda.test-token.uri"), "Property cyoda.test-token.uri is required in header-authenticator.properties");
    }


    @Override
    public Principal createAuthenticatedPrincipal(Headers headers) {
        String userToken = headers.getHeader(ACCESS_TOKEN).getFirst();
        return parseProvidedToken(authRestTemplate, testTokenUri, userToken);
    }


}
