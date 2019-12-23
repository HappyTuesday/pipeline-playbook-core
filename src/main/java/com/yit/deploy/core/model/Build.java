package com.yit.deploy.core.model;

import com.yit.deploy.core.control.DeployService;
import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.exceptions.ExitException;
import com.yit.deploy.core.exceptions.RenderTemplateException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.records.BuildRecord;
import com.yit.deploy.core.steps.MailStep;
import hudson.AbortException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Build {

    private final Build parent;
    private final DeployService service;
    private final PipelineScript script;

    private final Job job;
    private final DeploySpec spec;

    private BuildRecord record;
    private GitCommitDetail projectCommit;

    public Build(Job job,
                 DeploySpec spec,
                 DeployService service,
                 PipelineScript script,
                 Build parent) {
        this.service = service;
        this.script = script;
        this.spec = spec;
        this.job = job;
        this.parent = parent;
    }

    public Build(String jobName,
                 DeploySpec spec,
                 DeployModelTable modelTable,
                 DeployService service,
                 PipelineScript script,
                 Build parent) {
        this(modelTable.getJobByName(jobName), spec, service, script, parent);
    }

    public Build(String jobName, DeploySpec spec, DeployService service, PipelineScript script, Build parent) {
        this(jobName, spec, service.getModelTable(), service, script, parent);
    }

    public void execute() {
        if (record != null) {
            throw new IllegalStateException("build is already started");
        }

        record = new BuildRecord();
        if (parent != null) {
            record.setParentId(parent.getId());
        }
        DeployUser deployUser = script.getCurrentUser();
        if (deployUser != null) {
            record.setDeployUserName(deployUser.getFullName());
        }
        record.setJobName(job.getJobName());
        record.setEnvName(job.getEnvName());
        record.setProjectName(job.getProjectName());
        record.setJenkinsBuild(script.isRoot());
        record.setRecordCommitId(job.getModelTable().getCommit().getId());
        record.setConfigCommitHash(job.getModelTable().getDeployConfig().getCommitHash());
        record.setPlays(spec.plays);
        record.setTasksToSkip(spec.tasksToSkip);
        record.setServers(spec.servers);
        if (spec.userParameters != null) {
            LinkedHashMap<String, Object> ps = new LinkedHashMap<>(spec.userParameters);
            ps.remove("deployInventory"); // hotfix
            for (String key : ps.keySet()) {
                Object value = ps.get(key);

                if (value instanceof String && ((String) value).length() > 4096) {
                    ps.put(key, ((String) value).substring(0, 4096));
                }
            }

            record.setUserParameters(ps);
        }
        record.setStartedTime(new Date());

        service.addBuild(record);

        script.setJob(job);
        script.info("== START BUILD WITH CONFIG %s RECORD %d ==",
            record.getConfigCommitHash(),
            record.getRecordCommitId());

        try {
            script.createWorkSpaceFolders();
            job.execute(this);

            record.setFinishedTime(new Date());
            record.setFailed(false);

            DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            script.info("== BUILD FINISHED AT " + df.format(record.getFinishedTime()) + " ==");
        } catch (Throwable t) {
            String failedType = retrieveFailedType(t, new HashSet<>());
            String failedMessage = retrieveDetailedErrorMessage(t, new HashSet<>());

            record.setFinishedTime(new Date());
            record.setFailed(true);
            record.setFailedType(failedType);
            record.setFailedMessage(failedMessage);

            InterruptedException interrupted = ExitException.firstInterrupted(t);
            if (interrupted != null) {
                Thread.currentThread().interrupt();
            }

            if (script.isSendFailureEmail()) {
                sendFailureEmail();
            }

            throw t;
        } finally {
            service.finishBuild(record);
        }
    }

    public void setProjectCommit(GitCommitDetail detail) {
        if (record == null) {
            throw new IllegalStateException("build is not started");
        }

        this.projectCommit = detail;

        record.setProjectCommitBranch(detail.getBranch());
        record.setProjectCommitHash(detail.getHash());
        record.setProjectCommitEmail(detail.getEmailAddress());
        record.setProjectCommitDetail(detail.getDetail());
        record.setProjectCommitDate(new Date(detail.getDate()));

        service.updateProjectCommitToBuild(record);
    }

    public Build createNewBuild(Job targetJob, DeploySpec spec) {
        return new Build(targetJob, spec, service, script.fork(targetJob), this);
    }

    private void sendFailureEmail() {
        JobExecutionContext context = new JobExecutionContext(this);

        String emailSubject = String.format("[Jenkins][%s] Execute Job %s Failed - %s",
            job.getEnv().getName(), job.getJobName(), record.getFailedType());

        String emailTo = null;
        List<String> emailBodyLines = Lambda.asList(
            "Execute Job " + job.getJobName() + " Failed",
            "JENKINS BUILD URL: " + script.getAbsoluteUrl(),
            "ERROR DETAIL: " + record.getFailedMessage()
        );

        if (projectCommit != null) {
            if (!"jenkins@yit.com".equals(projectCommit.getEmailAddress())) {
                emailTo = projectCommit.getEmailAddress();
            }
            emailBodyLines.add("COMMIT DETAIL:");
            emailBodyLines.add(projectCommit.getDetail());
        }

        String copyto = context.getVariableOrDefault("DEPLOY_ITEM_FAILURE_NOTIFICATION_COPY_TO", "");

        if (script.getCurrentUser().getEmailAddress() != null) {
            emailTo = script.getCurrentUser().getEmailAddress();
            emailBodyLines.add(0, "USER: " + script.getCurrentUser().getDisplayName());
            copyto = context.getVariableOrDefault("DEPLOY_ITEM_FAILURE_NOTIFICATION_DEFAULT_COPY_TO", "");
        }

        if (emailTo != null) {
            MailStep mailStep = new MailStep(context);
            new MailStep.DslContext(mailStep)
                .to(emailTo)
                .cc(copyto)
                .subject(emailSubject)
                .body(String.join("\n", emailBodyLines));
            mailStep.execute();
        }
    }

    public long getId() {
        return record.getId();
    }

    public PipelineScript getScript() {
        return script;
    }

    public Job getJob() {
        return job;
    }

    public DeploySpec getSpec() {
        return spec;
    }

    public GitCommitDetail getProjectCommit() {
        return projectCommit;
    }

    public DeployService getService() {
        return service;
    }

    private static String retrieveDetailedErrorMessage(Throwable t, Set<Throwable> close) {
        if (t instanceof RenderTemplateException) { // avoid too detailed info
            return t.getMessage();
        }

        String msg = t.getMessage();
        Throwable cause = t.getCause();
        String innerMsg;
        if (cause != null && close.add(cause)) {
            innerMsg = retrieveDetailedErrorMessage(cause, close);
        } else {
            innerMsg = null;
        }

        if (msg == null) {
            return innerMsg;
        }
        if (innerMsg == null) {
            return msg;
        }
        return msg + ": " + innerMsg;
    }

    private static String retrieveFailedType(Throwable t, Set<Throwable> close) {
        if (!close.add(t)) {
            return "Unknown";
        }
        if (t instanceof AbortException ||
            t instanceof ExitException ||
            t.getClass() == Exception.class ||
            t.getClass() == RuntimeException.class) {

            if (t.getCause() != null) {
                return retrieveFailedType(t.getCause(), close);
            }
            return "Unknown";
        }

        return t.getClass().getSimpleName();
    }
}
