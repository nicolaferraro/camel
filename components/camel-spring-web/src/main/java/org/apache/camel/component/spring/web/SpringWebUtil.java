package org.apache.camel.component.spring.web;

import javax.servlet.http.HttpServletRequest;

/**
 * Utility methods for Spring web features.
 */
public final class SpringWebUtil {

    private SpringWebUtil() {}

    /**
     * Returns the part of the uri related to the user request.
     */
    public static String getRequestedPath(HttpServletRequest request) {
        // Avoid using servlet path in Spring web
        String uri = request.getRequestURI();

        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        return uri.substring(contextPath.length());
    }

}
