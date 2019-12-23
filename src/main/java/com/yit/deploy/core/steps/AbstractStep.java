package com.yit.deploy.core.steps;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.exceptions.ExitException;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.model.Environment;
import com.yit.deploy.core.model.PipelineScript;
import hudson.FilePath;

import java.util.function.Consumer;

/**
 * Created by nick on 14/09/2017.
 */
public abstract class AbstractStep {
    /**
     * while executing the step, we may need some variables. so this variable context does for us.
     */
    protected final JobExecutionContext context;

    public AbstractStep(JobExecutionContext context) {
        this.context = context;
    }

    public Environment getEnv() {
        return context.getEnv();
    }

    public PipelineScript getScript() {
        return context.getScript();
    }

    public JobExecutionContext getExecutionContext() {
        return context;
    }

    public Object getVariable(String name) {
        return context.getVariable(name);
    }

    public <T> T getVariable(String name, Class<T> clazz) {
        return context.getVariable(name, clazz);
    }

    /**
     * Execute this step
     * @return
     */
    public Object execute() {
        try {
            return executeOverride();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            if (ExitException.belongsTo(e)) {
                throw ExitException.wrap(e);
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * override this method to provide its execution implementation
     * @return
     * @throws Exception
     */
    protected abstract Object executeOverride() throws Exception;

    @SuppressWarnings("unchecked")
    public <T> T execute(Class<T> clazz) {
        return (T) execute();
    }

    protected void withTextTempFile(String content, Consumer<FilePath> consumer) {
        deleteAfterUse(getScript().createTextTempFile(content, "steps", this.getClass().getSimpleName()), consumer);
    }

    protected void withTempFolder(Consumer<FilePath> consumer) {
        deleteAfterUse(getScript().createTempDir("steps", this.getClass().getSimpleName()), consumer);
    }

    private void deleteAfterUse(FilePath file, Consumer<FilePath> consumer) {
        try {
            consumer.accept(file);
        } finally {
            try {
                file.delete();
            } catch (Exception e) {
                getScript().warn("delete temp file failed: %s", e.getMessage());
            }
        }
    }
}
