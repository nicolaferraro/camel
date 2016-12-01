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
import java.net.URISyntaxException;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.http.common.HttpBinding;
import org.apache.camel.http.common.HttpCommonEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;

/**
 * The camel-spring-web consumer-only endpoint.
 */
@UriEndpoint(scheme = "spring-web", extendsScheme = "http", title = "Spring Web", syntax = "spring-web:contextPath",
        consumerOnly = true, consumerClass = SpringWebConsumer.class, label = "http")
public class SpringWebEndpoint extends HttpCommonEndpoint {

    private HttpBinding binding;

    @UriPath(label = "consumer") @Metadata(required = "true")
    private String contextPath;

    public SpringWebEndpoint(String endPointURI, SpringWebComponent component, URI httpUri) throws URISyntaxException {
        super(endPointURI, component, httpUri);
        this.contextPath = httpUri.getPath();
    }

    @Override
    public HttpBinding getHttpBinding() {
        // make sure we include spring-web variant of the http binding
        if (this.binding == null) {
            this.binding = new SpringWebHttpBinding();
            this.binding.setTransferException(isTransferException());
            if (getComponent() != null) {
                this.binding.setAllowJavaSerializedObject(getComponent().isAllowJavaSerializedObject());
            }
            this.binding.setHeaderFilterStrategy(getHeaderFilterStrategy());
            this.binding.setEagerCheckContentAvailable(isEagerCheckContentAvailable());
            this.binding.setMapHttpMessageBody(isMapHttpMessageBody());
            this.binding.setMapHttpMessageHeaders(isMapHttpMessageHeaders());
            this.binding.setMapHttpMessageFormUrlEncodedBody(isMapHttpMessageFormUrlEncodedBody());
        }
        return this.binding;
    }

    @Override
    public void setHttpBinding(HttpBinding binding) {
        super.setHttpBinding(binding);
        this.binding = binding;
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
     * The context path.
     */
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
}
