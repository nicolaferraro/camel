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
package org.apache.camel.component.spring.web.rest;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.spring.web.config.TestAutoConfiguration;
import org.apache.camel.model.rest.RestBindingMode;
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
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootApplication
@DirtiesContext
@ContextConfiguration(classes = {TestAutoConfiguration.class, CamelAutoConfiguration.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestBindingModeAutoTest {

    @Value("${local.server.port}")
    protected Integer port;

    @Autowired
    ProducerTemplate template;

    @EndpointInject(uri = "mock:json")
    MockEndpoint mockJsonEndpoint;

    @EndpointInject(uri = "mock:xml")
    MockEndpoint mockXmlEndpoint;

    @Test
    public void testJsonConversion() throws Exception {
        mockJsonEndpoint.expectedMessageCount(1);
        mockJsonEndpoint.message(0).body().isInstanceOf(UserPojo.class);

        String body = "{\"id\": 123, \"name\": \"Json Donald Duck\"}";

        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put(Exchange.HTTP_METHOD, "POST");
        template.requestBodyAndHeaders("http://localhost:" + port + "/users/new", new ByteArrayInputStream(body.getBytes()), headers);

        mockJsonEndpoint.assertIsSatisfied();

        UserPojo user = mockJsonEndpoint.getReceivedExchanges().get(0).getIn().getBody(UserPojo.class);
        assertNotNull(user);
        assertEquals(123, user.getId());
        assertEquals("Json Donald Duck", user.getName());
    }

    @Test
    public void testXmlConversion() throws Exception {
        mockXmlEndpoint.expectedMessageCount(1);
        mockXmlEndpoint.message(0).body().isInstanceOf(UserPojo.class);

        String body = "<user name=\"Xml Donald Duck\" id=\"1234\"></user>";

        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/xml");
        headers.put(Exchange.HTTP_METHOD, "POST");
        template.requestBodyAndHeaders("http://localhost:" + port + "/users/new", new ByteArrayInputStream(body.getBytes()), headers);

        mockXmlEndpoint.assertIsSatisfied();

        UserPojo user = mockXmlEndpoint.getReceivedExchanges().get(0).getIn().getBody(UserPojo.class);
        assertNotNull(user);
        assertEquals(1234, user.getId());
        assertEquals("Xml Donald Duck", user.getName());
    }

    @Configuration
    static class RestBindingConfig {

        @Bean
        RoutesBuilder route() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {

                    restConfiguration().bindingMode(RestBindingMode.auto);

                    rest("/users/")
                            .post("new").type(UserPojo.class)
                            .route()
                            .choice()
                            .when()
                                .body(UserPojo.class, u -> u.getName().toLowerCase().contains("json"))
                                .to("mock:json")
                            .otherwise()
                                .to("mock:xml")
                            .endChoice();
                }
            };
        }

    }

}
