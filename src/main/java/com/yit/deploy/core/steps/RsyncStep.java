package com.yit.deploy.core.steps;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.model.ProcessLauncher;
import com.yit.deploy.core.global.resource.Resources;
import com.yit.deploy.core.model.ConnectionChannel;
import com.yit.deploy.core.model.Host;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.model.StatStruct;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import hudson.FilePath;

import java.io.IOException;
import java.util.*;

/**
 * Created by nick on 14/09/2017.
 */
public class RsyncStep extends AbstractStep {
    private Host sourceHost; // null means localhost
    private String sourceFile;
    private Host targetHost;
    private String targetFile;
    private String tempDir;

    private List<String> options = new ArrayList<>(Collections.singletonList("-arc"));

    /**
     * specify fetch files or push files
     */
    private boolean fetch;

    public RsyncStep(JobExecutionContext context) {
        super(context);
    }

    public RsyncStep setup(@DelegatesTo(value = DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    /**
     * Execute this step, providing a pipeline-script and a variable context
     */
    @Override
    protected Object executeOverride() throws Exception {
        assert sourceFile != null && targetFile != null;

        if (fetch) {
            transfer(targetHost, targetFile, sourceHost, sourceFile);
        } else {
            transfer(sourceHost, sourceFile, targetHost, targetFile);
        }
        return null;
    }

    private void transfer(Host sourceHost, String sourceFile, Host targetHost, String targetFile) throws Exception {
        if (isRemoteHost(sourceHost) && isRemoteHost(targetHost)) {
            if (tempDir == null) {
                withTempFolder(temp -> transferViaTempDir(temp, sourceHost, sourceFile, targetHost, targetFile));
            } else {
                FilePath temp = getScript().getFilePath(tempDir);
                Resources.path.acquire(tempDir);
                try {
                    makeTempDir(temp);
                    transferViaTempDir(temp, sourceHost, sourceFile, targetHost, targetFile);
                } finally {
                    Resources.path.release(tempDir);
                }
            }
        } else {
            transferSimple(sourceHost, sourceFile, targetHost, targetFile);
        }
    }

    private void transferViaTempDir(FilePath temp, Host sourceHost, String sourceFile, Host targetHost, String targetFile) {
        String sourceTemp = getTempFolderName(sourceFile);
        String targetTemp = getTempFolderName(targetFile);
        StatStruct targetFileStat = getRemoteFileStats(targetHost, targetFile);

        try {
            if (!Objects.equals(sourceTemp, targetTemp) && temp.child(sourceTemp).exists()) {
                deleteRecursive(temp.child(targetTemp));
                temp.child(sourceTemp).renameTo(temp.child(targetTemp));
            }

            if (targetFileStat.isExists() && targetFileStat.isDirectory()) {
                makeTempDir(temp.child(targetTemp));
            }

            transferSimple(sourceHost, sourceFile, null, temp.child(targetTemp).getRemote());

            if (!Objects.equals(sourceTemp, targetTemp)) {
                deleteRecursive(temp.child(sourceTemp));
                temp.child(targetTemp).renameTo(temp.child(sourceTemp));
            }

            transferSimple(null, temp.child(sourceTemp).getRemote(), targetHost, targetFile);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteRecursive(FilePath path) throws IOException, InterruptedException {
        if (path.exists()) {
            path.deleteRecursive();
        }
    }

    private void makeDir(FilePath path) throws IOException, InterruptedException {
        if (!path.exists()) {
            path.mkdirs();
        }
    }

    private void makeTempDir(FilePath path) throws IOException, InterruptedException {
        if (path.exists() && !path.isDirectory()) {
            path.delete();
        }
        makeDir(path);
    }

    private void transferSimple(Host sourceHost, String sourceFile, Host targetHost, String targetFile) {
        Host h = isRemoteHost(sourceHost) ? sourceHost : targetHost;
        Resources.ssh.acquire(h.getName());
        try {
            String shell = String.format("rsync -e 'ssh -o StrictHostKeyChecking=no -p %d' %s '%s' '%s'", h.getPort(), String.join(" ", options), getRsyncFile(sourceHost, sourceFile), getRsyncFile(targetHost, targetFile));
            new ProcessLauncher().script(getScript()).bash(shell).executePrintOutput();
        } finally {
            Resources.ssh.release(h.getName());
        }
    }

    private StatStruct getRemoteFileStats(Host host, String file) {
        StatStep step = new StatStep(getExecutionContext());
        new StatStep.DslContext(step).targetHost(host).target(file);
        return step.execute(StatStruct.class);
    }

    /**
     * the rsync command consider if the target or source folder name ends with '/'.
     * so while using temp folder to archive rsync between two remote servers, we must imitate the target folder when rsync to local
     * and imitate source folder when rsync from local.
     */
    private static String getTempFolderName(String exampleFilePath) {
        int startsFrom = exampleFilePath.length() - 1;
        if (exampleFilePath.endsWith("/")) {
            startsFrom--;
        }
        int index = exampleFilePath.lastIndexOf('/', startsFrom);
        return exampleFilePath.substring(index + 1);
    }

    private static boolean isRemoteHost(Host host) {
        return host != null && !ConnectionChannel.local.equals(host.getChannel());
    }

    private static String getRsyncFile(Host host, String file) {
        if (isRemoteHost(host)) {
            return String.format("%s@%s:%s", host.getUser(), host.getName(), file);
        } else {
            return file;
        }
    }

    public static class DslContext {

        private RsyncStep step;

        public DslContext(RsyncStep step) {
            this.step = step;
        }

        public DslContext targetHost(Host value) {
            step.targetHost = value;
            return this;
        }

        public DslContext target(String value) {
            step.targetFile = value;
            return this;
        }

        public DslContext sourceHost(Host value) {
            step.sourceHost = value;
            return this;
        }

        public DslContext source(String value) {
            step.sourceFile = value;
            return this;
        }

        public DslContext option(String ... value) {
            step.options.addAll(Arrays.asList(value));
            return this;
        }

        public DslContext fetch() {
            step.fetch = true;
            return this;
        }

        public DslContext tempDir(String value) {
            step.tempDir = value;
            return this;
        }

        public DslContext delete() {
            return option("--delete");
        }

        public DslContext verbose() {
            return option("--verbose");
        }

        public DslContext quiet() {
            return option("--quiet");
        }

        public DslContext archive() {
            return option("--archive");
        }
    }
}
