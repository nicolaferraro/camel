/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.component.openstack.cinder;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.spi.EndpointUriFactory;

/**
 * Generated by camel build tools - do NOT edit this file!
 */
public class CinderEndpointUriFactory extends org.apache.camel.support.component.EndpointUriFactorySupport implements EndpointUriFactory {

    private static final String BASE = ":host";

    @Override
    public boolean isEnabled(String scheme) {
        return "openstack-cinder".equals(scheme);
    }

    @Override
    public String buildUri(String scheme, Map<String, Object> parameters) throws URISyntaxException {
        String syntax = scheme + BASE;
        String uri = syntax;

        Map<String, Object> copy = new HashMap<>(parameters);

        uri = buildPathParameter(syntax, uri, "host", null, true, copy);
        uri = buildQueryParameters(uri, copy);
        return uri;
    }
}
