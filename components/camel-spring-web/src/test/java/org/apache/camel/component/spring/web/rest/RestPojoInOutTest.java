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
public class RestPojoInOutTest {

    @Value("${local.server.port}")
    protected Integer port;

    @Autowired
    ProducerTemplate template;


    @Test
    public void testPojoInOut() throws Exception {
        String body = "{\"id\": 1234, \"name\": \"Donald Duck\"}";

        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put(Exchange.HTTP_METHOD, "POST");
        String out = template.requestBodyAndHeaders("http://localhost:" + port + "/users/lives", new ByteArrayInputStream(body.getBytes()), headers, String.class);

        assertNotNull(out);
        assertEquals("{\"iso\":\"SE\",\"country\":\"Sweden\"}", out);
    }
    
    @Test
    public void testPojoGet() throws Exception {
        String body = "{\"id\": 1234, \"name\": \"Donald Duck\"}";

        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put(Exchange.HTTP_METHOD, "GET");
        String out = template.requestBodyAndHeaders("http://localhost:" + port + "/users/lives", new ByteArrayInputStream(body.getBytes()), headers, String.class);

        assertNotNull(out);
        assertEquals("{\"iso\":\"EN\",\"country\":\"England\"}", out);
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
                            // just return the default country here
                            .get("lives").to("direct:start")
                            .post("lives").type(UserPojo.class).outType(CountryPojo.class)
                            .route()
                            .bean(new UserService(), "livesWhere");

                    CountryPojo country = new CountryPojo();
                    country.setIso("EN");
                    country.setCountry("England");
                    from("direct:start").transform().constant(country);

                }
            };
        }

    }

}
