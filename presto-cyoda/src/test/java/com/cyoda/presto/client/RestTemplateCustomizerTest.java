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

package com.cyoda.presto.client;

import com.cyoda.core.model.reports.DistributedReportInfoView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

public class RestTemplateCustomizerTest {

    @Test
    public void testAnnotation() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        DistributedReportInfoView view = DistributedReportInfoView.builder()
                .id("my ID")
                .build();


        String json = mapper.writeValueAsString(view);
        DistributedReportInfoView reloaded = mapper.readValue(json,DistributedReportInfoView.class);
        assertNotNull(json);
    }

}