package org.apache.camel.component.spring.web;

import org.apache.camel.Processor;
import org.apache.camel.http.common.HttpConsumer;

/**
 *
 */
public class SpringWebConsumer extends HttpConsumer {

    public SpringWebConsumer(SpringWebEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

}
