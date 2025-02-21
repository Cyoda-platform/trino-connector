package com.cyoda.connector.auth;

import io.trino.spi.security.HeaderAuthenticator;
import io.trino.spi.security.HeaderAuthenticatorFactory;

import java.util.Map;

public class CyodaTokenAuthFactory implements HeaderAuthenticatorFactory {
    @Override
    public String getName() {
        return "token";
    }

    @Override
    public HeaderAuthenticator create(Map<String, String> config) {
        return new CyodaTokenAuth(config);
    }
}
