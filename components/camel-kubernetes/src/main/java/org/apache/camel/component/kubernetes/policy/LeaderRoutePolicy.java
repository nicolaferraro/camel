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
package org.apache.camel.component.kubernetes.policy;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Route;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.api.management.ManagedAttribute;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.support.RoutePolicySupport;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ReferenceCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ManagedResource(description = "Route policy using a clustered lock")
public class LeaderRoutePolicy extends RoutePolicySupport implements CamelContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderRoutePolicy.class);

    private final AtomicBoolean leader;
    private final Set<Route> startedRoutes;
    private final Set<Route> stoppeddRoutes;
    private final ReferenceCount refCount;

    private CamelContext camelContext;
    private boolean shouldStopRoute;

    private LeaderElectionService leaderElectionService;
    private LeaderElectionConfig config;

    public LeaderRoutePolicy(LeaderElectionService leaderElectionService, LeaderElectionConfig config) {
        this.leaderElectionService = leaderElectionService;
        this.config = config;
        this.stoppeddRoutes = new HashSet<>();
        this.startedRoutes = new HashSet<>();
        this.leader = new AtomicBoolean(false);
        this.shouldStopRoute = true;
        this.refCount = ReferenceCount.on(this::startService, this::stopService);
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public synchronized void onInit(Route route) {
        super.onInit(route);

        LOGGER.info("Route managed by {}. Setting route {} AutoStartup flag to false.", getClass(), route.getId());
        route.getRouteContext().getRoute().setAutoStartup("false");

        stoppeddRoutes.add(route);

        this.refCount.retain();

        startManagedRoutes();
    }

    @Override
    public synchronized void doShutdown() {
        this.refCount.release();
    }

    // ****************************************
    // Helpers
    // ****************************************

    private void startService() {
        // validate
        ObjectHelper.notNull(leaderElectionService, "leaderElectionService", this);
        ObjectHelper.notNull(camelContext, "camelContext", this);

        try {
            this.leaderElectionService.participate(this.config, new LeaderElectionCallback() {
                @Override
                public void onLeadershipGranted(String subject) {
                    setLeader(true);
                }

                @Override
                public void onLeadershipRevoked(String subject) {
                    setLeader(false);
                }
            });


        } catch (Exception e) {
            throw new RuntimeCamelException(e);
        }
    }

    private void stopService() {
        this.leaderElectionService.dismiss(config);
    }

    private void setLeader(boolean isLeader) {
        if (isLeader && leader.compareAndSet(false, isLeader)) {
            LOGGER.info("Leadership taken (service={}, config={})", leaderElectionService, config);
            startManagedRoutes();
        } else if (!isLeader && leader.getAndSet(isLeader)) {
            LOGGER.info("Leadership lost (service={}, config={})", leaderElectionService, config);
            stopManagedRoutes();
        }
    }

    private synchronized void startManagedRoutes() {
        if (!isLeader()) {
            return;
        }

        try {
            for (Route route : stoppeddRoutes) {
                LOGGER.debug("Starting route {}", route.getId());
                route.getRouteContext().getRoute().setAutoStartup("true");
                startRoute(route);
                startedRoutes.add(route);
            }

            stoppeddRoutes.removeAll(startedRoutes);
        } catch (Exception e) {
            handleException(e);
        }
    }

    private synchronized void stopManagedRoutes() {
        if (isLeader()) {
            return;
        }

        try {
            for (Route route : startedRoutes) {
                LOGGER.debug("Stopping route {}", route.getId());
                route.getRouteContext().getRoute().setAutoStartup("false");
                stopRoute(route);
                stoppeddRoutes.add(route);
            }

            startedRoutes.removeAll(stoppeddRoutes);
        } catch (Exception e) {
            handleException(e);
        }
    }

    // *************************************************************************
    // Getter/Setters
    // *************************************************************************

    @ManagedAttribute(description = "Whether to stop route when starting up and failed to become master")
    public boolean isShouldStopRoute() {
        return shouldStopRoute;
    }

    public void setShouldStopRoute(boolean shouldStopRoute) {
        this.shouldStopRoute = shouldStopRoute;
    }

    @ManagedAttribute(description = "Is this route the master or a slave")
    public boolean isLeader() {
        return leader.get();
    }


}