package org.apache.camel.component.kubernetes.policy;

/**
 * Created by nferraro on 5/26/17.
 */
public class KubernetesLeaderElectionConfig {

    private String namespace;

    private String configMapName;

    public KubernetesLeaderElectionConfig() {
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

}
