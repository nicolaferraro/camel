package org.apache.camel.component.spring.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;

import org.apache.camel.http.common.HttpConsumer;
import org.apache.camel.http.common.ServletResolveConsumerStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

/**
 * A Spring Bean responsible for intercepting requests to the Spring Servlet if they are related to Camel routes.
 */
public class CamelHandlerMapping implements HandlerMapping, Ordered {

    private Logger log = LoggerFactory.getLogger(getClass());

    private ServletResolveConsumerStrategy servletResolveConsumerStrategy = new SpringWebResolveConsumerStrategy();

    private Map<String, HttpConsumer> consumers = new ConcurrentHashMap<>();

    private Map<String, CamelHttpRequestAdapter> adapters = new ConcurrentHashMap<>();

    public CamelHandlerMapping() {
    }

    /**
     * Tries to resolve a Camel consumer, otherwise returns null,
     * delegating the task of handling the request to the Spring Framework.
     */
    @Override
    public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        HttpConsumer consumer = servletResolveConsumerStrategy.resolve(request, consumers);
        if (consumer != null) {
            CamelHttpRequestAdapter adapter = adapters.get(consumerKey(consumer));
            if (adapter != null) {
                // it can be null during shutdown
                return new HandlerExecutionChain(adapter);
            }
        }

        return null;
    }

    public void connect(HttpConsumer consumer) {
        log.debug("Connecting consumer: {}", consumer);
        consumers.put(consumerKey(consumer), consumer);
        adapters.put(consumerKey(consumer), new CamelHttpRequestAdapter(consumer));
    }

    public void disconnect(HttpConsumer consumer) {
        log.debug("Disconnecting consumer: {}", consumer);
        consumers.remove(consumerKey(consumer));
        adapters.remove(consumerKey(consumer));
    }

    protected String consumerKey(HttpConsumer consumer) {
        return consumer.getEndpoint().getEndpointUri();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
