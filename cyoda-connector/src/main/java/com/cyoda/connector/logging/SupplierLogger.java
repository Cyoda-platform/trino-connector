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

package com.cyoda.connector.logging;


import java.util.Arrays;
import java.util.function.Supplier;

public class SupplierLogger {

    private final CyodaLogger logger;

    private SupplierLogger(CyodaLogger logger) {
        this.logger = logger;
    }

    public static SupplierLogger get(Class<?> clazz)
    {
        return get(clazz.getName());
    }
    public static SupplierLogger get(String name)
    {
        return new SupplierLogger(CyodaLogger.get(name));
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public void debug(String message)
    {
        logger.debug(message);
    }
    public void debug(Supplier<String> supplier) {
        if (logger.isDebugEnabled()) {
            logger.debug(supplier.get());
        }
    }
    public void debug(String format, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(format, args);
        }
    }
    public void debug(String format, Supplier<?>... input) {
        if (logger.isDebugEnabled()) {
            logger.debug(format, Arrays.stream(input).map(Supplier::get).toArray());
        }
    }
    public void debug(Throwable exception, String message)
    {
        logger.debug(exception, message);
    }
    public void debug(Throwable exception, String format, Object... args) {
        logger.debug(exception,format,args);
    }
    public void debug(Throwable exception,String format, Supplier<?>... input) {
        if (logger.isDebugEnabled()) {
            logger.debug(exception, format, Arrays.stream(input).map(Supplier::get).toArray());
        }
    }

    public void info(String message)
    {
        logger.info(message);
    }
    public void info(String format, Object... args) {
        logger.info(format,args);
    }
    public void info(String format, Supplier<?>... input) {
        if ( logger.isInfoEnabled() ) {
            logger.info(format, Arrays.stream(input).map(Supplier::get).toArray());
        }
    }

    public void warn(String message)
    {
        logger.warn(message);
    }
    public void warn(String format, Object... args) {
        logger.warn(format,args);
    }
    public void warn(Throwable exception, String message)
    {
        logger.warn(exception, message);
    }
    public void warn(Throwable exception, String format, Object... args) {
        logger.warn(exception,format,args);
    }

    public void error(Throwable exception, String message)
    {
        logger.error(exception, message);
    }
    public void error(String message)
    {
        logger.error(message);
    }
    public void error(Throwable exception)
    {
        logger.error(exception);
    }

}
