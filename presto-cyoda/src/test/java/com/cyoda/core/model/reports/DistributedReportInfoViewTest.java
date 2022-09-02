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
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class DistributedReportInfoViewTest {

    @Test
    public void testDeserialization() throws JsonProcessingException {
        String json = "{\"id\":\"000186be-0000-1000-8080-808080808080-Book-b83fae40-2a1e-11ed-9bb9-901b0e8fc197\",\"createTime\":\"2022-09-01T17:51:35.068+00:00\",\"finishTime\":\"2022-09-01T17:51:36.337+00:00\",\"reportFailed\":false,\"secondPhaseFinished\":true,\"groupsCount\":1,\"totalRowsCount\":2000,\"markedAsCancelled\":false,\"rowsCountForFinishedShards\":{\"22\":62,\"23\":73,\"24\":69,\"25\":70,\"26\":70,\"27\":71,\"28\":60,\"29\":71,\"30\":68,\"10\":76,\"11\":65,\"12\":66,\"13\":69,\"14\":70,\"15\":71,\"16\":68,\"17\":58,\"18\":55,\"19\":55,\"1\":65,\"2\":71,\"3\":78,\"4\":76,\"5\":64,\"6\":67,\"7\":69,\"8\":65,\"9\":68,\"20\":56,\"21\":54},\"reportFailedShards\":{},\"userName\":\"paul.schleger@cyoda.com\",\"userId\":\"000186be-0000-1000-8080-808080808080\",\"configName\":\"PLAY-Book-DEFAULT\",\"gridConfigId\":null,\"version\":null,\"pointTime\":\"2199-12-31T23:00:00.000+00:00\",\"valuationPointTime\":\"2022-08-19T19:04:55.480+00:00\",\"description\":null,\"groupingVersion\":\"00000000-0000-1000-0000-000000000000\",\"hierarchy\":false,\"regroupingPossible\":true,\"groupingCols\":{\"data\":[]}}";
        ObjectMapper mapper = new ObjectMapper();
        //mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        DistributedReportInfoView infoView = mapper.readValue(json, DistributedReportInfoView.class);
        assertNotNull(infoView);
    }

    @Test
    public void testRestTemplate() throws MalformedURLException {
        CyodaConfig config = new CyodaConfig();
        config.setServerUrl(new URL("http://localhost"));
        RestTemplateCustomizer customizer = new RestTemplateCustomizer(config);
        AuthContext authContext = mock(AuthContext.class);
        RestTemplate restTemplate = customizer.getRestTemplate(authContext);
        MappingJackson2HttpMessageConverter converter = (MappingJackson2HttpMessageConverter) restTemplate.getMessageConverters().stream().filter(it -> it instanceof MappingJackson2HttpMessageConverter).findAny()
                .orElseThrow(() -> new RuntimeException("Cannot find converter"));
        converter.getObjectMapper().enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
    }
}