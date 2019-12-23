package com.yit.deploy.core.dsl.evaluate;

import com.yit.deploy.core.model.DeployModelTable;
import com.yit.deploy.core.model.Environment;
import com.yit.deploy.core.model.Playbook;
import com.yit.deploy.core.variables.SimpleVariables;
import com.yit.deploy.core.variables.Variables;
import com.yit.deploy.core.variables.resolvers.SimpleVariableResolver;
import com.yit.deploy.core.variables.resolvers.VariableResolver;

public class PlaybookEvaluationContext extends EnvironmentEvaluationContext {

    private final Playbook playbook;
    private final Variables playbookWritable;

    public PlaybookEvaluationContext(Playbook playbook, Variables playbookWritable, Environment env, DeployModelTable modelTable) {
        super(env, modelTable);
        this.playbook = playbook;
        this.playbookWritable = playbookWritable;

        resolveVars(playbook.getVars());

        resolveWritableVars(playbookWritable);
    }

    public void playbookvar(String variable, Object value) {
        VariableResolver resolver = new SimpleVariableResolver();
        resolver.setWritableVars(playbookWritable);
        resolver.setVariable(variable, value);
    }

    public Playbook getPlaybook() {
        return playbook;
    }

    protected Variables getPlaybookWritable() {
        return playbookWritable;
    }
}
