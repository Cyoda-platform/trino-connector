package com.cyoda.connector.auth;

import io.trino.spi.security.BasicPrincipal;

public interface TimedAuth {
    void reAuthenticate();

    boolean hasUserName(String userName);

    boolean isExpired();

    BasicPrincipal authenticate(String token);

    CyodaAuthorizationManager.UserInfo getUserInfo();

    class DummyTimedAuth implements TimedAuth {
        private final String userId;
        public DummyTimedAuth(String userId) {
            this.userId = userId;
        }
        @Override
        public void reAuthenticate() {}

        @Override
        public boolean hasUserName(String userName) {
            return true;
        }
        @Override
        public boolean isExpired() {
            return false;
        }
        @Override
        public BasicPrincipal authenticate(String token) {
            return new BasicPrincipal(userId);
        }
        @Override
        public CyodaAuthorizationManager.UserInfo getUserInfo() {
            return new CyodaAuthorizationManager.UserInfo(userId, true);
        }
    }
}
