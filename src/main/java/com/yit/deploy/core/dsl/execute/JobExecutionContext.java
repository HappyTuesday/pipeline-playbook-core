package com.yit.deploy.core.dsl.execute;

import com.yit.deploy.core.dsl.evaluate.JobEvaluationContext;
import com.yit.deploy.core.dsl.support.MemoryCacheProvider;
import com.yit.deploy.core.model.*;
import com.yit.deploy.core.support.*;
import com.yit.deploy.core.variables.CacheProvider;
import com.yit.deploy.core.variables.resolvers.ResolveContext;
import com.yit.deploy.core.variables.SimpleVariables;
import com.yit.deploy.core.variables.Variables;

public class JobExecutionContext
    extends JobEvaluationContext
    implements StepsSupport, ClosuresSupport, ContainersSupport, DockerSupport,
    WorkspaceSupport, FilePathSupport, ResourceSupport, ConfigSupport {

    private final Build build;
    private final CacheProvider cacheProvider;

    public JobExecutionContext(JobExecutionContext cx) {
        this(cx.build, cx.cacheProvider, cx.getPlaybookWritable());
    }

    public JobExecutionContext(Build build) {
        this(build, new MemoryCacheProvider(), new SimpleVariables());
    }

    private JobExecutionContext(Build build, CacheProvider cacheProvider, Variables playbookWritable) {
        super(build.getJob(), playbookWritable);
        this.build = build;
        this.cacheProvider = cacheProvider;
    }

    public PlayExecutionContext toPlay(Play play) {
        return new PlayExecutionContext(this, play);
    }

    @Override
    public JobExecutionContext getExecutionContext() {
        return this;
    }

    @Override
    public PipelineScript getScript() {
        return build.getScript();
    }

    /**
     * get the resolve context
     *
     * @return resolve context
     */
    @Override
    public ResolveContext getResolveContext() {
        return super.getResolveContext().userParameters(build.getSpec().userParameters).cacheProvider(cacheProvider);
    }

    @Override
    public JobExecutionContext withJob(Job job) {
        if (this.build.getJob() == job) {
            return this;
        }

        return new JobExecutionContext(build.createNewBuild(job, build.getSpec()));
    }

    @Override
    public Build getBuild() {
        return build;
    }

    public GitCommitDetail getProjectCommit() {
        return build.getProjectCommit();
    }

    public void setProjectCommit(GitCommitDetail detail) {
        build.setProjectCommit(detail);
    }

}
