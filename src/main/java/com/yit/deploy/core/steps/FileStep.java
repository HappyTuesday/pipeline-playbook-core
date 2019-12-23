package com.yit.deploy.core.steps;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.Environment;
import com.yit.deploy.core.model.FileStatus;
import com.yit.deploy.core.model.FileType;
import com.yit.deploy.core.model.Host;
import com.yit.deploy.core.model.JinjaTemplate;
import com.yit.deploy.core.model.StatStruct;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.utils.EncryptionUtils;
import com.yit.deploy.core.utils.IO;
import com.yit.deploy.core.utils.Utils;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import hudson.FilePath;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by nick on 14/09/2017.
 */
public class FileStep extends AbstractFileStep {
    private byte[] content;
    private FilePath source;
    private String fileMode;
    private String fileOwner;
    private String fileGroup;
    private FileStatus status = FileStatus.present;
    private boolean pure;
    private boolean withJinja;
    private boolean withDecrypt;

    private List<FileHook> hooks;

    public FileStep(JobExecutionContext context) {
        super(context);
    }

    public FileStep setup(@DelegatesTo(value = DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    /**
     * Execute this step, providing a pipeline-script and a variable context
     */
    @Override
    protected Object executeOverride() throws Exception {
        assert targetHost != null;
        assert path != null && !path.isEmpty() && !"/".equals(path);

        List<FileHook> originHooks = hooks;

        hooks = hooks == null ? new ArrayList<>() : new ArrayList<>(hooks);
        if (withJinja) {
            hooks.add(new JinjaFileHook(context));
        }
        if (withDecrypt) {
            hooks.add(new DecryptFileHook(getEnv()));
        }

        StatStruct ss = statFile(path);
        switch (status) {
            case absent:
                if (ss.isExists()) {
                    deleteFile(path);
                } else {
                    // file already deleted
                }
                break;
            case present:
                if (ss.isExists()) {
                    if (FileType.file.equals(ss.getType())) {
                        createFileFromContentOrSource(ss);
                    } else if (FileType.directory.equals(ss.getType())) {
                        createDirectoryFromContentOrSource(ss);
                    } else {
                        assert false;
                    }
                } else {
                    if (content != null) {
                        createFileFromContentOrSource(ss);
                    } else if (source != null) {
                        if (source.isDirectory()) {
                            createDirectoryFromContentOrSource(ss);
                        } else {
                            createFileFromContentOrSource(ss);
                        }
                    } else {
                        createFile(path);
                    }
                }
                break;
            case file:
                createFileFromContentOrSource(ss);
                break;
            case directory:
                createDirectoryFromContentOrSource(ss);
                break;
            default:
                assert false;
        }

        hooks = originHooks;
        return null;
    }

    private void createFileFromContentOrSource(StatStruct ss) throws IOException, InterruptedException {
        if (ss.isExists() && !FileType.file.equals(ss.getType())) {
            throw new ChangeFileTypeException(path, ss.getType(), FileType.file);
        }

        if (content != null) {
            if (!ss.isExists()) {
                createFile(path);
            }
            syncFileContent(withJinja ? "file.j2" : "file", path, content);
        } else if (source != null) {
            assert !source.isDirectory();
            syncFile(source, path, ss);
        } else if (!ss.isExists()) {
            createFile(path);
        } else {
            syncFileAttributes(path, ss);
        }
    }

    private void createDirectoryFromContentOrSource(StatStruct ss) throws IOException, InterruptedException {
        if (ss.isExists() && !FileType.directory.equals(ss.getType())) {
            throw new ChangeFileTypeException(path, ss.getType(), FileType.directory);
        }

        assert content == null;
        if (source != null) {
            assert source.isDirectory();
            syncDirectory(source, path, ss);
        } else if (!ss.isExists()) {
            createDirectory(path);
        } else {
            syncFileAttributes(path, ss);
        }
    }

    /**
     * sync file content and file attributes to target file
     * @param from
     * @param to
     * @param ss
     */
    private void syncFile(FilePath from, String to, StatStruct ss) {
        if (ss.isExists()) {
            syncFileAttributes(to, ss);
        } else {
            createFile(to);
        }

        syncFileContent(from.getRemote(), to, IO.readBytes(from));
    }

    /**
     * sync all children files and directories and the file attributes to target file
     * @param from
     * @param to
     * @param ss
     */
    private void syncDirectory(FilePath from, String to, StatStruct ss) throws IOException, InterruptedException {
        if (ss.isExists()) {
            syncFileAttributes(to, ss);
        } else {
            createDirectory(to);
        }

        Map<String, StatStruct> childrenStats = Lambda.toMap(statFilesInDirectory(to), x -> (new File(x.getPath())).getName(), x -> x);

        for (FilePath file : filterFilesToSync(from.list())) {
            String name = toTargetFileName(file.getName());
            String targetFile = to + "/" + name;
            StatStruct stat = childrenStats.get(name);
            if (stat == null) {
                stat = new StatStruct();
                stat.setExists(false);
            }
            childrenStats.remove(name);
            if (file.isDirectory()) {
                syncDirectory(file, targetFile, stat);
            } else {
                syncFile(file, targetFile, stat);
            }
        }

        if (pure && !childrenStats.isEmpty()) {
            for (StatStruct stat : childrenStats.values()) {
                deleteFile(stat.getPath());
            }
        }
    }

    private void createFile(String file) {
        getScript().info("create file " + file);
        List<String> cs = new ArrayList<>();
        cs.add("touch '" + file + "'");
        cs.addAll(getModifyFileAttrsCommands(file));
        executeShell(String.join(" && ", cs));
    }

    private void createDirectory(String file) {
        getScript().info("create folder " + file);
        List<String> cs = new ArrayList<>();
        cs.add("mkdir -p '" + file + "'");
        cs.addAll(getModifyFileAttrsCommands(file));
        executeShell(String.join(" && ", cs));
    }

    private void deleteFile(String file) {
        getScript().info("delete file " + file);
        executeShell("sudo rm -rf '" + file + "'");
    }

    private void syncFileContent(String sourceFile, String targetFile, byte[] bytes) {
        bytes = toTargetFileContent(sourceFile, bytes);
        byte[] oldBytes = getFileContent(targetFile);
        if (!Arrays.equals(bytes, oldBytes)) {
            String diff = Utils.diff(oldBytes, bytes);
            getScript().info("update file %s, diff: \n%s", targetFile, diff);
            getRemoteLauncher("cat > '" + targetFile + "'").input(bytes).executeIgnoreOutput();
        }
    }

    private void syncFileAttributes(String file, StatStruct ss) {
        //script.debug("sync file attr " + file + " to " + fileOwner:" + fileGroup + ":" + fileMode);

        syncFileOwner(file, ss);
        syncFileGroup(file, ss);
        syncFileMode(file, ss);
    }

    private void syncFileOwner(String file, StatStruct ss) {
        if (fileOwner != null && !Objects.equals(fileOwner, ss.getOwner())) {
            getScript().info("change file %s owner to %s", file, fileOwner);
            executeShell("sudo chown " + fileOwner + " '" + file + "'");
        }
    }

    private void syncFileGroup(String file, StatStruct ss) {
        if (fileGroup != null && !Objects.equals(fileGroup, ss.getGroup())) {
            getScript().info("change file %s group to %s", file, fileGroup);
            executeShell("sudo chown :" + fileGroup + " '" + file + "'");
        }
    }

    private void syncFileMode(String file, StatStruct ss) {
        if (fileMode != null && !Objects.equals(fileMode, ss.getMode())) {
            getScript().info("change file " + file + " mode to " + fileMode);
            executeShell("chmod " + fileMode + " '" + file + "'");
        }
    }

    private List<String> getModifyFileAttrsCommands(String file) {
        List<String> cs = new ArrayList<>();
        if (fileOwner != null) {
            cs.add("chown " + fileOwner + " '" + file + "'");
        }
        if (fileGroup != null) {
            cs.add("chown :" + fileGroup + " '" + file + "'");
        }
        if (fileMode != null) {
            cs.add("chmod " + fileMode + " '" + file + "'");
        }
        return cs;
    }

    private byte[] getFileContent(String file) {
        return executeShellReturnOutput("cat '" + file + "'");
    }

    private String toTargetFileName(String sourceFileName) {
        for (FileHook hook : hooks) {
            sourceFileName = hook.toTargetFileName(sourceFileName);
        }
        return sourceFileName;
    }

    private byte[] toTargetFileContent(String sourceFileName, byte[] content) {
        for (FileHook hook : hooks) {
            content = hook.toTargetFileContent(sourceFileName, content);
        }
        return content;
    }

    private List<FilePath> filterFilesToSync(List<FilePath> files) {
        for (FileHook hook : hooks) {
            files = hook.filterFilesToSync(files);
        }
        return files;
    }

    public static class DslContext {

        private FileStep step;

        public DslContext(FileStep step) {
            this.step = step;
        }

        public DslContext targetHost(Host value) {
            step.targetHost = value;
            return this;
        }

        public DslContext path(String value) {
            step.path = value;
            return this;
        }

        public DslContext target(String value) {
            return path(value);
        }

        public DslContext content(String value) {
            return content(value.getBytes(Utils.DefaultCharset));
        }

        public DslContext content(byte[] value) {
            step.content = value;
            return this;
        }

        public DslContext source(FilePath value) {
            step.source = value;
            return this;
        }

        public DslContext fileMode(String value) {
            step.fileMode = value;
            return this;
        }

        public DslContext fileOwner(String value) {
            step.fileOwner = value;
            return this;
        }

        public DslContext group(String value) {
            step.fileGroup = value;
            return this;
        }

        public DslContext status(FileStatus value) {
            step.status = value;
            return this;
        }

        public DslContext pure() {
            step.pure = true;
            return this;
        }

        public DslContext withDecrypt() {
            step.withDecrypt = true;
            return this;
        }

        public DslContext withJinja() {
            step.withJinja = true;
            return this;
        }

        public DslContext template(String templateText) {
            step.withJinja = true;
            return content(templateText);
        }
    }

    private static class DecryptFileHook extends FileHook {
        final EncryptionUtils encryptionUtils;

        DecryptFileHook(Environment env) {
            encryptionUtils = new EncryptionUtils(env.getEnvtype());
        }

        @Override
        byte[] toTargetFileContent(String sourceFileName, byte[] content) {
            if (EncryptionUtils.isEncrypted(content)) {
                content = encryptionUtils.decrypt(new String(content, Utils.DefaultCharset));
            }
            return content;
        }
    }

    private static class JinjaFileHook extends FileHook {
        static final String JINJA_EXTENSION = ".j2";

        final JinjaTemplate jinjaTemplate;

        JinjaFileHook(JobExecutionContext context) {
            jinjaTemplate = new JinjaTemplate(context);
        }

        @Override
        String toTargetFileName(String sourceFileName) {
            if (sourceFileName.endsWith(JINJA_EXTENSION)) {
                return sourceFileName.substring(0, sourceFileName.length() - JINJA_EXTENSION.length());
            } else {
                return sourceFileName;
            }
        }

        @Override
        byte[] toTargetFileContent(String sourceFileName, byte[] content) {
            if (sourceFileName.endsWith(JINJA_EXTENSION)) {
                return jinjaTemplate.render(new String(content, Utils.DefaultCharset), sourceFileName).getBytes(Utils.DefaultCharset);
            } else {
                return content;
            }
        }

        @Override
        List<FilePath> filterFilesToSync(List<FilePath> files) {
            Set<String> ignoredFiles = new HashSet<>();

            for (FilePath file : files) {
                String path = file.getRemote();
                if (path.endsWith(JINJA_EXTENSION)) {
                    ignoredFiles.add(path.substring(0, path.length() - JINJA_EXTENSION.length()));
                }
            }

            if (ignoredFiles.isEmpty()) {
                return files;
            } else {
                return Lambda.findAll(files, f -> !ignoredFiles.contains(f.getRemote()));
            }
        }
    }

    public static class FileHook {
        String toTargetFileName(String sourceFileName) {
            return sourceFileName;
        }

        byte[] toTargetFileContent(String sourceFileName, byte[] content) {
            return content;
        }

        List<FilePath> filterFilesToSync(List<FilePath> files) {
            return files;
        }
    }

    public static class ChangeFileTypeException extends RuntimeException {
        ChangeFileTypeException(String file, FileType from, FileType to) {
            super("could not change file type from " + from + " to " + to + " for file " + file);
        }
    }
}
