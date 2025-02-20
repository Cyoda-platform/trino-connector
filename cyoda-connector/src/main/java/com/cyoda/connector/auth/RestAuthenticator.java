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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestAuthenticator {
    protected static final HttpHeaders HEADERS = RestAuthenticator.standardHeader();
    private static final String JWT_PARAM = "jwt";
    private static final SupplierLogger LOG = SupplierLogger.get(RestAuthenticator.class);
    static {
        HEADERS.add("X-Requested-With", "XMLHttpRequest");
    }
    protected static final String UUID_PATTERN_STRING = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final Pattern USER_ID_PATTERN = Pattern.compile("userId=(" + UUID_PATTERN_STRING + ")");

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
    static @NotNull BasicPrincipal parseProvidedToken(AuthRestTemplate authRestTemplate, String testTokenUri, String userToken) {
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put(JWT_PARAM, userToken);
        HttpHeaders sendHeader = RestAuthenticator.standardHeader();
        sendHeader.add("X-Requested-With", "XMLHttpRequest");
        sendHeader.add("Authorization", "Bearer " + userToken);
        HttpEntity<?> requestEntity = new HttpEntity<>(sendHeader);
        ResponseEntity<String> response =
                authRestTemplate.getRestTemplate()
                        .exchange(testTokenUri, HttpMethod.GET, requestEntity, String.class, uriParams);
        if ( response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Matcher matcher = USER_ID_PATTERN.matcher(response.getBody());
            if (matcher.find()) {
                return new BasicPrincipal(matcher.group(1));
            } else {
                LOG.warn("Could not extract user ID from the parsed token: "+response.getBody());
                throw new AccessDeniedException("Unauthorized");
            }
        } else {
            LOG.warn("access denied to given token with reason: "+response.toString());
            throw new AccessDeniedException("Unauthorized");
        }
    }
}
