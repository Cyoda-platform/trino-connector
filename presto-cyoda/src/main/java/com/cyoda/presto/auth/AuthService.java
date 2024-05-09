package com.cyoda.presto.auth;

import com.cyoda.presto.CyodaConfig;
import io.trino.spi.connector.ConnectorSession;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.security.Principal;

public class AuthService {
    private final CyodaConfig config;
    private final AuthContext anonymousAuth;
    private final AuthContext technicalAuth;

    @Inject
    public AuthService(CyodaConfig config) {
        this.config = config;
        anonymousAuth = new AuthContextWithToken(
                config.getAnonymousUserId(),
                new AuthPayload(
                        null,
                        null,
                        null,
                        null,
                        config.getAnonymousToken(),
                        config.getAnonymousRefreshToken(),
                        config.getAnonymousUserName()
                )
        );
        //TODO for now it is just same anonymous auth
        technicalAuth = anonymousAuth;
    }


    public AuthContext getTechnicalAuth() {
        return technicalAuth;
    }

    public @Nonnull AuthContext fromSession(@Nonnull ConnectorSession session) {
        if ( config.isAnonymousLogin() ) return anonymousAuth;

        Principal principal = session.getIdentity().getPrincipal()
                .orElseThrow(() -> new IllegalArgumentException("principal is missing"));

        return new AuthContext(principal.getName());
    }
}
