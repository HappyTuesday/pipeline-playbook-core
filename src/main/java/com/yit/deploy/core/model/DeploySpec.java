package com.yit.deploy.core.model;


import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * the context of the whole deployment, used to store middle time variables
 */
public class DeploySpec {
    public final List<String> plays;
    public final List<String> tasksToSkip;
    public final List<String> servers;
    public final List<String> serversToRetire;
    public final Map<String, Object> userParameters;

    public DeploySpec() {
        this(Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyMap()
        );
    }

    public DeploySpec(List<String> plays) {
        this(plays,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyMap()
        );
    }

    public DeploySpec(List<String> plays,
                      List<String> tasksToSkip,
                      List<String> servers,
                      List<String> serversToRetire,
                      Map<String, Object> userParameters) {
        this.plays = plays;
        this.tasksToSkip = tasksToSkip;
        this.servers = servers;
        this.serversToRetire = serversToRetire;
        this.userParameters = userParameters;
    }
}
