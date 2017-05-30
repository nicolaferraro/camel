package org.apache.camel.component.kubernetes.policy;

import java.util.Optional;

import org.apache.camel.util.ObjectHelper;

/**
 * Created by nferraro on 5/26/17.
 */
public class LeaderElectionConfig {

    private String subject;

    private String id;

    public LeaderElectionConfig(String subject) {
        this(subject, null);
    }

    public LeaderElectionConfig(String subject, String id) {
        this.subject = ObjectHelper.notNull(subject, "subject");
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }
}
