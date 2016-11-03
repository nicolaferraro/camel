package org.apache.camel.component.spring.web;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.http.common.HttpCommonEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;

/**
 *
 */
@UriEndpoint(scheme = "spring-web", extendsScheme = "http", title = "Spring Web", syntax = "spring-web:contextPath",
        consumerOnly = true, consumerClass = SpringWebConsumer.class, label = "http")
public class SpringWebEndpoint extends HttpCommonEndpoint {


    @UriPath(label = "consumer") @Metadata(required = "true")
    private String contextPath;

    public SpringWebEndpoint(String endPointURI, SpringWebComponent component, URI httpUri) throws URISyntaxException {
        super(endPointURI, component, httpUri);
        this.contextPath = httpUri.getPath();
    }

    @Override
    public Producer createProducer() throws Exception {
        throw new UnsupportedOperationException("You cannot create producer with the spring-web endpoint, please consider using the http or http4 endpoint.");
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return new SpringWebConsumer(this, processor);
    }

    /**
     * The context path
     */
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
}
