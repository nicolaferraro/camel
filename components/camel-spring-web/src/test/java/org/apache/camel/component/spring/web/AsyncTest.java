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

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.spring.web.config.TestAutoConfiguration;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(SpringRunner.class)
@SpringBootApplication
@DirtiesContext
@ContextConfiguration(classes = {TestAutoConfiguration.class, CamelAutoConfiguration.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AsyncTest {

    @Value("${local.server.port}")
    protected Integer port;

    @Autowired
    ProducerTemplate template;

    @EndpointInject(uri = "mock:rest")
    MockEndpoint mockRestEndpoint;

    @EndpointInject(uri = "mock:simple")
    MockEndpoint mockSimpleEndpoint;

    @Test
    public void testRestAsync() throws Exception {
        String response = template.requestBodyAndHeader("http://localhost:" + port + "/rest-endpoint", null, Exchange.HTTP_METHOD, "GET", String.class);
        assertEquals("1234", response);

        mockRestEndpoint.expectedMessageCount(1);
        mockRestEndpoint.assertIsSatisfied();
        assertTrue(((SpringWebEndpoint) mockRestEndpoint.getExchanges().get(0).getFromEndpoint()).isAsync());
    }

    @Test
    public void testSimpleAsync() throws Exception {
        String response = template.requestBodyAndHeader("http://localhost:" + port + "/simple-endpoint", null, Exchange.HTTP_METHOD, "GET", String.class);
        assertEquals("12345", response);

        mockRestEndpoint.expectedMessageCount(1);
        mockSimpleEndpoint.assertIsSatisfied();
        assertTrue(((SpringWebEndpoint) mockSimpleEndpoint.getExchanges().get(0).getFromEndpoint()).isAsync());
    }

    @Configuration
    static class AsyncTestConfig {

        @Bean
        RoutesBuilder route() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {

                    restConfiguration().endpointProperty("async", "true");
                    rest().get("/rest-endpoint").route().transform().constant("1234").to("mock:rest");

                    from("spring-web:/simple-endpoint?async=true").transform().constant("12345").to("mock:simple");

                }
            };
        }

    }

}