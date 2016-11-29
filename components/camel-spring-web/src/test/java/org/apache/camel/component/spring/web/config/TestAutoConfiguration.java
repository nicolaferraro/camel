package org.apache.camel.component.spring.web.config;

import org.apache.camel.component.spring.web.CamelHandlerMapping;
import org.apache.camel.component.spring.web.SpringWebComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for all tests
 */
@Configuration
public class TestAutoConfiguration {

    @Bean("spring-web-component")
    public SpringWebComponent springWebComponent() {
        return new SpringWebComponent();
    }

    @Bean("camelHandlerMapping")
    public CamelHandlerMapping camelHandlerMapping(SpringWebComponent comp) {
        return new CamelHandlerMapping();
    }

}
