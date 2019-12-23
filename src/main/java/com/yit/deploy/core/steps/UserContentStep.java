package com.yit.deploy.core.steps;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.support.ExceptionSupport;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import hudson.FilePath;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by nick on 14/09/2017.
 */
public class UserContentStep extends AbstractStep implements ExceptionSupport {
    private static final int MaxFilesToDelete = 50;

    private String description;
    private FilePath sourceFile;
    private String folderName;
    private int filesToKeep = -1;

    public UserContentStep(JobExecutionContext context) {
        super(context);
    }

    public UserContentStep setup(@DelegatesTo(value = DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    /**
     * Execute this step, providing a pipeline-script and a variable context
     */
    @Override
    protected String executeOverride() throws Exception {
        assert sourceFile != null && sourceFile.exists();
        assert folderName != null && !folderName.isEmpty();

        String userContentFolder = findValidUserContentFolder();
        FilePath userContentFilePath = getUserContentFilePath(userContentFolder);
        sourceFile.copyRecursiveTo(userContentFilePath);
        String url = getScript().getEnvvars().get("JENKINS_URL") + "userContent/" + userContentFolder + "/";
        getScript().info("== %s is exported at link %s ==", description, url);
        return url;
    }

    private String findValidUserContentFolder() throws IOException, InterruptedException {
        String rootFolder = folderName + "/" + getEnv().getName();
        FilePath root = getUserContentFilePath(rootFolder);
        String timestamp = new SimpleDateFormat("YYYY-MM-dd-HH-mm").format(new Date());

        for (int i = 1; i <= 10; i++) {
            String folderName = timestamp;
            if (i > 1) {
                folderName += "_" + i;
            }
            FilePath filepath = root.child(folderName);
            if (!filepath.exists()) {
                filepath.mkdirs();
                deleteOldFolders(root, filepath);

                return rootFolder + "/" + folderName;
            }
        }
        throw new RuntimeException("could not find valid folder after 10 times trying");
    }

    private void deleteOldFolders(FilePath filePath, FilePath exclude) throws IOException, InterruptedException {
        if (filesToKeep < 0) return;
        List<FilePath> files = new ArrayList<>(filePath.listDirectories());
        files.sort(Comparator.comparing(file -> unchecked(file, FilePath::lastModified)));
        int up = files.size() - filesToKeep;
        for (int i = 0; i < up && i < MaxFilesToDelete; i++) {
            FilePath file = files.get(i);
            if (file == exclude) break;
            getScript().info("delete old folder " + file);
            file.deleteRecursive();
        }
        if (MaxFilesToDelete < up) {
            getScript().warn("there are still %s file(s) should be deleted. they will be deleted at next time.", up - MaxFilesToDelete);
        }
    }

    private FilePath getUserContentFilePath(String folder) {
        String path = context.getVariable("JENKINS_USER_HOME") + "/userContent/" + folder;
        return new FilePath(new File(path));
    }

    public static class DslContext {

        private UserContentStep step;

        public DslContext(UserContentStep step) {
            this.step = step;
        }

        public DslContext description(String value) {
            step.description = value;
            return this;
        }

        public DslContext from(FilePath value) {
            step.sourceFile = value;
            return this;
        }

        public DslContext fromWorkspace(String relativePath) {
            from(step.getScript().getWorkspaceFilePath().child(relativePath));
            return this;
        }

        public DslContext folder(String value) {
            step.folderName = value;
            return this;
        }

        public DslContext filesToKeep(int value) {
            step.filesToKeep = value;
            return this;
        }
    }
}
