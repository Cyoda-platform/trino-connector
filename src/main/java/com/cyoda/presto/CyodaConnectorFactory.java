package com.cyoda.presto;

import com.facebook.airlift.bootstrap.Bootstrap;
import com.facebook.airlift.json.JsonModule;
import com.facebook.presto.example.ExampleConnector;
import com.facebook.presto.example.ExampleModule;
import com.facebook.presto.spi.ConnectorHandleResolver;
import com.facebook.presto.spi.connector.Connector;
import com.facebook.presto.spi.connector.ConnectorContext;
import com.facebook.presto.spi.connector.ConnectorFactory;
import com.google.inject.Injector;

import java.util.Map;

import static com.google.common.base.Throwables.throwIfUnchecked;
import static java.util.Objects.requireNonNull;

public class CyodaConnectorFactory implements ConnectorFactory {
    public static final String CONNECTOR_FACTORY_NAME = "CyodaConnectorFactory";

    @Override
    public String getName() {
        return CONNECTOR_FACTORY_NAME;
    }

    @Override
    public ConnectorHandleResolver getHandleResolver() {
        return new CyodaHandleResolver();
    }

    @Override
    public Connector create(String catalogName, Map<String, String> requiredConfig, ConnectorContext context)
    {
        requireNonNull(requiredConfig, "requiredConfig is null");
        try {
            // A plugin is not required to use Guice; it is just very convenient
            Bootstrap app = new Bootstrap(
                    new JsonModule(),
                    new CyodaModule(catalogName, context.getTypeManager()));

            //noinspection UnstableApiUsage
            Injector injector = app
                    .doNotInitializeLogging()
                    .setRequiredConfigurationProperties(requiredConfig)
                    .initialize();

            return injector.getInstance(ExampleConnector.class);
        }
        catch (Exception e) {
            throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }}
