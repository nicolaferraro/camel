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

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.http.common.HttpBinding;
import org.apache.camel.http.common.HttpCommonComponent;
import org.apache.camel.http.common.HttpConsumer;
import org.apache.camel.spi.RestApiConsumerFactory;
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.spi.RestConsumerFactory;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.StringHelper;
import org.apache.camel.util.URISupport;
import org.apache.camel.util.UnsafeUriCharactersEncoder;

/**
 * The Camel-spring-web component.
 */
public class SpringWebComponent extends HttpCommonComponent implements RestConsumerFactory, RestApiConsumerFactory {

    public SpringWebComponent() {
        super(SpringWebEndpoint.class);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {

        String scheme = StringHelper.before(uri, ":");
        String after = StringHelper.after(uri, ":");
        // rebuild uri to have exactly one leading slash
        while (after.startsWith("/")) {
            after = after.substring(1);
        }
        after = "/" + after;
        uri = scheme + ":" + after;

        // restructure uri to be based on the parameters left as we dont want to include the Camel internal options
        URI httpUri = URISupport.createRemainingURI(new URI(UnsafeUriCharactersEncoder.encodeHttpURI(uri)), parameters);


        SpringWebEndpoint endpoint = new SpringWebEndpoint(uri, this, httpUri);
        setProperties(endpoint, parameters);

        return endpoint;
    }

    @Override
    public void connect(HttpConsumer consumer) {
        getHandlerMapping().connect(consumer);
    }

    @Override
    public void disconnect(HttpConsumer consumer) {
        getHandlerMapping().disconnect(consumer);
    }

    private CamelHandlerMapping getHandlerMapping() {
        CamelHandlerMapping mapping = getCamelContext().getRegistry().lookupByNameAndType("camelHandlerMapping", CamelHandlerMapping.class);
        if (mapping == null) {
            throw new IllegalStateException("No bean named 'camelHandlerMapping' found in the registry");
        }
        return mapping;
    }

    @Override
    public Consumer createConsumer(CamelContext camelContext, Processor processor, String verb, String basePath, String uriTemplate,
                                   String consumes, String produces, RestConfiguration configuration, Map<String, Object> parameters) throws Exception {
        return doCreateConsumer(camelContext, processor, verb, basePath, uriTemplate, consumes, produces, configuration, parameters, false);
    }

    @Override
    public Consumer createApiConsumer(CamelContext camelContext, Processor processor, String contextPath,
                                      RestConfiguration configuration, Map<String, Object> parameters) throws Exception {
        // reuse the createConsumer method we already have. The api need to use GET and match on uri prefix
        return doCreateConsumer(camelContext, processor, "GET", contextPath, null, null, null, configuration, parameters, true);
    }

    Consumer doCreateConsumer(CamelContext camelContext, Processor processor, String verb, String basePath, String uriTemplate,
                              String consumes, String produces, RestConfiguration configuration, Map<String, Object> parameters, boolean api) throws Exception {

        String path = basePath;
        if (uriTemplate != null) {
            // make sure to avoid double slashes
            if (uriTemplate.startsWith("/")) {
                path = path + uriTemplate;
            } else {
                path = path + "/" + uriTemplate;
            }
        }
        path = FileUtil.stripLeadingSeparator(path);

        // if no explicit port/host configured, then use port from rest configuration
        RestConfiguration config = configuration;
        if (config == null) {
            config = camelContext.getRestConfiguration("spring-web", true);
        }

        Map<String, Object> map = new HashMap<String, Object>();
        // build query string, and append any endpoint configuration properties
        if (config.getComponent() == null || config.getComponent().equals("spring-web")) {
            // setup endpoint options
            if (config.getEndpointProperties() != null && !config.getEndpointProperties().isEmpty()) {
                map.putAll(config.getEndpointProperties());
            }
        }

        boolean cors = config.isEnableCORS();
        if (cors) {
            // allow HTTP Options as we want to handle CORS in rest-dsl
            map.put("optionsEnabled", "true");
        }

        // do not append with context-path as the servlet path should be without context-path

        String query = URISupport.createQueryString(map);

        String url;
        if (api) {
            url = "spring-web:///%s?matchOnUriPrefix=true&httpMethodRestrict=%s";
        } else {
            url = "spring-web:///%s?httpMethodRestrict=%s";
        }

        // must use upper case for restrict
        String restrict = verb.toUpperCase(Locale.US);
        if (cors) {
            restrict += ",OPTIONS";
        }
        // get the endpoint
        url = String.format(url, path, restrict);

        if (!query.isEmpty()) {
            url = url + "&" + query;
        }

        SpringWebEndpoint endpoint = camelContext.getEndpoint(url, SpringWebEndpoint.class);
        setProperties(camelContext, endpoint, parameters);

        if (!map.containsKey("httpBindingRef")) {
            // use the rest binding, if not using a custom http binding
            HttpBinding binding = new SpringWebHttpBinding();
            binding.setHeaderFilterStrategy(endpoint.getHeaderFilterStrategy());
            binding.setTransferException(endpoint.isTransferException());
            binding.setEagerCheckContentAvailable(endpoint.isEagerCheckContentAvailable());
            endpoint.setHttpBinding(binding);
        }

        // configure consumer properties
        Consumer consumer = endpoint.createConsumer(processor);
        if (config.getConsumerProperties() != null && !config.getConsumerProperties().isEmpty()) {
            setProperties(camelContext, consumer, config.getConsumerProperties());
        }

        return consumer;
    }
}
