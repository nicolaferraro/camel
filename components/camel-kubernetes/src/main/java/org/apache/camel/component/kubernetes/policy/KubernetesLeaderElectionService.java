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

    private KubernetesLeaderElectionConfig config;

    private KubernetesClient client = new DefaultKubernetesClient();

    private boolean started;

    public KubernetesLeaderElectionService(KubernetesLeaderElectionConfig config) {
        this.config = config;
    }

    @Override
    public void join(String subject, LeaderElectionCallback callback) {
        if (lockManagers.containsKey(subject)) {
            throw new IllegalStateException("Leader election service is already participating on subject " + subject);
        }
        KubernetesLock lock = new KubernetesLock(subject, config, callback, client);
        lockManagers.put(subject, lock);
        if (started) {
            lock.tryAcquireLock();
        }
    }

    @Override
    public void leave(String subject) {
        lockManagers.remove(subject).close();
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
