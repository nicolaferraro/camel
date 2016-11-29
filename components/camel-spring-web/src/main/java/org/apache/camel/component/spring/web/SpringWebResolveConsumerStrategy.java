package org.apache.camel.component.spring.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.camel.http.common.HttpConsumer;
import org.apache.camel.http.common.HttpServletResolveConsumerStrategy;
import org.apache.camel.support.RestConsumerContextPathMatcher;

/**
 *
 */
public class SpringWebResolveConsumerStrategy extends HttpServletResolveConsumerStrategy {

    @Override
    @SuppressWarnings("unchecked")
    public HttpConsumer resolve(HttpServletRequest request, Map<String, HttpConsumer> consumers) {
        HttpConsumer answer = null;

        String path = request.getServletPath();
        if (path == null) {
            return null;
        }
        String method = request.getMethod();
        if (method == null) {
            return null;
        }

        List<RestConsumerContextPathMatcher.ConsumerPath> paths = new ArrayList<RestConsumerContextPathMatcher.ConsumerPath>();
        for (final Map.Entry<String, HttpConsumer> entry : consumers.entrySet()) {
            paths.add(new RestConsumerContextPathMatcher.ConsumerPath<HttpConsumer>() {
                @Override
                public String getRestrictMethod() {
                    return entry.getValue().getEndpoint().getHttpMethodRestrict();
                }

                @Override
                public String getConsumerPath() {
                    return entry.getValue().getPath();
                }

                @Override
                public HttpConsumer getConsumer() {
                    return entry.getValue();
                }
            });
        }

        RestConsumerContextPathMatcher.ConsumerPath<HttpConsumer> best = RestConsumerContextPathMatcher.matchBestPath(method, path, paths);
        if (best != null) {
            answer = best.getConsumer();
        }

        return answer;
    }
}
