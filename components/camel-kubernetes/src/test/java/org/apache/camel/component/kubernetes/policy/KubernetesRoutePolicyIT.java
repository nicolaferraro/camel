package org.apache.camel.component.kubernetes.policy;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Test;

/**
 * Created by nferraro on 5/29/17.
 */
public class KubernetesRoutePolicyIT {

    @Test
    public void test() throws Exception {

        KubernetesLeaderElectionConfig config = new KubernetesLeaderElectionConfig("aaaaa");
        config.setConfigMapName("aaaaa");
        config.setEndpointName("bbbbb");
        config.setNamespace("myproject");

        KubernetesLeaderElectionService ser = new KubernetesLeaderElectionService();
        LeaderRoutePolicy policy = new LeaderRoutePolicy(ser, config);

        CamelContext ctx = new DefaultCamelContext();
        ctx.addService(ser, true, false);

        new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("timer:tick")
                .routePolicy(policy)
                .log("Hello world");
            }
        }.addRoutesToCamelContext(ctx);

        ctx.start();

        Thread.sleep(60000);

    }

}
