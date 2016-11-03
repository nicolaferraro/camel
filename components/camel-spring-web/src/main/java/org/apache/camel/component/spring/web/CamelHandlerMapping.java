package org.apache.camel.component.spring.web;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.camel.http.common.HttpConsumer;
import org.apache.camel.http.common.ServletResolveConsumerStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

/**
 *
 */
public class CamelHandlerMapping implements HandlerMapping, Ordered {

    private static final CamelHandlerMapping INSTANCE = new CamelHandlerMapping();

    private Logger log = LoggerFactory.getLogger(getClass());

    private ServletResolveConsumerStrategy servletResolveConsumerStrategy = new SpringWebResolveConsumerStrategy();

    private Map<String, HttpConsumer> consumers = new HashMap<>(); // TODO should it be thread safe?

    private CamelHandlerMapping() {
    }

    public static CamelHandlerMapping getInstance() {
        return INSTANCE;
    }

    @Override
    public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        HttpConsumer consumer = servletResolveConsumerStrategy.resolve(request, consumers);
        if (consumer != null) {
            return new HandlerExecutionChain(new CamelHttpRequestHandler(consumer));
        }

        return null;
    }

    public void connect(HttpConsumer consumer) {
        log.debug("Connecting consumer: {}", consumer);
        consumers.put(consumer.getEndpoint().getEndpointUri(), consumer);
    }

    public void disconnect(HttpConsumer consumer) {
        log.debug("Disconnecting consumer: {}", consumer);
        consumers.remove(consumer.getEndpoint().getEndpointUri());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
