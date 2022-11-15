/*
 * Copyright (C) 2022 Cyoda Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.cyoda.presto;

import com.cyoda.presto.auth.CyodaAuthenticatorFactory;
import com.cyoda.presto.client.types.BigDecimalDistinctFromOperator;
import com.cyoda.presto.client.types.BigDecimalOperators;
import com.cyoda.presto.client.types.BigDecimalType;
import com.cyoda.presto.client.types.BigIntegerDistinctFromOperator;
import com.cyoda.presto.client.types.BigIntegerOperators;
import com.cyoda.presto.client.types.BigIntegerType;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.Plugin;
import com.facebook.presto.spi.connector.ConnectorFactory;
import com.facebook.presto.spi.eventlistener.EventListenerFactory;
import com.facebook.presto.spi.security.PasswordAuthenticatorFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Set;

public class CyodaPlugin implements Plugin {
    @Override
    public Iterable<ConnectorFactory> getConnectorFactories() {
        return ImmutableList.of(new CyodaConnectorFactory());
    }

    @Override
    public Iterable<EventListenerFactory> getEventListenerFactories() {
        return ImmutableList.of(new CyodaEventListenerFactory());
    }

    private static final List<Type> OUR_TYPES = ImmutableList.<Type>builder()
            .add(BigDecimalType.BIG_DECIMAL_TYPE)
            .add(BigIntegerType.BIG_INTEGER_TYPE)
            .build();

    @Override
    public Iterable<Type> getTypes() {
        return OUR_TYPES;
    }

    private static final Set<Class<?>> OUR_FUNCTIONS = ImmutableSet.<Class<?>>builder()
            .add(BigDecimalOperators.class)
            .add(BigDecimalDistinctFromOperator.class)
            .add(BigIntegerOperators.class)
            .add(BigIntegerDistinctFromOperator.class)
            .build();

    @Override
    public Set<Class<?>> getFunctions() {
        return OUR_FUNCTIONS;
    }

    @Override
    public Iterable<PasswordAuthenticatorFactory> getPasswordAuthenticatorFactories() {
        return ImmutableList.<PasswordAuthenticatorFactory>builder()
                .add(new CyodaAuthenticatorFactory())
                .build();
    }


}
