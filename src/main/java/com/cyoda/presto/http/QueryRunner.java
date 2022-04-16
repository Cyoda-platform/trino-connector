package com.cyoda.presto.http;

import com.cyoda.presto.CyodaConfig;
import com.facebook.presto.client.SocketChannelSocketFactory;
import okhttp3.OkHttpClient;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import static com.facebook.presto.client.OkHttpUtil.*;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.TimeUnit.SECONDS;


/**
 * The extra stuff has been adopted from {@link com.facebook.presto.testing.QueryRunner}
 * Look there, for example, if we ever want to support Kerberos authentication.
 */
@SuppressWarnings("UnstableApiUsage")
public class QueryRunner implements Closeable {

    private final OkHttpClient httpClient;

    public QueryRunner(CyodaConfig config) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.socketFactory(new SocketChannelSocketFactory());

        setupTimeouts(builder, 30, SECONDS);
        setupCookieJar(builder);
        setupSocksProxy(builder, config.getSocksProxy());
        setupHttpProxy(builder, config.getHttpProxy());
        setupBasicAuth(builder, config.getServerUrl(), config.getBasicAuthenticationUsername(), config.getBasicAuthenticationPassword());
        setupTokenAuth(builder, config.getServerUrl(), config.getAccessToken());

        this.httpClient =  builder.build();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void setupBasicAuth(
            OkHttpClient.Builder clientBuilder,
            URL url,
            Optional<String> user,
            Optional<String> password)
    {
        if (user.isPresent() && password.isPresent()) {
            checkArgument(url.getProtocol().equalsIgnoreCase("https"),
                    "Authentication using username/password requires HTTPS to be enabled");
            clientBuilder.addInterceptor(basicAuth(user.get(), password.get()));
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void setupTokenAuth(
            OkHttpClient.Builder clientBuilder,
            URL url,
            Optional<String> accessToken)
    {
        if (accessToken.isPresent()) {

            checkArgument(url.getProtocol().equalsIgnoreCase("https"),
                    "Authentication using an access token requires HTTPS to be enabled");
            clientBuilder.addInterceptor(tokenAuth(accessToken.get()));
        }
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public void close() throws IOException {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
