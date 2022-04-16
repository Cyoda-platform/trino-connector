package com.cyoda.presto;

import com.cyoda.presto.CyodaClient.CyodaAuthenticationType;
import com.facebook.airlift.configuration.AbstractConfigurationAwareModule;
import com.google.inject.Binder;
import com.google.inject.Module;

import java.util.function.Predicate;

import static com.cyoda.presto.CyodaClient.CyodaAuthenticationType.NONE;
import static com.cyoda.presto.CyodaClient.CyodaAuthenticationType.BASIC;
import static com.cyoda.presto.CyodaClient.CyodaAuthenticationType.JWT;
import static com.facebook.airlift.configuration.ConditionalModule.installModuleIf;
import static com.facebook.airlift.http.client.HttpClientBinder.httpClientBinder;

@SuppressWarnings("UnstableApiUsage")
public class CyodaAuthenticationModule extends AbstractConfigurationAwareModule {
    @Override
    protected void setup(Binder binder)
    {
        bindAuthenticationModule(
                config -> config.getCyodaAuthenticationType() ==  NONE,
                noneAuthenticationModule());

        bindAuthenticationModule(
                config -> config.getCyodaAuthenticationType() == BASIC,
                basicAuthenticationModule());

        bindAuthenticationModule(
                config -> config.getCyodaAuthenticationType() == JWT,
                jwtAuthenticationModule());
    }

    private void bindAuthenticationModule(Predicate<CyodaConfig> predicate, Module module)
    {
        install(installModuleIf(CyodaConfig.class, predicate, module));
    }

    private static Module noneAuthenticationModule()
    {
        return binder -> httpClientBinder(binder).bindHttpClient("cyoda-client", ForCyodaClient.class);
    }

    private static Module basicAuthenticationModule()
    {
        return binder -> httpClientBinder(binder).bindHttpClient("cyoda-client", ForCyodaClient.class)
                .withConfigDefaults(
                        config -> config.setAuthenticationEnabled(false) //disable Kerberos auth
                ).withFilter(
                        CyodaBasicAuthHttpRequestFilter.class);
    }

    private static Module jwtAuthenticationModule()
    {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
