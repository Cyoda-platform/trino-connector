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

package com.cyoda.api.deserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.joda.beans.Bean;

import java.io.IOException;

import static com.cyoda.core.util.JodaBeanSerUtil.compact;

public class JodaBeanJacksonDeserializer<T extends Bean> extends StdDeserializer<T> {

    /**
     * Need to introduce this, even though it is the same as _valueClass of the superclass.
     * But that is a {@literal Class<?>} and leads to warnings here in deserialize.
     */
    private final Class<T> clazz;

    public JodaBeanJacksonDeserializer(Class<T> vc) {
        super(vc);
        this.clazz = vc;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String json = p.readValueAsTree().toString();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(null);
            return compact().jsonReader().read(json,clazz);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }
}