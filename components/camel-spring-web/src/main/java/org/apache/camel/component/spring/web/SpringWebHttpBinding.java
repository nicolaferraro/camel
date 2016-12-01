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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import javax.activation.DataSource;
import javax.servlet.http.HttpServletRequest;

import org.apache.camel.Attachment;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.http.common.DefaultHttpBinding;
import org.apache.camel.http.common.HttpCommonEndpoint;
import org.apache.camel.http.common.HttpMessage;
import org.apache.camel.impl.DefaultAttachment;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * A custom HttpBinding taking into account some features of the requests from spring web.
 * It handles both normal and multipart requests.
 */
public class SpringWebHttpBinding extends DefaultHttpBinding {

    public SpringWebHttpBinding() {
    }

    @Deprecated
    public SpringWebHttpBinding(HttpCommonEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    protected void populateRequestParameters(HttpServletRequest request, HttpMessage message) throws Exception {
        super.populateRequestParameters(request, message);

        String path = getRawPath(request);
        if (path == null) {
            return;
        }

        // in the endpoint the user may have defined rest {} placeholders
        // so we need to map those placeholders with data from the incoming request context path

        SpringWebEndpoint endpoint = (SpringWebEndpoint) message.getExchange().getFromEndpoint();
        String consumerPath = endpoint.getPath();

        if (useRestMatching(consumerPath)) {

            // split using single char / is optimized in the jdk
            String[] paths = path.split("/");
            String[] consumerPaths = consumerPath.split("/");

            for (int i = 0; i < consumerPaths.length; i++) {
                if (paths.length < i) {
                    break;
                }
                String p1 = consumerPaths[i];
                if (p1.startsWith("{") && p1.endsWith("}")) {
                    String key = p1.substring(1, p1.length() - 1);
                    String value = paths[i];
                    if (value != null) {
                        message.setHeader(key, value);
                    }
                }
            }
        }
    }

    private boolean useRestMatching(String path) {
        // only need to do rest matching if using { } placeholders
        return path.indexOf('{') > -1;
    }

    @Override
    protected String getRawPath(HttpServletRequest request) {
        return SpringWebUtil.getRequestedPath(request);
    }

    @Override
    protected void populateAttachments(HttpServletRequest request, HttpMessage message) {
        if (request instanceof MultipartHttpServletRequest) {
            populateAttachmentsMultipart((MultipartHttpServletRequest) request, message);
        } else {
            // fallback with default implementation
            super.populateAttachments(request, message);
        }
    }

    protected void populateAttachmentsMultipart(MultipartHttpServletRequest request, HttpMessage message) {
        try {
            // Parts have already been converted in multipart files
            Iterator<String> names = request.getFileNames();
            while (names.hasNext()) {
                String name = names.next();
                MultipartFile file = request.getFile(name);
                DataSource ds = new MultiPartFileDataSource(file);
                Attachment attachment = new DefaultAttachment(ds);

                HttpHeaders headers = request.getMultipartHeaders(name);
                for (String headerName : headers.keySet()) {
                    for (String headerValue : headers.get(headerName)) {
                        attachment.addHeader(headerName, headerValue);
                    }
                }
                message.addAttachmentObject(file.getName(), attachment);
            }
        } catch (Exception e) {
            throw new RuntimeCamelException("Cannot populate multipart file attachments", e);
        }
    }

    static final class MultiPartFileDataSource implements DataSource {
        private final MultipartFile part;

        MultiPartFileDataSource(MultipartFile part) {
            this.part = part;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return null;
        }

        @Override
        public String getName() {
            return part.getName();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return part.getInputStream();
        }

        @Override
        public String getContentType() {
            return part.getContentType();
        }
    }
}
