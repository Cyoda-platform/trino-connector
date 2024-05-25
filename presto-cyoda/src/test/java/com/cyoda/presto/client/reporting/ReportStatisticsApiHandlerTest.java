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

package com.cyoda.presto.client.reporting;

import com.google.common.collect.ImmutableMap;
import org.springframework.hateoas.UriTemplate;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHttp.REPORT_ENDPOINT;
import static org.testng.Assert.assertEquals;

public class ReportStatisticsApiHandlerTest {

    @Test
    public void testUriTemplate() throws MalformedURLException, URISyntaxException {
        String path = "/{id}/{grouping_version}/stats{?full}";
        String url = "https://demo.cyoda.com";
        String myId = "myId";
        String groupingVersion = "groupingVersion";
        String expected = url+REPORT_ENDPOINT+"/"+myId+"/"+groupingVersion+"/stats?full=false";

        URI uri = new URL(url).toURI().resolve(REPORT_ENDPOINT);
        UriTemplate template = UriTemplate.of(uri.toASCIIString()+path);

        URI expand = template.expand(
                ImmutableMap.of(
                        "id", myId,
                        "grouping_version", groupingVersion,
                        "full", "false"
                )
        );
        assertEquals(expand.toString(),expected);

    }

}