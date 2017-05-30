package org.apache.camel.component.kubernetes.policy;

/**
 * Created by nferraro on 5/26/17.
 */
@FunctionalInterface
public interface LeaderElectionCallback {

    void onLeadershipChange(String subject, boolean leader);

}
