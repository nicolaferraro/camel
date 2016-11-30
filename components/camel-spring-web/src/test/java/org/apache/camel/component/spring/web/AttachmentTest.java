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

import java.nio.charset.Charset;

import org.apache.camel.Attachment;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootApplication
@DirtiesContext
@ContextConfiguration(classes = {TestAutoConfiguration.class, CamelAutoConfiguration.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AttachmentTest {

    @Value("${local.server.port}")
    protected Integer port;

    @Autowired
    ProducerTemplate template;

    @EndpointInject(uri = "mock:file")
    MockEndpoint fileEndpoint;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    public void testAttachment() throws Exception {
        String textIn = "Some text in the file";
        MockMultipartFile textFile = new MockMultipartFile("text", "filename.txt", "text/plain", textIn.getBytes("UTF-8"));

        String jsonIn = "{\"result\": \"OK\"}";
        MockMultipartFile jsonFile = new MockMultipartFile("json", "", "application/json", jsonIn.getBytes("UTF-8"));

        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mockMvc.perform(MockMvcRequestBuilders.fileUpload("/upload")
                .file(textFile)
                .file(jsonFile)
                .param("anotherParam", "anotherValue"))
                .andExpect(status().is(200))
                .andExpect(content().string("success"));


        fileEndpoint.expectedMessageCount(1);
        fileEndpoint.expectedHeaderReceived("anotherParam", "anotherValue");

        fileEndpoint.assertIsSatisfied();

        Exchange exchange = fileEndpoint.getReceivedExchanges().get(0);

        Attachment textAttachment = exchange.getIn().getAttachmentObject("text");
        assertNotNull(textAttachment);
        assertEquals("text/plain", textAttachment.getHeader("Content-Type"));
        String textOut = StreamUtils.copyToString(textAttachment.getDataHandler().getInputStream(), Charset.forName("UTF-8"));
        assertEquals(textIn, textOut);

        Attachment jsonAttachment = exchange.getIn().getAttachmentObject("json");
        assertNotNull(jsonAttachment);
        assertEquals("application/json", jsonAttachment.getHeader("Content-Type"));
        String jsonOut = StreamUtils.copyToString(jsonAttachment.getDataHandler().getInputStream(), Charset.forName("UTF-8"));
        assertEquals(jsonIn, jsonOut);
    }


    @Configuration
    static class AttachmentConfig {

        @Bean
        RoutesBuilder route() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {

                    from("spring-web:/upload").setBody().constant("success").to("mock:file");

                }
            };
        }

    }

}