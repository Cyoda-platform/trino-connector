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

package com.cyoda.core.model.reports;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.auth.AuthContextWithToken;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.mockito.Mockito.mock;

public class DistributedReportInfoViewTest {


    @Test
    public void testRestTemplate() throws MalformedURLException {
        CyodaConfig config = new CyodaConfig();
        config.setServerUrl(new URL("http://localhost"));

        RestTemplateCustomizer customizer = new RestTemplateCustomizer(config, null);
        AuthContext authContext = mock(AuthContextWithToken.class);
        RestTemplate restTemplate = customizer.getRestTemplate(authContext);
        MappingJackson2HttpMessageConverter converter = (MappingJackson2HttpMessageConverter) restTemplate.getMessageConverters().stream().filter(it -> it instanceof MappingJackson2HttpMessageConverter).findAny()
                .orElseThrow(() -> new RuntimeException("Cannot find converter"));
        converter.getObjectMapper().enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
    }
}