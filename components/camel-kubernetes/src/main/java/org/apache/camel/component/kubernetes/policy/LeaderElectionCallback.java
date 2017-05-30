package org.apache.camel.component.kubernetes.policy;

/**
 * Created by nferraro on 5/26/17.
 */
public interface LeaderElectionCallback {

    void onLeadershipGranted(String subject);

    void onLeadershipRevoked(String subject);

}
