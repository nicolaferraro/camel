package org.apache.camel.component.spring.web;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.http.common.HttpConsumer;
import org.apache.camel.http.common.HttpHelper;
import org.apache.camel.http.common.HttpMessage;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;

/**
 *
 */
public class CamelHttpRequestHandler implements HttpRequestHandler {

    private Logger log = LoggerFactory.getLogger(getClass());

    private HttpConsumer consumer;

    public CamelHttpRequestHandler(HttpConsumer consumer) {
        ObjectHelper.notNull(consumer, "consumer");
        this.consumer = consumer;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // are we suspended?
        if (consumer.isSuspended()) {
            log.debug("Consumer suspended, cannot service request {}", request);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        // if its an OPTIONS request then return which method is allowed
        if ("OPTIONS".equals(request.getMethod()) && !consumer.isOptionsEnabled()) {
            String s;
            if (consumer.getEndpoint().getHttpMethodRestrict() != null) {
                s = "OPTIONS," + consumer.getEndpoint().getHttpMethodRestrict();
            } else {
                // allow them all
                s = "GET,HEAD,POST,PUT,DELETE,TRACE,OPTIONS,CONNECT,PATCH";
            }
            response.addHeader("Allow", s);
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        if (consumer.getEndpoint().getHttpMethodRestrict() != null
                && !consumer.getEndpoint().getHttpMethodRestrict().contains(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        if ("TRACE".equals(request.getMethod()) && !consumer.isTraceEnabled()) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        // create exchange and set data on it
        Exchange exchange = new DefaultExchange(consumer.getEndpoint(), ExchangePattern.InOut);

        if (consumer.getEndpoint().isBridgeEndpoint()) {
            exchange.setProperty(Exchange.SKIP_GZIP_ENCODING, Boolean.TRUE);
            exchange.setProperty(Exchange.SKIP_WWW_FORM_URLENCODED, Boolean.TRUE);
        }
        if (consumer.getEndpoint().isDisableStreamCache()) {
            exchange.setProperty(Exchange.DISABLE_HTTP_STREAM_CACHE, Boolean.TRUE);
        }

        // we override the classloader before building the HttpMessage just in case the binding
        // does some class resolution
        //ClassLoader oldTccl = overrideTccl(exchange);
        HttpHelper.setCharsetFromContentType(request.getContentType(), exchange);
        exchange.setIn(new HttpMessage(exchange, request, response));
        // set context path as header
        String contextPath = consumer.getEndpoint().getPath();
        exchange.getIn().setHeader("CamelSpringWebContextPath", contextPath);

//        String httpPath = (String)exchange.getIn().getHeader(Exchange.HTTP_PATH);
//        // here we just remove the CamelServletContextPath part from the HTTP_PATH
//        if (contextPath != null
//                && httpPath.startsWith(contextPath)) {
//            exchange.getIn().setHeader(Exchange.HTTP_PATH,
//                    httpPath.substring(contextPath.length()));
//        }

        // we want to handle the UoW
        try {
            consumer.createUoW(exchange);
        } catch (Exception e) {
            log.error("Error processing request", e);
            throw new ServletException(e);
        }

        try {
            if (log.isTraceEnabled()) {
                log.trace("Processing request for exchangeId: {}", exchange.getExchangeId());
            }
            // process the exchange
            consumer.getProcessor().process(exchange);
        } catch (Exception e) {
            exchange.setException(e);
        }

        try {
            // now lets output to the response
            if (log.isTraceEnabled()) {
                log.trace("Writing response for exchangeId: {}", exchange.getExchangeId());
            }
            Integer bs = consumer.getEndpoint().getResponseBufferSize();
            if (bs != null) {
                log.trace("Using response buffer size: {}", bs);
                response.setBufferSize(bs);
            }
            consumer.getBinding().writeResponse(exchange, response);
        } catch (IOException e) {
            log.error("Error processing request", e);
            throw e;
        } catch (Exception e) {
            log.error("Error processing request", e);
            throw new ServletException(e);
        } finally {
            consumer.doneUoW(exchange);
//            restoreTccl(exchange, oldTccl);
        }

    }
}
