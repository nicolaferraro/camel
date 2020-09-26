/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.component.jmx;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.spi.EndpointUriFactory;

/**
 * Generated by camel build tools - do NOT edit this file!
 */
public class JMXEndpointUriFactory extends org.apache.camel.support.component.EndpointUriFactorySupport implements EndpointUriFactory {

    private static final String BASE = ":serverURL";

    @Override
    public boolean isEnabled(String scheme) {
        return "jmx".equals(scheme);
    }

    @Override
    public String buildUri(String scheme, Map<String, Object> parameters) throws URISyntaxException {
        String syntax = scheme + BASE;
        String uri = syntax;

        Map<String, Object> copy = new HashMap<>(parameters);

        uri = buildPathParameter(syntax, uri, "serverURL", null, false, copy);
        uri = buildQueryParameters(uri, copy);
        return uri;
    }
}
