/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.kubernetes.policy;


import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.DoneableConfigMap;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;

import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages locking a named endpoint in Kubernetes
 */
public class KubernetesLock implements Closeable {
    private static final transient Logger LOG = LoggerFactory.getLogger(KubernetesLock.class);

    private static final String DEFAULT_NAMESPACE = "default";

    private final KubernetesLeaderElectionConfig config;
    private final LeaderElectionCallback callback;
    private final KubernetesClient client;

    private String endpointName;

    private String namespace;
    private final String id;
    private Watch podWatch = null;
    private Watch configMapWatch;
    private AtomicBoolean owner = new AtomicBoolean(false);

    public KubernetesLock(String subject, KubernetesLeaderElectionConfig config, LeaderElectionCallback callback, KubernetesClient client) {
        this.config = config;
        this.callback = callback;
        this.client = client;

        this.namespace = config.getNamespace();
        if (ObjectHelper.isEmpty(namespace)) {
            namespace = DEFAULT_NAMESPACE;
        }

        this.id = System.getenv("HOSTNAME");
        if (isEmpty(this.id)) {
            throw new IllegalArgumentException("Could not find this podName via $HOSTNAME environment variable!");
        }

        this.endpointName = subject;
    }

    protected static boolean isEmpty(String ownerPodId) {
        return ownerPodId == null || ownerPodId.length() == 0;
    }

    /**
     * Tries to acquire the lock for the endpointName and invokes the <code>onOwnerNotification</code> if it
     * gets the lock
     */
    public void tryAcquireLock() {
        Resource<ConfigMap, DoneableConfigMap> configMapResource = client.configMaps().inNamespace(namespace).withName(config.getConfigMapName());
        boolean retry;
        do {
            retry = false;
            ConfigMap configMap = configMapResource.get();
            boolean create = false;
            if (configMap == null) {
                // lets create an empty ConfigMap
                configMap = new ConfigMapBuilder().
                        withNewMetadata().withName(config.getConfigMapName()).
                        addToLabels("provider", "fabric8").addToLabels("kind", "camel-locks").
                        endMetadata().build();
                create = true;
            } else {
                // lets watch the ConfigMap to see if another pod takes ownership to ensure we keep watching the correct pod
                // for when that one dies
                configMapWatch = configMapResource.watch(new Watcher<ConfigMap>() {
                    @Override
                    public void eventReceived(Action action, ConfigMap configMap) {
                        switch (action) {
                        case MODIFIED:
                        case DELETED:
                            LOG.info("ConfigMap " + config.getConfigMapName() + " " + action + " so lets try acquire the lock again");
                            tryAcquireLock();
                        }
                    }

                    @Override
                    public void onClose(KubernetesClientException e) {
                    }
                });
            }
            Map<String, String> data = configMap.getData();
            if (data == null) {
                data = new HashMap<>();
            }

            String currentOwnerPodName = data.get(endpointName);
            if (id.equals(currentOwnerPodName)) {
                // we are already the owner
                // e.g. the pod restarted
                LOG.info("This pod " + id + " already has the lock for endpoint: " + endpointName + " in ConfigMap " + config.getConfigMapName());
                notifyHasLock();
                return;
            }
            if (!isEmpty(currentOwnerPodName)) {
                // a different pod claims to own this lock
                // so lets watch if the pod dies
                if (podWatch != null) {
                    podWatch.close();
                }
                final String deadPodName = currentOwnerPodName;
                LOG.info("Pod " + deadPodName + " has the lock for endpoint: " + endpointName + " in ConfigMap " + config.getConfigMapName() + " so lets watch it...");

                PodResource<Pod, DoneablePod> podResource = client.pods().inNamespace(namespace).withName(currentOwnerPodName);
                podWatch = podResource.watch(new Watcher<Pod>() {
                    @Override
                    public void eventReceived(Action action, Pod pod) {
                        if (action == Action.DELETED) {
                            LOG.info("Pod " + deadPodName + " has died so lets try grab the lock");
                            tryAcquireLock();
                        }
                    }

                    @Override
                    public void onClose(KubernetesClientException e) {
                    }
                });

                // lets see if the pod is already dead so we may have missed the watch event
                Pod pod = null;
                try {
                    pod = podResource.get();
                } catch (Exception e) {
                    // ignore
                }

                // pod has died so lets try claim it right now
                if (pod == null) {
                    // we don't need the watch any more
                    podWatch.close();
                    podWatch = null;

                    LOG.info("Pod " + deadPodName + " has died so lets try grab the lock");
                    currentOwnerPodName = null;
                }
            }

            if (isEmpty(currentOwnerPodName)) {
                // lets try claim the lock!
                LOG.info("Trying to grab the lock for " + endpointName + " in ConfigMap " + config.getConfigMapName());

                data.put(endpointName, id);
                try {
                    if (create) {
                        configMapResource.create(configMap);
                    } else {
                        configMapResource
                                .lockResourceVersion(configMap.getMetadata().getResourceVersion())
                                .replace(configMap);
                    }
                    String operation = create ? "Created" : "Updated";
                    LOG.info(operation + " ConfigMap: " + config.getConfigMapName() + " with data " + configMap.getData());

                    notifyHasLock();
                } catch (Exception e) {
                    // if failed to update its probably someone beat us to it..
                    // so lets watch the owner pod again
                    LOG.info("Failed to update ConfigMap " + config.getConfigMapName() + ". Probably due other pod winning: " + e);

                    // lets try to grab the lock again, either we'll get it or someone will have beaten us again!
                    retry = true;
                }
            }
        } while (retry);
    }

    /**
     * Returns true if this object is the owner of the lock for the given endpoint name
     */
    public boolean isOwner() {
        return owner.get();
    }

    protected void notifyHasLock() {
        owner.set(true);
        callback.onLeadershipChange(endpointName, true);
    }

    @Override
    public void close() {
        if (configMapWatch != null) {
            configMapWatch.close();
        }
        if (podWatch != null) {
            podWatch.close();
        }
    }
}