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
package com.cyoda.service.interactors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WrappedEntityModel<T> {
    private final T content;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    private WrappedEntityModel(@JsonProperty("content") T content) {
        this.content = content;
    }

    private void checkType(T content) {
        if ( ByteBuffer.class.isAssignableFrom(content.getClass())) {
            return;
        }
        SimpleTypeEnum type = CLASS_TO_SIMPLE_TYPE.get(content.getClass());
        Preconditions.checkArgument(type!=null,"Class %s is not a SimpleType", content.getClass().getName());
    }

    public @Nonnull T getContent() {
        return content;
    }

    public static <S> EntityModel<WrappedEntityModel<S>> wrapInEntityModel(@Nonnull S content, List<Link> links) {
        return EntityModel.of(new WrappedEntityModel<>(content),links);
    }

    public static <S> EntityModel<WrappedEntityModel<S>> wrapInEntityModel(@Nonnull S content, Link... links) {
        return wrapInEntityModel(content, Arrays.asList(links));
    }

    public static <S> EntityModel<WrappedEntityModel<S>> wrapInEntityModel(@Nonnull S content) {
        return wrapInEntityModel(content, Collections.emptyList());
    }

    private static final Map<Class<?>, SimpleTypeEnum> CLASS_TO_SIMPLE_TYPE =
            ImmutableMap.copyOf(Arrays.stream(SimpleTypeEnum.values()).collect(Collectors.toMap(SimpleTypeEnum::getClazz, Function.identity())));

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedEntityModel<?> that = (WrappedEntityModel<?>) o;
        return Objects.equal(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(content);
    }

    public enum SimpleTypeEnum {
        STRING(String.class),
        BYTE(Byte.class),
        BOOLEAN(Boolean.class),
        SHORT(Short.class),
        CHARACTER(Character.class),
        INTEGER(Integer.class),
        LONG(Long.class),
        FLOAT(Float.class),
        DOUBLE(Double.class),
        DATE(Date.class),
        BIG_INTEGER(BigInteger.class),
        BIG_DECIMAL(BigDecimal.class),
        BYTE_ARRAY(byte[].class),
        UUID(UUID.class),
        CLASS(Class.class),
        BYTE_BUFFER(ByteBuffer.class),
        LOCALE(Locale .class);

        private final Class<?> clazz;

        SimpleTypeEnum(Class<?> clazz) {
            this.clazz = clazz;
        }
        public Class<?> getClazz() {
            return clazz;
        }

    }

}