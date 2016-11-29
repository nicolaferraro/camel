package org.apache.camel.component.spring.web;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
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


@RunWith(SpringRunner.class)
@SpringBootApplication
@DirtiesContext
@ContextConfiguration(classes = {TestAutoConfiguration.class, CamelAutoConfiguration.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestBindingTest {

    @Value("${local.server.port}")
    protected Integer port;

    @Autowired
    ProducerTemplate template;

    @Test
    public void testGet() throws Exception {
        String response = template.requestBodyAndHeader("http://localhost:" + port + "/hello", "Hello Camel!", Exchange.HTTP_METHOD, "GET", String.class);
        assertEquals("Get Camel", response);
    }

    @Test
    public void testPost() throws Exception {
        String response = template.requestBodyAndHeader("http://localhost:" + port + "/hello", "Hello Camel!", Exchange.HTTP_METHOD, "POST", String.class);
        assertEquals("Post Camel", response);
    }

    @Test
    public void testPut() throws Exception {
        String response = template.requestBodyAndHeader("http://localhost:" + port + "/hello", "Hello Camel!", Exchange.HTTP_METHOD, "PUT", String.class);
        assertEquals("Put Camel", response);
    }

    @Test
    public void testOneParameter() throws Exception {
        String response = template.requestBodyAndHeader("http://localhost:" + port + "/users/123/basic", "Hi !", Exchange.HTTP_METHOD, "GET", String.class);
        assertEquals("123;Donald Duck", response);
    }

    @Test
    public void testMultipleParameters() throws Exception {
        String response = template.requestBodyAndHeader("http://localhost:" + port + "/users/123/details/address", "Hi !", Exchange.HTTP_METHOD, "GET", String.class);
        assertEquals("123;address;5th Street", response);
    }

    @Test
    public void testQueryParams() throws Exception {
        String response = template.requestBodyAndHeader("http://localhost:" + port + "/users/?query1=age", "Hi !", Exchange.HTTP_METHOD, "GET", String.class);
        assertEquals("age", response);
    }


    @Configuration
    static class RestBindingConfig {

        @Bean
        RoutesBuilder route() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {

                    rest("/hello")
                            .get().route().transform().constant("Get Camel").endRest()
                            .post().route().transform().constant("Post Camel").endRest();

                    rest().put("/hello").produces("text/plain").route().transform().constant("Put Camel");

                    rest("/users/")
                            .get("{id}/basic")
                            .route()
                            .transform().simple("${header.id};Donald Duck");

                    rest("/users/{id}/details")
                            .get("/{detail}")
                            .route()
                            .transform().simple("${header.id};${header.detail};5th Street");

                    rest("/users")
                            .get()
                            .route()
                            .transform().simple("${header.query1}");

                }
            };
        }

    }

}

