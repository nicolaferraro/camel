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
