package com.yit.deploy.core.support;

import com.yit.deploy.core.docker.DockerImage;
import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.exceptions.ExitException;
import com.yit.deploy.core.exceptions.ExitPlayBookException;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.*;
import com.yit.deploy.core.utils.IO;
import com.yit.deploy.core.utils.Utils;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.steps.*;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import hudson.AbortException;
import hudson.FilePath;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by nick on 06/11/2017.
 */
public interface StepsSupport extends ExceptionSupport {

    JobExecutionContext getExecutionContext();

    default Project getProject() {
        return getExecutionContext().getProject();
    }

    default Environment getEnv() {
        return getExecutionContext().getEnv();
    }

    default PipelineScript getScript() {
        return getExecutionContext().getScript();
    }

    default PipelineScriptSteps getSteps() {
        return getScript().getSteps();
    }

    default DeployModelTable getModelTable() {
        return getExecutionContext().getModelTable();
    }

    default Object mail(@DelegatesTo(value = MailStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        return new MailStep(getExecutionContext()).setup(closure).execute();
    }

    default Object mailToCurrentUser(@DelegatesTo(value = MailStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        if (getEnv().isLocalEnv()) return null;

        if (getExecutionContext().getScript().getCurrentUser().getEmailAddress() == null) {
            getExecutionContext().getScript().info("we could not determine the email address of current user, so sending email to current user is skipped.");
            return null;
        } else {
            MailStep step = new MailStep(getExecutionContext());
            new MailStep.DslContext(step).toCurrentUser();
            return step.setup(closure).execute();
        }
    }

    default Object userContent(@DelegatesTo(value = UserContentStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        return new UserContentStep(getExecutionContext()).setup(closure).execute();
    }

    default String j2Template(String content) {
        return j2Template(content, null, Collections.emptyMap());
    }

    default String j2Template(String content, String templateName) {
        return j2Template(content, templateName, Collections.emptyMap());
    }

    default String j2Template(String content, String templateName, Map<String, Object> binding) {
        return new JinjaTemplate(getExecutionContext()).render(content, templateName, binding);
    }

    default String j2Template(FilePath path) {
        try {
            return j2Template(path.readToString(), path.getRemote());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    default DisconfStep createDisconfStep() {
        DisconfStep step = new DisconfStep(getExecutionContext());
        new DisconfStep.DslContext(step)
                .apiHost(getExecutionContext().getVariable("DISCONF_MAIN_HOST", String.class))
                .apiPort(getExecutionContext().getVariable("DISCONF_PORT", Integer.class))
                .apiAdminPassword(getExecutionContext().getVariable("DISCONF_ADMIN_PASSWORD", String.class))
                .dbConn((MysqlDBConnection) getExecutionContext().getVariable("DISCONF_DB_CONNECTION"))
                .appName(getExecutionContext().getVariable("DISCONF_APP_NAME", String.class))
                .envName(getEnv().getName());
        return step;
    }

    default Object uri(@DelegatesTo(value = UriStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        return new UriStep(getExecutionContext()).setup(closure).execute();
    }

    default void uploadToOSS(String bucketNameValue, String ossFolderValue, FilePath localFolderValue) {
        UploadToOSSStep step = new UploadToOSSStep(getExecutionContext());
        new UploadToOSSStep.DslContext(step).bucketName(bucketNameValue).ossFolder(ossFolderValue).localFolder(localFolderValue);
        step.execute();
    }

    default Object aliDNS(@DelegatesTo(value = AliDNSStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        return new AliDNSStep(getExecutionContext()).setup(closure).execute();
    }

    default void uploadToQiniu(@DelegatesTo(value = UploadToQiniuStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        UploadToQiniuStep step = new UploadToQiniuStep(getExecutionContext());
        step.setup(closure);
        step.execute();
    }

    default void modifyRdsSecurityIps(@DelegatesTo(value = ModifySecurityIpsStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        ModifySecurityIpsStep step = new ModifySecurityIpsStep(getExecutionContext());
        new ModifySecurityIpsStep.DslContext(step).instanceType("rds");
        step.setup(closure).execute();
    }

    default void modifyRedisSecurityIps(@DelegatesTo(value = ModifySecurityIpsStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        ModifySecurityIpsStep step = new ModifySecurityIpsStep(getExecutionContext());
        new ModifySecurityIpsStep.DslContext(step).instanceType("redis");
        step.setup(closure).execute();
    }

    default void modifySlbAclAddSecurityIps(@DelegatesTo(value = ModifySecurityIpsStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        ModifySecurityIpsStep step = new ModifySecurityIpsStep(getExecutionContext());
        new ModifySecurityIpsStep.DslContext(step).instanceType("slbAclAdd");
        step.setup(closure).execute();
    }

    default void modifySlbAclDelSecurityIps(@DelegatesTo(value = ModifySecurityIpsStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        ModifySecurityIpsStep step = new ModifySecurityIpsStep(getExecutionContext());
        new ModifySecurityIpsStep.DslContext(step).instanceType("slbAclDel");
        step.setup(closure).execute();
    }

    default void modifyMongoDBSecurityIps(@DelegatesTo(value = ModifySecurityIpsStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        ModifySecurityIpsStep step = new ModifySecurityIpsStep(getExecutionContext());
        new ModifySecurityIpsStep.DslContext(step).instanceType("mongodb");
        step.setup(closure).execute();
    }

    /**
     * executeReturnText bash command on jenkins node
     * @param shell command to execute
     */
    default void sh(String shell) {
        new ProcessLauncher().bash(shell).script(getScript()).executePrintOutput();
    }

    default String shReturnText(String shell) {
        return new ProcessLauncher().bash(shell).script(getScript()).executeReturnText();
    }

    default void dir(String path, Runnable runnable) {
        getSteps().dir(path, runnable);
    }

    default void parallel(Map<String, Runnable> map) {
        parallel(map, -1);
    }

    default void parallel(Map<String, Runnable> map, int stepSize) {
        if (stepSize <= 0) {
            getSteps().parallel(map);
        } else {
            List<String> keys = new ArrayList<>(map.keySet());
            for (int i = 0; true ; i++) {
                List<String> selectedKeys = new ArrayList<>();
                for (int j = 0; j < stepSize && j + i * stepSize < keys.size(); j++) {
                    selectedKeys.add(keys.get(j + i * stepSize));
                }
                if (selectedKeys.isEmpty()) break;

                getSteps().parallel(Lambda.toMap(selectedKeys, map::get));
            }
        }
    }

    default <T> void parallel(Collection<T> c, int stepSize, Function<T, String> title, Consumer<T> body) {
        Map<String, Runnable> map = new HashMap<>(c.size());
        for (T t : c) {
            map.put(title.apply(t), () -> body.accept(t));
        }
        parallel(map, stepSize);
    }

    default <T> void parallel(Collection<T> c, Function<T, String> title, Consumer<T> body) {
        parallel(c, 0, title, body);
    }

    default <T> void parallel(Collection<T> c, Consumer<T> body) {
        parallel(c, 0, Objects::toString, body);
    }

    default <K, V> void parallel(Map<K, V> map, int stepSize, Consumer<Map.Entry<K, V>> body) {
        parallel(map.entrySet(), stepSize, entry -> entry.getKey() != null ? entry.getKey().toString() : null, body);
    }

    default <K, V> void parallel(Map<K, V> map, Consumer<Map.Entry<K, V>> body) {
        parallel(map, 0, body);
    }

    default void echo(Object obj) {
        getSteps().echo(obj);
    }

    default void println(Object msg) {
        getSteps().echo(msg);
    }

    default void error(Object msg) throws AbortException {
        getScript().error(msg);
    }

    default void warn(Object msg) {
        getScript().warn(msg);
    }

    default void info(Object msg) {
        getScript().info(msg);
    }

    default void debug(Object msg) {
        getScript().debug(msg);
    }

    default String makeLink(String url) {
        return getSteps().makeLink(url, url);
    }

    default String makeLink(String url, String text) {
        return getSteps().makeLink(url, text);
    }

    @Nonnull
    default String input(@Nonnull String message, @Nonnull List<String> choices) throws AbortException {
        return getSteps().input(message, choices);
    }

    @Nonnull
    default String input(@Nonnull String message, String ... choices) throws AbortException {
        return input(message, Arrays.asList(choices));
    }

    /**
     * executeReturnText process on current jenkins node
     * @param bin
     * @param args
     * @return
     */
    default String executeReturnText(String bin, String ... args) {
        return new ProcessLauncher(Lambda.concat(bin, args)).script(getScript()).executeReturnText();
    }

    default Object waitFor(@DelegatesTo(value = WaitForStep.DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        return new WaitForStep(getExecutionContext()).setup(closure).execute();
    }

    default void userConfirm(String message) {
        getScript().userConfirm(message);
    }

    default FilePath createTempFile() {
        return getScript().createTempFile("executing", "execute_play");
    }

    default FilePath createTextTempFile(String content) {
        return getScript().createTextTempFile(content, "executing", "execute_play");
    }

    default <T> T withTextTempFile(String content, @ClosureParams(value = SimpleType.class, options = "hudson.FilePath") Closure<T> closure) {
        FilePath file = createTextTempFile(content);
        try {
            return closure.call(file);
        } finally {
            deleteRecuirsiveQuietly(file);
        }
    }

    default <T> T withTextTempFile(String content, String ext, @ClosureParams(value = SimpleType.class, options = "hudson.FilePath") Closure<T> closure) {
        FilePath file = getScript().createTextTempFile(content, "executing", "execute_play", ext);
        try {
            return closure.call(file);
        } finally {
            deleteRecuirsiveQuietly(file);
        }
    }

    default FilePath createTempDir() {
        return getScript().createTempDir("executing", "execute_play");
    }

    default void withTempDir(@ClosureParams(value = SimpleType.class, options = {"hudson.FilePath"}) Closure closure) {
        FilePath file = createTempDir();
        try {
            closure.call(file);
        } finally {
            deleteRecuirsiveQuietly(file);
        }
    }

    default FilePath createWorkspaceTempFile() {
        return getScript().createWorkspaceTempFile("execute_play");
    }

    default FilePath createWorkspaceTextTempFile(String content) {
        return getScript().createWorkspaceTextTempFile(content, "execute_play");
    }

    default void withWorkspaceTextTempFile(String content, Consumer<FilePath> consumer) {
        FilePath file = createWorkspaceTextTempFile(content);
        try {
            consumer.accept(file);
        } finally {
            deleteRecuirsiveQuietly(file);
        }
    }

    default void withWorkspaceTextTempFiles(List<String> contents, Consumer<List<FilePath>> consumer) {
        List<FilePath> files = new ArrayList<>(contents.size());
        RuntimeException exception = null;
        for (String content : contents) {
            try {
                files.add(createWorkspaceTextTempFile(content));
            } catch (RuntimeException e) {
                exception = e;
                break;
            }
        }

        try {
            if (exception == null) {
                consumer.accept(files);
            }
        } finally {
            for (int i = files.size() - 1; i >= 0; i--) {
                deleteRecuirsiveQuietly(files.get(i));
            }
        }

        if (exception != null) throw exception;
    }

    default FilePath createWorkspaceTempDir() {
        return getScript().createWorkspaceTempDir("execute_play");
    }

    default void withWorkspaceTempDir(Consumer<FilePath> consumer) {
        FilePath file = createWorkspaceTempDir();
        try {
            consumer.accept(file);
        } finally {
            deleteRecuirsiveQuietly(file);
        }
    }

    default void deleteRecuirsiveQuietly(FilePath path) {
        try {
            path.deleteRecursive();
        } catch (IOException | InterruptedException e) {
            getScript().warn("delete file " + path + " faild: " + e.getMessage());
        }
    }

    default void deleteFilesIfExists(FilePath ... paths) {
        try {
            for (FilePath path : paths) {
                if (path.exists()) {
                    path.deleteRecursive();
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    default void executeProject(@Nonnull Project project) {
        executeProject(project, null);
    }

    default void executeProject(
        @Nonnull Project project,
        @Nullable
        @DelegatesTo(value = JobExecutionDsl.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "com.yit.deploy.core.model.Job")
            Closure closure) {

        executeProject(project.getProjectName(), closure);
    }

    default void executeProject(@Nonnull String projectName) {
        executeProject(projectName, null);
    }

    default void executeProject(
        @Nonnull
            String projectName,
        @Nullable
        @DelegatesTo(value = JobExecutionDsl.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "com.yit.deploy.core.model.Job")
            Closure closure) {

        Job job = getModelTable().findJob(projectName, getEnv().getName());

        if (job == null) {
            getScript().warn("project " + projectName + " does not exist, skipping to execute it");
        } else {
            executeJob(job, dsl -> {
                dsl.filterPlays();
                if (closure != null) {
                    Closures.with(dsl, closure, job);
                }
            });
        }
    }

    default void executeJob(Job job, Consumer<JobExecutionDsl> descriptor) {
        JobExecutionDsl dsl = new JobExecutionDsl().plays(job.getPlays()).servers(job.getServers());
        descriptor.accept(dsl);

        assert dsl._plays != null && dsl._tasksToSkip != null && dsl._servers != null && dsl._retiredServers != null;

        if (dsl._filterPlays) {
            dsl._plays = Lambda.intersect(dsl._plays, job.getAllPlays());
        }

        DeploySpec spec = new DeploySpec(dsl._plays, dsl._tasksToSkip, dsl._servers,
            dsl._retiredServers, dsl._userParameters);

        for (int i = dsl._retryTimes; i >= 0; i--) {
            Build build = getExecutionContext().getBuild().createNewBuild(job, spec);
            try {
                build.execute();
                break;
            } catch (Exception e) {
                if (i > 0 && !(ExitException.belongsTo(e))) {
                    build.getScript().warn("execute project %s failed: %s. retry ...",
                        job.getProject().getProjectName(), e.getMessage());
                } else if (dsl._ignoreFailure) {
                    build.getScript().warn("execute project %s failed: %s",
                        job.getProject().getProjectName(), e.getMessage());
                    break;
                } else {
                    throw e;
                }
            }
        }
    }

    class JobExecutionDsl {
        private List<String> _plays;
        private List<String> _tasksToSkip = new ArrayList<>();
        private List<String> _servers;
        private List<String> _retiredServers = new ArrayList<>();
        private Map<String, Object> _userParameters;
        private boolean _filterPlays;
        private boolean _ignoreFailure;
        private int _retryTimes = 0;

        public JobExecutionDsl plays(List<String> value) {
            _plays = value;
            return this;
        }

        public JobExecutionDsl plays(String ... value) {
            return plays(Arrays.asList(value));
        }

        public JobExecutionDsl tasksToSkip(List<String> value) {
            _tasksToSkip = value;
            return this;
        }

        public JobExecutionDsl tasksToSkip(String ... value) {
            return tasksToSkip(Arrays.asList(value));
        }

        public JobExecutionDsl servers(List<String> value) {
            _servers = value;
            return this;
        }

        public JobExecutionDsl servers(String ... value) {
            return servers(Arrays.asList(value));
        }

        public JobExecutionDsl retiredServers(List<String> value) {
            _retiredServers = value;
            return this;
        }

        public JobExecutionDsl userParameterSource(Map<String, Object> value) {
            _userParameters = value;
            return this;
        }

        public JobExecutionDsl filterPlays() {
            return filterPlays(true);
        }

        public JobExecutionDsl filterPlays(boolean value) {
            _filterPlays = value;
            return this;
        }

        public JobExecutionDsl ignoreFailure() {
            _ignoreFailure = true;
            return this;
        }

        public JobExecutionDsl retryTimes(int value) {
            _retryTimes = value;
            return this;
        }
    }

    default void executeProjects(@Nonnull Iterable<Project> ps) {
        executeProjects(ps, null);
    }

    default void executeProjects(
        @Nonnull
            Iterable<Project> ps,
        @Nullable
        @DelegatesTo(value = JobExecutionDsl.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "com.yit.deploy.core.model.Job")
            Closure closure) {

        executeProjectsWithName(Lambda.map(ps, Project::getProjectName), closure);
    }

    default void executeProjectsWithName(@Nonnull Iterable<String> ps) {
        executeProjectsWithName(ps, null);
    }

    default void executeProjectsWithName(
        @Nonnull
            Iterable<String> ps,
        @Nullable
        @DelegatesTo(value = JobExecutionDsl.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "com.yit.deploy.core.model.Job")
            Closure closure) {

        Map<String, Runnable> map = new HashMap<>();
        for (String p : ps) {
            map.put(p, () -> executeProject(p, closure));
        }
        parallel(map);
    }

    default void scheduleProject(@Nonnull String projectName) {
        scheduleProject(projectName, null);
    }

    default void scheduleProject(
        @Nonnull
            String projectName,
        @Nullable
        @DelegatesTo(value = Job.ScheduleDsl.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "com.yit.deploy.core.model.Job")
            Closure closure) {

        Job job = getModelTable().findJob(projectName, getEnv().getName());

        if (job == null) {
            getScript().warn("project " + projectName + " does not exist, skipping to schedule it.");
        } else {
            job.schedule(dsl -> {
                dsl.script(getScript());
                dsl.filterPlays();
                if (closure != null) {
                    Closures.with(dsl, closure, job);
                }
            });
        }
    }

    default void scheduleProject(
        @Nonnull
            Project project,
        @Nullable
        @DelegatesTo(value = Job.ScheduleDsl.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "com.yit.deploy.core.model.Job")
            Closure closure) {

        scheduleProject(project.getProjectName(), closure);
    }

    default void scheduleProjectsWithName(
        @Nonnull
            Iterable<String> ps,
        @Nullable
        @DelegatesTo(value = Job.ScheduleDsl.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "com.yit.deploy.core.model.Job")
            Closure closure) {

        Map<String, Runnable> map = new HashMap<>();
        for (String p : ps) {
            map.put(p, () -> scheduleProject(p, closure));
        }
        parallel(map);
    }

    default void scheduleProjects(
        @Nonnull
            Iterable<Project> ps,
        @Nullable
        @DelegatesTo(value = Job.ScheduleDsl.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "com.yit.deploy.core.model.Job")
            Closure closure) {

        scheduleProjectsWithName(Lambda.map(ps, Project::getProjectName), closure);
    }

    default DockerImage buildDockerImage(String imageName) {
        return buildDockerImage(imageName, ".", "Dockerfile");
    }

    default DockerImage buildDockerImage(String imageName, String folderName, String dockerfile) {
        return DockerImage.build(getExecutionContext(), imageName, folderName, dockerfile);
    }

    /**
     * exit playbook with message
     * @param message message used to exit
     */
    default void exitPlaybook(String message) {
        throw new ExitPlayBookException(message);
    }
}
