package org.apache.camel.component.kubernetes.policy;

import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * Created by nferraro on 5/26/17.
 */
public class KubernetesLeaderElectionConfig extends LeaderElectionConfig {

    private String namespace;

    private String configMapName;

    private String endpointName;

    public KubernetesLeaderElectionConfig(String subject) {
        this(subject, null);
    }

    public KubernetesLeaderElectionConfig(String subject, String id) {
        super(subject, id);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getConfigMapName() {
        return configMapName;
    }

    public void setConfigMapName(String configMapName) {
        this.configMapName = configMapName;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }
}
