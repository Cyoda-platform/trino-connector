package com.cyoda.connector.auth;

import com.cyoda.connector.CyodaConfig;
import io.trino.spi.connector.ConnectorSession;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import java.security.Principal;

public class AuthService {
    private final CyodaConfig config;
    private final CyodaAuthorizationManager authorizationHandler;

    @Inject
    public AuthService(CyodaConfig config, CyodaAuthorizationManager authorizationHandler) {
        this.config = config;
        this.authorizationHandler = authorizationHandler;
    }


    public @Nonnull AuthContext fromSession(@Nonnull ConnectorSession session) {
        Principal principal = session.getIdentity().getPrincipal()
                .orElseThrow(() -> new IllegalArgumentException("principal is missing"));

        return authorizationHandler.getAuthContext(principal.toString());
    }
}
