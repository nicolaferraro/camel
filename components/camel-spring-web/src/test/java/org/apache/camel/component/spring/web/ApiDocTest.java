package org.apache.camel.component.spring.web;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.spring.web.config.TestAutoConfiguration;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootApplication
@DirtiesContext
@ContextConfiguration(classes = {TestAutoConfiguration.class, CamelAutoConfiguration.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApiDocTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    public void testCatalog() throws Exception {
        ResponseEntity<List> catalog = restTemplate.getForEntity("/api-docs", List.class);
        assertEquals(200, catalog.getStatusCodeValue());
        assertEquals(1, catalog.getBody().size());

        String contextName = ((Map<String, String>) catalog.getBody().get(0)).get("name");
        assertNotNull(contextName);

        ResponseEntity<Map> services = restTemplate.getForEntity("/api-docs/" + URLEncoder.encode(contextName, "UTF-8"), Map.class);
        Map paths = (Map) services.getBody().get("paths");
        Map rest = (Map) paths.get("/rest");
        Map ser = (Map) rest.get("get");

        assertEquals("Hello", ser.get("summary"));

        List<String> produces = (List<String>) ser.get("produces");
        assertTrue(produces.contains("text/plain"));
    }

    @Configuration
    static class RouteConfig {

        @Bean
        RoutesBuilder route() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {

                    restConfiguration().apiContextPath("/api-docs").apiContextListing(true).enableCORS(true);

                    rest().get("/rest").description("Hello").produces("text/plain").route().transform().constant("Hello Camel!");

                }
            };
        }

    }

}
