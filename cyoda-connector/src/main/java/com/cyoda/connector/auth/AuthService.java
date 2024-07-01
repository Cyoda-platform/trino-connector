package com.cyoda.connector.auth;

import com.cyoda.connector.CyodaConfig;
import io.trino.spi.connector.ConnectorSession;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import java.security.Principal;

public class AuthService {
    private final CyodaConfig config;
    private final AuthContext anonymousAuth;

    @Inject
    public AuthService(CyodaConfig config) {
        this.config = config;
        anonymousAuth = new AuthContext(config.getAnonymousUserId());
    }


    public @Nonnull AuthContext fromSession(@Nonnull ConnectorSession session) {
        if ( config.isAnonymousLogin() ) return anonymousAuth;

        Principal principal = session.getIdentity().getPrincipal()
                .orElseThrow(() -> new IllegalArgumentException("principal is missing"));

        return new AuthContext(principal.getName());
    }
}
