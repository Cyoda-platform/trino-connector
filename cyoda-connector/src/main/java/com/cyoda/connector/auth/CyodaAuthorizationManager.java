package com.cyoda.connector.auth;

import com.cyoda.connector.client.AuthRestTemplate;
import com.cyoda.connector.logging.SupplierLogger;
import io.trino.spi.security.AccessDeniedException;
import io.trino.spi.security.BasicPrincipal;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CyodaAuthorizationManager {

    protected static final String UUID_PATTERN_STRING = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final Pattern USER_ID_PATTERN = Pattern.compile("userId=(" + UUID_PATTERN_STRING + ")");
    private static final String JWT_PARAM = "jwt";
    private static final Pattern EXPIRY_PATTERN = Pattern.compile("exp=(\\d+)");
    private static final Pattern SCOPES_PATTERN = Pattern.compile("scopes=\\[([^]]*)]");
    private static final SupplierLogger LOG = SupplierLogger.get(CyodaAuthorizationManager.class);

    private final Map<String, AbstractTimedAuth> authMap = new ConcurrentHashMap<>();

    public Principal authToken(AuthRestTemplate restTemplate, String tokenUrl, String token) {
        TokenTimedAuth timedAuth = new TokenTimedAuth(restTemplate, tokenUrl);
        Principal principal = timedAuth.authenticate(token);
        authMap.put(principal.toString(), timedAuth);
        return principal;
    }

    public Principal authPassword(AuthRestTemplate restTemplate, URI loginUri, String tokenUri, String user, String password) {
        PasswordTimedAuth timedAuth = new PasswordTimedAuth(restTemplate, loginUri, tokenUri, user, password);
        Principal principal = timedAuth.authenticate();
        authMap.put(principal.toString(), timedAuth);
        return principal;
    }

    public TimedAuth verifyAuth(String principal) {
        AbstractTimedAuth auth = authMap.get(principal);
        if (auth != null) {
            if (auth.isExpired())
                auth.reAuthenticate();
            return auth;
        } else
            throw new AccessDeniedException("Principal "+ principal +" never authorized");
    }
    public AuthContext getAuthContext(String principal) {
        AbstractTimedAuth auth = authMap.get(principal);
        if (auth != null) {
            return new AuthContext(auth.getUserInfo().userId());
        } else return null;
    }

    private class PasswordTimedAuth extends AbstractTimedAuth {
        private final URI loginUri;
        private final String username;
        private final String password;

        private PasswordTimedAuth(AuthRestTemplate restTemplate, URI loginUri, String tokenUri, String user, String password) {
            super(restTemplate, tokenUri);
            this.loginUri = loginUri;
            this.username = user;
            this.password = password;
        }

        protected BasicPrincipal authenticate() {
            String token = passwordAuth(restTemplate, loginUri, username, password);
            return authenticate(token);
        }

        @Override
        public void reAuthenticate() {
            authenticate();
        }

        @Override
        public boolean hasUserName(String userName) {
            return userName.equals(username);
        }

        private static @NotNull String passwordAuth(AuthRestTemplate authRestTemplate, URI loginUri, String user, String password) {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "client_credentials");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Add Basic Authentication header with client_id:client_secret
            String credentials = user + ":" + password;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
            headers.add("Authorization", "Basic " + encodedCredentials);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);
            ResponseEntity<OAuth2TokenResponse> response =
                    authRestTemplate.getRestTemplate()
                            .exchange(loginUri, HttpMethod.POST, requestEntity, OAuth2TokenResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().token;
            } else {
                LOG.warn("access denied to " + user + " with reason: " + response);
                throw new AccessDeniedException("Unauthorized");
            }
        }
    }

    private class TokenTimedAuth extends AbstractTimedAuth {
        private TokenTimedAuth(AuthRestTemplate restTemplate, String tokenUri) {
            super(restTemplate, tokenUri);
        }

        @Override
        public void reAuthenticate() {
            throw new AccessDeniedException("Token expired.");
        }

        @Override
        public boolean hasUserName(String userName) {
            return true;
        }

    }


    private static abstract class AbstractTimedAuth implements TimedAuth {
        private final BasicPrincipal principal;
        protected final AuthRestTemplate restTemplate;
        protected final String tokenUri;
        protected Date expiryDate;
        private UserInfo userInfo;

        private AbstractTimedAuth(AuthRestTemplate restTemplate, String tokenUri) {
            principal = new BasicPrincipal(UUID.randomUUID().toString());
            this.restTemplate = restTemplate;
            this.tokenUri = tokenUri;
        }

        @Override
        public boolean isExpired() {
            if (expiryDate == null) {
                LOG.warn("Token expiry date is null");
                return false;
            } else
                return expiryDate.before(new Date());
        }

        @Override
        public BasicPrincipal authenticate(String token) {
            ParsedToken parsed = parseProvidedToken(restTemplate, tokenUri, token);
            expiryDate = parsed.expiryDate;
            userInfo = new UserInfo(parsed.userId, parsed.roles.contains("SUPER_USER") || parsed.roles.contains("ADMIN"));
            return principal;
        }

        @Override
        public UserInfo getUserInfo() {
            return userInfo;
        }

        protected static @NotNull ParsedToken parseProvidedToken(AuthRestTemplate authRestTemplate, String testTokenUri, String userToken) {
            URI tokenUriWithParam = UriComponentsBuilder.fromUriString(testTokenUri)
                    .queryParam(JWT_PARAM, userToken).build().toUri();
            HttpHeaders sendHeader = RestAuthenticator.standardHeader();
            sendHeader.add("Authorization", "Bearer " + userToken);
            HttpEntity<?> requestEntity = new HttpEntity<>(sendHeader);
            ResponseEntity<String> response =
                    authRestTemplate.getRestTemplate()
                            .exchange(tokenUriWithParam, HttpMethod.GET, requestEntity, String.class);
            String responseBody = response.getBody();
            if ( response.getStatusCode().is2xxSuccessful() && responseBody != null) {
                Matcher userIdMatcher = USER_ID_PATTERN.matcher(responseBody);
                Matcher expiryMatcher = EXPIRY_PATTERN.matcher(responseBody);
                Matcher scopeMatcher = SCOPES_PATTERN.matcher(responseBody);
                if (userIdMatcher.find()) {
                    String userId = userIdMatcher.group(1);
                    Date expiryDate = expiryMatcher.find() ? new Date(Long.parseLong(expiryMatcher.group(1))*1000) : null;
                    Set<String> scopes = scopeMatcher.find() ?
                            Arrays.stream(scopeMatcher.group(1).split(","))
                                    .map(String::trim).collect(Collectors.toSet()) : null;
                    return new ParsedToken(userId, scopes, expiryDate);
                } else {
                    LOG.warn("Could not extract user ID from the parsed token: "+ responseBody);
                    throw new AccessDeniedException("Unauthorized");
                }
            } else {
                LOG.warn("access denied to given token with reason: "+response);
                throw new AccessDeniedException("Unauthorized");
            }
        }
    }

    private record ParsedToken(String userId, Set<String> roles, Date expiryDate){}
    public record UserInfo(String userId, boolean isAdmin){}
}
