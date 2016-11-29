package org.apache.camel.component.spring.web;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.http.common.CamelServlet;
import org.apache.camel.http.common.HttpConsumer;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;

/**
 * Adapts a Spring HttpRequestHandler to the Camel Servlet.
 */
public class CamelHttpRequestAdapter implements HttpRequestHandler {

    private Logger log = LoggerFactory.getLogger(getClass());

    private CamelServlet servletDelegate;

    public CamelHttpRequestAdapter(HttpConsumer consumer) {
        ObjectHelper.notNull(consumer, "consumer");

        log.debug("Creating servlet delegate for {}", consumer);
        this.servletDelegate = new CamelServlet();
        this.servletDelegate.setAsync(consumer.getEndpoint().isAsync());
        this.servletDelegate.setServletName("servletDelegate-" + consumer.getEndpoint().getEndpointUri());
        this.servletDelegate.connect(consumer);
        // Using a per-consumer strategy
        // The choice of the consumer has been made in advance
        this.servletDelegate.setServletResolveConsumerStrategy((r, c) -> consumer);
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.servletDelegate.service(request, response);
    }

}
