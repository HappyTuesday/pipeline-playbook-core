package com.yit.deploy.core.model;

import com.yit.deploy.core.exceptions.ExitException;
import com.yit.deploy.core.exceptions.ProcessExecutionException;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.utils.Utils;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import org.apache.tools.ant.filters.StringInputStream;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ProcessLauncher {
    private static final Logger logger = Logger.getLogger(ProcessLauncher.class.getName());

    private PipelineScript script;
    private FilePath pwd;
    private List<String> cmd;
    private ProcessLauncher pipeTo;
    private InputStream input;
    private EnvVars envVars;
    private boolean verbose = false;
    private int timeout = -1;
    private Set<Integer> allowedCodes = Collections.singleton(0);

    public ProcessLauncher() {}

    public ProcessLauncher(List<String> cmd) {
        this.cmd = cmd;
    }

    public ProcessLauncher(String bin, String ... args) {
        this.cmd = Lambda.concat(bin, args);
    }

    public List<String> getCmd() {
        return cmd;
    }

    public ProcessLauncher script(PipelineScript script) {
        this.script = script;
        return this;
    }

    public ProcessLauncher pwd(FilePath pwd) {
        this.pwd = pwd;
        return this;
    }

    public ProcessLauncher input(InputStream input) {
        this.input = input;
        return this;
    }

    public ProcessLauncher input(String input) {
        return this.input(new StringInputStream(input, Utils.DefaultCharset.name()));
    }

    public ProcessLauncher input(byte[] input) {
        return input(new ByteArrayInputStream(input));
    }

    public ProcessLauncher input(FilePath file) {
        try {
            return this.input(file.read());
        } catch (IOException | InterruptedException e) {
            throw new ExitException(e);
        }
    }

    public ProcessLauncher cmd(String bin, String ... args) {
        cmd = Lambda.concat(bin, args);
        return this;
    }

    public ProcessLauncher cmd(List<String> value) {
        // ensure that all items are of string type
        cmd = Lambda.map(value, String::valueOf);
        return this;
    }

    public ProcessLauncher bash(String shell) {
        cmd = Arrays.asList("bash", "-eu", "-o", "pipefail", "-c", shell);
        return this;
    }

    public ProcessLauncher allowedCodes(Set<Integer> value) {
        allowedCodes = value;
        return this;
    }

    public ProcessLauncher allowedCodes(Integer ... value) {
        return allowedCodes(new HashSet<>(Arrays.asList(value)));
    }

    public ProcessLauncher verbose(boolean value) {
        verbose = value;
        return this;
    }

    /**
     * timeout of the process execution, in milliseconds
     * @param timeout
     * @return
     */
    public ProcessLauncher timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public ProcessLauncher pipeTo(ProcessLauncher pipeTo) {
        this.pipeTo = pipeTo;
        return this;
    }

    public ProcessLauncher setup(Consumer<ProcessLauncherDslContext> dsl) {
        dsl.accept(new ProcessLauncherDslContext(this));
        return this;
    }

    public ProcessLauncher setup(@DelegatesTo(value = ProcessLauncherDslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<Object> closure) {
        Closures.with(new ProcessLauncherDslContext(this), closure);
        return this;
    }

    public static class ProcessLauncherDslContext implements Serializable {

        private ProcessLauncher launcher;

        public ProcessLauncherDslContext(ProcessLauncher launcher) {
            this.launcher = launcher;
        }

        public ProcessLauncherDslContext pwd(FilePath value) {
            launcher.pwd = value;
            return this;
        }

        public ProcessLauncherDslContext bash(String shell) {
            launcher.bash(shell);
            return this;
        }

        public ProcessLauncherDslContext cmd(String bin, String ... args) {
            launcher.cmd(bin, args);
            return this;
        }

        public ProcessLauncherDslContext cmd(List<String> value) {
            launcher.cmd(value);
            return this;
        }

        public ProcessLauncherDslContext input(InputStream input) {
            launcher.input = input;
            return this;
        }

        public ProcessLauncherDslContext input(String value) {
            try {
                return input(new ByteArrayInputStream(value.getBytes(Utils.DefaultCharset.name())));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }

        public ProcessLauncherDslContext input(byte[] value) {
            return input(new ByteArrayInputStream(value));
        }

        public ProcessLauncherDslContext input(FilePath file) {
            try {
                return input(file.read());
            } catch (IOException | InterruptedException e) {
                throw new ExitException(e);
            }
        }

        public ProcessLauncherDslContext envVar(String name, String value) {
            if (launcher.envVars == null) launcher.envVars = new EnvVars();
            launcher.envVars.put(name, value);
            return this;
        }

        public ProcessLauncherDslContext envVars(EnvVars value) {
            launcher.envVars = value;
            return this;
        }

        public ProcessLauncherDslContext verbose() {
            launcher.verbose = true;
            return this;
        }

        public ProcessLauncherDslContext pipeTo(ProcessLauncher pipeTo) {
            launcher.pipeTo(pipeTo);
            return this;
        }
    }

    public ProcessExecutionStatus executeReturnStatus() {
        return execute(true);
    }

    public byte[] executeReturnOutput() {
        ProcessExecutionStatus status = execute(true);
        if (!allowedCodes.contains(status.getCode())) {
            throw new ProcessExecutionException(status);
        }
        return status.getStdout();
    }

    public List<String> executeReturnLines() {
        return Lambda.tokenize(executeReturnText(), "\n");
    }

    public String executeReturnText() {
        return executeReturnText(true);
    }

    public String executeReturnText(boolean trim) {
        ProcessExecutionStatus status = execute(true);
        if (!allowedCodes.contains(status.getCode())) {
            throw new ProcessExecutionException(status);
        }
        String text = status.getText();
        if (trim) {
            text = text.trim();
        }
        return text;
    }

    public void executePrintOutput() {
        ProcessExecutionStatus status = execute(false);
        if (!allowedCodes.contains(status.getCode())) {
            throw new ProcessExecutionException(status);
        }
    }

    public void executeIgnoreOutput() {
        ProcessExecutionStatus status = execute(true);
        if (!allowedCodes.contains(status.getCode())) {
            throw new ProcessExecutionException(status);
        }
    }

    public ProcessExecutionStatus execute(boolean returnOutput) {
        try {
            return executeRecursive(returnOutput, null);
        } catch (InterruptedException | IOException e) {
            throw new ExitException(e);
        }
    }

    private ProcessExecutionStatus executeRecursive(boolean returnOutput, InputStream stdin) throws InterruptedException, IOException {

        InputStream finalStdin;
        if (stdin == null) {
            finalStdin = input; // first of the pipe
        } else {
            finalStdin = stdin;
        }

        PrintStream stream = getTaskListener().getLogger();

        ByteArrayOutputStream stdout, stderr;
        OutputStream finalStdout, finalStderr;

        if (pipeTo != null) {
            finalStdout = stderr = stdout = null;
            finalStderr = stream;
        } else if (returnOutput || script == null) {
            finalStdout = stdout = new ByteArrayOutputStream();
            finalStderr = stderr = new ByteArrayOutputStream();
        } else {
            stderr = stdout = null;
            finalStderr = finalStdout = stream;
        }

        Proc proc = startProcess(finalStdin, finalStdout, finalStderr);

        ProcessExecutionStatus status = null;
        if (pipeTo != null) {
            status = pipeTo.executeRecursive(returnOutput, proc.getStdout());
        }

        ProcessExecutionStatus selfStatus = finishProcess(proc, stdout, stderr);

        // the last status in the pipe is the final status
        if (status == null) {
            status = selfStatus;
        } else if (selfStatus.getCode() != 0) {
            status.setCode(selfStatus.getCode());
        }

        // close input whether we used it or not
        if (input != null) input.close();
        // we actively close the stdin created by outer call
        if (stdin != null) stdin.close();

        if (stdout != null) stdout.close();
        if (stderr != null) stderr.close();

        return status;
    }

    private Proc startProcess(InputStream stdin, OutputStream stdout, OutputStream stderr)
        throws IOException, InterruptedException {

        EnvVars envVars = this.envVars == null ? new EnvVars() : this.envVars;

        VirtualChannel channel = null;
        if (pwd != null) {
            channel = pwd.getChannel();
        } else if (script != null && script.getNodeName() != null && !script.getNodeName().equals("master")) {
            Computer computer = script.getComputer();
            if (computer != null) {
                channel = computer.getChannel();
            }
        }

        TaskListener verboseListener = verbose ? getTaskListener() : TaskListener.NULL;

        Launcher launcher;
        if (channel == null || channel.equals(FilePath.localChannel)) {
            launcher = new Launcher.LocalLauncher(verboseListener);
        } else {
            launcher = new Launcher.RemoteLauncher(verboseListener, channel, true);
        }

        Launcher.ProcStarter p = launcher.launch().cmds(cmd).envs(envVars);

        if (stdout == null) {
            p.readStdout();
        } else {
            p.stdout(stdout);
        }

        if (stderr == null) {
            p.readStderr();
        } else {
            p.stderr(stderr);
        }

        if (stdin != null) {
            p.stdin(stdin);
        }

        String pwd = null;
        if (this.pwd != null) {
            pwd = this.pwd.getRemote();
        } else if (script != null && script.getWorkspaceFilePath().exists()) {
            pwd = script.getWorkspace();
        }

        if (pwd != null) {
            p.pwd(pwd);
        }

        logger.info("EXECUTE: " + String.join(" ", p.cmds()));
        return p.start();
    }

    private ProcessExecutionStatus finishProcess(Proc proc, ByteArrayOutputStream stdout, ByteArrayOutputStream stderr)
        throws IOException, InterruptedException {

        int code;
        if (timeout < 0) {
            code = proc.join();
        } else {
            code = proc.joinWithTimeout(timeout, TimeUnit.MILLISECONDS, getTaskListener());
        }

        ProcessExecutionStatus s = new ProcessExecutionStatus();
        s.setLauncher(this);
        s.setStdout(stdout == null ? new byte[0] : stdout.toByteArray());
        s.setStderr(stderr == null ? new byte[0] : stderr.toByteArray());
        s.setCode(code);

        return s;
    }

    private TaskListener getTaskListener() {
        return script != null ? script.getSteps().getTaskListener() : TaskListener.NULL;
    }
}
