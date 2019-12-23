package com.yit.deploy.core.steps;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.exceptions.UnsupportedPlatformException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.*;
import com.yit.deploy.core.exceptions.ProcessExecutionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by nick on 14/09/2017.
 */
public abstract class AbstractFileStep extends AbstractStep {
    protected Host targetHost;
    protected String path;

    public AbstractFileStep(JobExecutionContext context) {
        super(context);
    }

    public StatStruct statFile(String file) {
        return statFileCommon(file, false).get(0);
    }

    public List<StatStruct> statFilesInDirectory(String file) {
        return statFileCommon(file, true);
    }

    private List<StatStruct> statFileCommon(String file, boolean listChildren) {
        String del = "|";
        if (file.contains(del)) {
            throw new IllegalArgumentException("currently the file name could not contain the reserved string " + del);
        }

        String glob = listChildren ? "/*" : "";
        List<StatStruct> result = new ArrayList<>();

        try {
            if (targetHost.isDarwin()) {
                List<String> lines = Lambda.tokenize(executeShellReturnText("stat -f '%N" + del + "%p" + del + "%Su" + del + "%Sg%n' '" + file + "'" + glob), "\n");
                for (String line : lines) {
                    List<String> s = Lambda.tokenize(line, del);
                    char i = s.get(1).charAt(0);
                    FileType ft;
                    if (i == '4') {
                        ft = FileType.directory;
                    } else if (i == '1') {
                        ft = FileType.file;
                    } else {
                        throw new UnsupportedFileTypeException(i);
                    }

                    StatStruct ss = new StatStruct();
                    ss.setExists(true);
                    ss.setPath(s.get(0));
                    ss.setType(ft);
                    ss.setMode(s.get(1).substring(1));
                    ss.setOwner(s.get(2));
                    ss.setGroup(s.get(3));

                    result.add(ss);
                }
            } else if (targetHost.isLinux()) {
                List<String> lines = Lambda.tokenize(executeShellReturnText("stat --printf \"%n" + del + "%F" + del + "%a" + del + "%U" + del + "%G\n\" '" + file + "'" + glob), "\n");
                for (String line : lines) {
                    List<String> s = Lambda.tokenize(line, del);
                    String t = s.get(1);
                    FileType ft;
                    if ("directory".equals(t)) {
                        ft = FileType.directory;
                    } else if ("regular file".equals(t) || "regular empty file".equals(t)) {
                        ft = FileType.file;
                    } else {
                        throw new UnsupportedFileTypeException(t);
                    }

                    StatStruct ss = new StatStruct();
                    ss.setExists(true);
                    ss.setPath(s.get(0));
                    ss.setType(ft);
                    ss.setMode(s.get(2));
                    ss.setOwner(s.get(3));
                    ss.setGroup(s.get(4));

                    result.add(ss);
                }
            } else {
                throw new UnsupportedPlatformException(targetHost.getUname(), "could not stat file " + file);
            }
        } catch (ProcessExecutionException e) {
            if (e.getError().contains("No such file or directory")) {
                if (listChildren) {
                    return Collections.emptyList();
                } else {
                    StatStruct ss = new StatStruct();
                    ss.setExists(false);
                    return Collections.singletonList(ss);
                }
            }
            throw e;
        }
        return result;
    }

    protected void executeShell(String shell) {
        getRemoteLauncher(shell).executePrintOutput();
    }

    protected String executeShellReturnText(String shell) {
        return executeShellReturnText(shell, true);
    }

    protected String executeShellReturnText(String shell, boolean trim) {
        return getRemoteLauncher(shell).executeReturnText(trim);
    }

    protected byte[] executeShellReturnOutput(String shell) {
        return getRemoteLauncher(shell).executeReturnOutput();
    }

    protected ProcessLauncher getRemoteLauncher(String shell) {
        return new RemoteProcessLauncher(targetHost, shell).launcher.script(getScript());
    }

    public static class UnsupportedFileTypeException extends RuntimeException {

        public UnsupportedFileTypeException(Object type) {
            this(type, "");
        }

        public UnsupportedFileTypeException(Object type, String message) {
            super("unsupported file type " + type + " is found. " + message);
        }
    }
}
