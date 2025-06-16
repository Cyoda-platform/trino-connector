package com.cyoda.connector.auth;


import io.trino.spi.security.HeaderAuthenticator;

import java.security.Principal;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class CyodaTokenAuth extends RestAuthenticator implements HeaderAuthenticator {
    private static final String ACCESS_TOKEN = "access_token";


    private final String testTokenUri;

    private final CyodaAuthorizationManager authHandler;

    public CyodaTokenAuth(Map<String, String> config, CyodaAuthorizationManager authHandler) {
        super(config);
        this.testTokenUri = requireNonNull(config.get("cyoda.test-token.uri"), "Property cyoda.test-token.uri is required in header-authenticator.properties");
        this.authHandler = authHandler;
    }


    @Override
    public Principal createAuthenticatedPrincipal(Headers headers) {
        String userToken = headers.getHeader(ACCESS_TOKEN).getFirst();
        return authHandler.authToken(authRestTemplate, testTokenUri, userToken);
    }


}
