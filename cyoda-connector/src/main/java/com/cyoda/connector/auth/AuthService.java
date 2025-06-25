package com.cyoda.connector.auth;

import com.cyoda.connector.CyodaConfig;
import io.trino.spi.connector.ConnectorSession;

import javax.annotation.Nonnull;

import io.trino.spi.security.AccessDeniedException;
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

        AuthContext authContext = authorizationHandler.getAuthContext(principal.toString());
        if (authContext == null) {
            if (config.isTestingMode()) {
                return new AuthContext(principal.toString()); // test mode access with userId as username
            } else throw new AccessDeniedException("Principal "+ principal +" never authorized");
        } else return authContext;
    }
}
