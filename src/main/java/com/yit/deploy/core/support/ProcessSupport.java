package com.yit.deploy.core.support;

import com.yit.deploy.core.model.PipelineScript;
import com.yit.deploy.core.model.ProcessExecutionStatus;
import com.yit.deploy.core.model.ProcessLauncher;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

public interface ProcessSupport {

    default PipelineScript getScript() {
        return null;
    }

    default ProcessLauncher launch() {
        ProcessLauncher launcher = new ProcessLauncher();
        PipelineScript script = getScript();
        if (script != null) {
            launcher.script(script);
        }
        return launcher;
    }

    default ProcessExecutionStatus execute(
        @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ProcessLauncher.ProcessLauncherDslContext.class)
        Closure<Object> closure
    ) {
        return launch().setup(closure).executeReturnStatus();
    }

    default void bash(String shell) {
        launch().bash(shell).executePrintOutput();
    }
}
