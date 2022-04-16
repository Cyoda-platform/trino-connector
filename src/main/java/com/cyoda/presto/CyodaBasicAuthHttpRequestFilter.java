package com.cyoda.presto;

import com.facebook.airlift.http.client.BasicAuthRequestFilter;
import com.facebook.airlift.http.client.HttpRequestFilter;
import com.facebook.airlift.http.client.Request;

import javax.inject.Inject;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public class CyodaBasicAuthHttpRequestFilter implements HttpRequestFilter {
    private final BasicAuthRequestFilter filter;

    @Inject
    public CyodaBasicAuthHttpRequestFilter(CyodaConfig config)
    {
        Optional<String> username = config.getBasicAuthenticationUsername();
        Optional<String> password = config.getBasicAuthenticationPassword();
        boolean credentialsDefined = ( username.isPresent() && password.isPresent() );
        if ( credentialsDefined ) {
            this.filter = new BasicAuthRequestFilter(username.get(), password.get());
        }
        throw new IllegalArgumentException("the CyodaConfig passed does not have the username/password defined for basic authentication");
    }

    @Override
    public Request filterRequest(Request request)
    {
        return filter.filterRequest(request);
    }
}
