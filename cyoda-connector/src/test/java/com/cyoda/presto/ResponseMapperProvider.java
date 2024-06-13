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

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import org.springframework.hateoas.MediaTypes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public class ResponseMapperProvider {

    private ImmutableMap<String, ResponseMapper> pathInfoToMapper;

    public ResponseMapperProvider() {
    }

    public ResponseMapperProvider withResponseMapperProvider(ImmutableMap<String, ResponseMapper> pathInfoToMapper) {
        this.pathInfoToMapper = pathInfoToMapper;
        return this;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestURI = request.getRequestURI();

        ResponseMapper mapper = Optional.ofNullable(pathInfoToMapper)
                .orElseThrow(() -> new IllegalStateException("no mapper for " + request.getRequestURI()))
                .get(requestURI);
        String responseStr = mapper.resolveResponse().orElse("");
        ByteArrayInputStream ins = new ByteArrayInputStream(responseStr.getBytes(StandardCharsets.UTF_8));
        ByteStreams.copy(ins, response.getOutputStream());
        response.setContentType(MediaTypes.HAL_JSON.toString());
    }
}
