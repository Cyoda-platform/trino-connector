///*
// * Copyright (C) 2022 Cyoda Ltd.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// */
//
//package com.cyoda.connector.handles;
//
//import io.trino.spi.CatalogSchemaName;
//import io.trino.spi.function.FunctionHandle;
//import io.trino.spi.function.Signature;
//import com.fasterxml.jackson.annotation.JsonCreator;
//import com.fasterxml.jackson.annotation.JsonProperty;
//
//import java.util.Objects;
//
//import static java.util.Objects.requireNonNull;
//
////TODO Interface is gone. Need to find out if this logic is necessary
//public class CyodaFunctionHandle implements FunctionHandle
//{
//    private final Signature signature;
//
//    @JsonCreator
//    public CyodaFunctionHandle(@JsonProperty("signature") Signature signature)
//    {
//        this.signature = requireNonNull(signature, "signature is null");
//    }
//
//    @Override
//    public CatalogSchemaName getCatalogSchemaName()
//    {
//        return signature.getName().getCatalogSchemaName();
//    }
//
//    @JsonProperty
//    public Signature getSignature()
//    {
//        return signature;
//    }
//
//    @Override
//    public boolean equals(Object o)
//    {
//        if (this == o) {
//            return true;
//        }
//        if (o == null || getClass() != o.getClass()) {
//            return false;
//        }
//        CyodaFunctionHandle that = (CyodaFunctionHandle) o;
//        return Objects.equals(signature, that.signature);
//    }
//
//    @Override
//    public int hashCode()
//    {
//        return Objects.hash(signature);
//    }
//
//    @Override
//    public String toString()
//    {
//        return signature.toString();
//    }
//}
