package org.apache.camel.component.kubernetes.policy;

import org.apache.camel.Service;

/**
 * Created by nferraro on 5/26/17.
 */
public interface LeaderElectionService extends Service {

    void join(String subject, LeaderElectionCallback callback);

    void leave(String subject);

}
