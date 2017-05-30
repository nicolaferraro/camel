package org.apache.camel.component.kubernetes.policy;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * Created by nferraro on 5/26/17.
 */
public class KubernetesLeaderElectionService implements LeaderElectionService {

    private Map<String, KubernetesLock> lockManagers = new HashMap<>();

    private KubernetesClient client = new DefaultKubernetesClient();

    private boolean started;

    public KubernetesLeaderElectionService() {
    }

    @Override
    public void participate(LeaderElectionConfig config, LeaderElectionCallback callback) {
        if (lockManagers.containsKey(config.getSubject())) {
            throw new IllegalStateException("Leader election service is already participating on subject " + config.getSubject());
        }
        KubernetesLock lock = new KubernetesLock((KubernetesLeaderElectionConfig) config, callback, client);
        lockManagers.put(config.getSubject(), lock);
        if (started) {
            lock.tryAcquireLock();
        }
    }

    @Override
    public void dismiss(LeaderElectionConfig config) {
        lockManagers.remove(config.getSubject()).close();
    }

    @Override
    public void start() throws Exception {
        for (KubernetesLock lock : lockManagers.values()) {
            lock.tryAcquireLock();
        }
        started = true;
    }

    @Override
    public void stop() throws Exception {
        started = false;
        for (KubernetesLock lock : lockManagers.values()) {
            lock.close();
        }
    }
}
