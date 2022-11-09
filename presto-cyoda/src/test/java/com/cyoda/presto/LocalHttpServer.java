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

import io.airlift.bootstrap.Bootstrap;
import io.airlift.bootstrap.LifeCycleManager;
import io.airlift.http.server.TheServlet;
import io.airlift.http.server.testing.TestingHttpServerModule;
import io.airlift.node.testing.TestingNodeModule;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class LocalHttpServer {
    private final LifeCycleManager lifeCycleManager;
    private final URI baseUri;
    private final LocalHttpServerModule ourHttpServerModule;

    public LocalHttpServer() {
        this.ourHttpServerModule = new LocalHttpServerModule();
        Bootstrap app = new Bootstrap(
                new TestingNodeModule(),
                new TestingHttpServerModule(),
                ourHttpServerModule);

        Injector injector = app
                .doNotInitializeLogging()
                .initialize();

        lifeCycleManager = injector.getInstance(LifeCycleManager.class);
        baseUri = injector.getInstance(io.airlift.http.server.testing.TestingHttpServer.class).getBaseUrl();
    }

    public void setResponseMapperProvider(ResponseMapperProvider provider) {
        ourHttpServerModule.setResponseMapperProvider(provider);
    }

    public void stop() {
        lifeCycleManager.stop();
    }

    public URI resolve(String s) {
        return baseUri.resolve(s);
    }

    public URI getBaseUri() {
        return baseUri;
    }

    private static class LocalHttpServerModule
            implements Module {
        private final OurHttpServlet instance = new OurHttpServlet();

        public LocalHttpServerModule() {
        }

        public void setResponseMapperProvider(ResponseMapperProvider provider) {
            instance.setResponseMapperProvider(provider);
        }

        @Override
        public void configure(Binder binder) {
            binder.bind(new TypeLiteral<Map<String, String>>() {
            }).annotatedWith(TheServlet.class).toInstance(ImmutableMap.of());
            binder.bind(Servlet.class).annotatedWith(TheServlet.class).toInstance(instance);
        }
    }

    private static class OurHttpServlet
            extends HttpServlet {
        private ResponseMapperProvider mapperProvider;

        public OurHttpServlet() {
        }

        public void setResponseMapperProvider(ResponseMapperProvider provider) {
            this.mapperProvider = provider;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws IOException {
            Objects.requireNonNull(mapperProvider);
            mapperProvider.doGet(request, response);

        }

    }
}
