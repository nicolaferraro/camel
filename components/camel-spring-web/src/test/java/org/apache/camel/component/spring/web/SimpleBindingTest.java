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
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootApplication
@DirtiesContext
@ContextConfiguration(classes = {TestAutoConfiguration.class, CamelAutoConfiguration.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SimpleBindingTest {

    @Value("${local.server.port}")
    protected Integer port;

    @Autowired
    ProducerTemplate template;

    @EndpointInject(uri = "mock:myapp")
    MockEndpoint myAppEndpoint;

    @EndpointInject(uri = "mock:myapp2")
    MockEndpoint myApp2Endpoint;

    @EndpointInject(uri = "mock:echo")
    MockEndpoint echoEndpoint;

    @Test
    public void testSimpleEndpoint() throws Exception {
        myAppEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        String response = template.requestBody("http://localhost:" + port + "/myapp", "Hello Camel!", String.class);
        assertNotNull(response);
        assertEquals("Bye Camel!", response);

        myAppEndpoint.assertIsSatisfied();

        for (Exchange exchange : myAppEndpoint.getExchanges()) {
            assertEquals("Bye Camel!", exchange.getIn().getBody(String.class));
        }
    }

    @Test
    public void testSimpleEndpoint2() throws Exception {
        myApp2Endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        String response = template.requestBody("http://localhost:" + port + "/myapp2", "Hello Camel!", String.class);
        assertNotNull(response);
        assertEquals("Set Body Camel!", response);

        myApp2Endpoint.assertIsSatisfied();

        for (Exchange exchange : myApp2Endpoint.getExchanges()) {
            assertEquals("Set Body Camel!", exchange.getIn().getBody(String.class));
        }
    }

    @Test
    public void testEchoEndpoint() throws Exception {
        echoEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        String response = template.requestBody("http://localhost:" + port + "/echo", "Hello Camel!", String.class);
        assertNotNull(response);
        assertEquals("Hello Camel!", response);

        echoEndpoint.assertIsSatisfied();

        for (Exchange exchange : echoEndpoint.getExchanges()) {
            assertEquals("Hello Camel!", exchange.getIn().getBody(String.class));
        }
    }


    @Configuration
    static class SimpleBindingConfig {

        @Bean
        RoutesBuilder route() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("spring-web:/myapp").transform().constant("Bye Camel!").to("mock:myapp");
                    from("spring-web:/myapp2").setBody().constant("Set Body Camel!").to("mock:myapp2");
                    from("spring-web:/echo").convertBodyTo(String.class).to("mock:echo");
                }
            };
        }

    }

}