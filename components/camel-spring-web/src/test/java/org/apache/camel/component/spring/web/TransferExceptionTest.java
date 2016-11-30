/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.spring.web;

import java.io.ByteArrayInputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.spring.web.config.TestAutoConfiguration;
import org.apache.camel.http.common.HttpConstants;
import org.apache.camel.http.common.HttpHelper;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.apache.camel.test.junit4.TestSupport.assertIsInstanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootApplication
@DirtiesContext
@ContextConfiguration(classes = {TestAutoConfiguration.class, CamelAutoConfiguration.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TransferExceptionTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    CamelContext context;

    @Test
    public void testTransferException() throws Exception {
        ResponseEntity<byte[]> response = restTemplate.postForEntity("/hello", "", byte[].class);

        assertEquals(500, response.getStatusCodeValue());
        assertEquals(HttpConstants.CONTENT_TYPE_JAVA_SERIALIZED_OBJECT, response.getHeaders().getFirst("Content-Type"));
        Object object = HttpHelper.deserializeJavaObjectFromStream(new ByteArrayInputStream(response.getBody()), context);
        assertNotNull(object);

        IllegalArgumentException cause = assertIsInstanceOf(IllegalArgumentException.class, object);
        assertEquals("Damn", cause.getMessage());
    }


    @Configuration
    static class RouteConfig {

        @Bean
        RoutesBuilder route() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("spring-web:hello?transferException=true")
                            .throwException(new IllegalArgumentException("Damn"));
                }
            };
        }

    }

}
