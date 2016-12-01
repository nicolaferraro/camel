package org.apache.camel.example.spring.boot.web;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class SampleRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        restConfiguration().apiContextPath("/api-docs").apiContextListing(true).enableCORS(true);

        rest().get("/rest").description("Hello").produces("text/plain").route().transform().constant("Hello");


        from("spring-web:/simple").transform().constant("Hello 2");

    }
}
