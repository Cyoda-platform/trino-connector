package com.cyoda.presto;

import com.facebook.presto.common.type.Type;
import com.facebook.presto.common.type.TypeManager;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;

import javax.inject.Inject;

import static com.facebook.airlift.configuration.ConfigBinder.configBinder;
import static com.facebook.airlift.json.JsonBinder.jsonBinder;
import static com.facebook.airlift.json.JsonCodec.listJsonCodec;
import static com.facebook.airlift.json.JsonCodecBinder.jsonCodecBinder;
import static com.facebook.presto.common.type.TypeSignature.parseTypeSignature;
import static java.util.Objects.requireNonNull;

public class CyodaModule implements Module {
    private final String connectorId;
    private final TypeManager typeManager;


    public CyodaModule(String connectorId, TypeManager typeManager) {
        this.connectorId = requireNonNull(connectorId, "connector id is null");
        this.typeManager = requireNonNull(typeManager, "typeManager is null");
    }

    @Override
    public void configure(Binder binder)
    {
        binder.bind(TypeManager.class).toInstance(typeManager);

        binder.bind(CyodaConnector.class).in(Scopes.SINGLETON);
        binder.bind(CyodaConnectorId.class).toInstance(new CyodaConnectorId(connectorId));
        binder.bind(CyodaMetadata.class).in(Scopes.SINGLETON);
        binder.bind(CyodaClient.class).in(Scopes.SINGLETON);
        binder.bind(CyodaSplitManager.class).in(Scopes.SINGLETON);
        binder.bind(CyodaRecordSetProvider.class).in(Scopes.SINGLETON);
        configBinder(binder).bindConfig(CyodaConfig.class);

        jsonBinder(binder).addDeserializerBinding(Type.class).to(CyodaModule.TypeDeserializer.class);
        jsonCodecBinder(binder).bindMapJsonCodec(String.class, listJsonCodec(CyodaTable.class));
    }

    public static final class TypeDeserializer
            extends FromStringDeserializer<Type>
    {
        private final TypeManager typeManager;

        @Inject
        public TypeDeserializer(TypeManager typeManager)
        {
            super(Type.class);
            this.typeManager = requireNonNull(typeManager, "typeManager is null");
        }

        @Override
        protected Type _deserialize(String value, DeserializationContext context)
        {
            return typeManager.getType(parseTypeSignature(value));
        }
    }
}
