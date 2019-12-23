package com.yit.deploy.core.docker.containers;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.utils.IO;
import groovy.lang.Closure;
import hudson.FilePath;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by nick on 24/10/2017.
 */
public class MavenContainer extends JavaContainer {
    private FilePath pomFile;
    private List<String> options = Collections.emptyList();
    private boolean releaseMode;

    public MavenContainer(JobExecutionContext executionContext, String imageFullName) {
        this(executionContext, imageFullName, Collections.emptyList());
    }

    public MavenContainer(JobExecutionContext executionContext) {
        this(executionContext, Collections.emptyList());
    }

    public MavenContainer(JobExecutionContext executionContext, List<String> runOptions) {
        this(executionContext, executionContext.getVariable("DOCKER_REGISTRY", String.class) + "/official/maven:8u111-alpine-3.4-mvn-3.5.3", runOptions);
    }

    public MavenContainer(JobExecutionContext executionContext, String imageFullName, List<String> runOptions) {
        super(executionContext, imageFullName, runOptions);
    }

    public MavenContainer options(List<String> options) {
        MavenContainer c = (MavenContainer) clone();
        c.options = Lambda.concat(c.options, options);
        return c;
    }

    public MavenContainer options(String ... list) {
        return options(Arrays.asList(list));
    }

    public MavenContainer pomFile(FilePath file) {
        MavenContainer c = (MavenContainer) clone();
        c.pomFile = file;
        return c;
    }

    public MavenContainer releaseMode() {
        MavenContainer c = (MavenContainer) clone();
        c.releaseMode = true;
        return c;
    }

    public void mvn(List<String> args) {
        run(x -> executePrintOutput(getMvnCommand(args)));
    }

    public void run(Closure closure) {
        run(closure::call);
    }

    public void run(Consumer<MavenContainer> action) {
        try {
            List<List<?>> pomBackup = new ArrayList<>();

            if (releaseMode) {
                FilePath[] poms = getScript().getWorkspaceFilePath().list("**/pom.xml");
                for (FilePath pom : poms) {
                    String origin = pom.readToString();
                    String release = origin.replaceAll("-SNAPSHOT", "");
                    if (!Objects.equals(origin, release)) {
                        getScript().info("change pom file %s to relase mode", pom);
                        pomBackup.add(Arrays.asList(pom, origin));
                        pom.write(release, null);
                    }
                }
            }

            try {
                action.accept(this);
            } finally {
                for (List item : pomBackup) {
                    ((FilePath) item.get(0)).write((String) item.get(1), null);
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void mvn(String ... args) {
        mvn(Arrays.asList(args));
    }

    private List<String> getMvnCommand(List<String> args) {
        List<String> cmd = new ArrayList<>(args.size() + options.size() + 5);
        cmd.add("mvn");
        cmd.add("-Dmaven.repo.local=" + image.getUserHome() + "/.m2/repository");
        cmd.addAll(options);
        cmd.addAll(args);
        if (pomFile != null) {
            cmd.add("-f");
            cmd.add(pomFile.getRemote());
        }
        return cmd;
    }

    private FilePath getPwdFilePath() {
        return Lambda.cascade(Lambda.safeNavigate(pomFile, FilePath::getParent), getScript().getWorkspaceFilePath());
    }

    public void compile(List<String> actions) {
        compile(actions, true, true);
    }

    public void compile(List<String> actions, boolean skipTest) {
        compile(actions, skipTest, true);
    }

    public void compile(List<String> actions, boolean skipTest, boolean clean) {
        if (clean && !actions.contains("clean")) {
            actions = Lambda.concat("clean", actions);
        }
        if (skipTest) {
            actions = new ArrayList<>(actions);
            actions.add("-Dmaven.test.skip");
        }
        mvn(actions);
    }

    public void compile(String ... actions) {
        compile(Arrays.asList(actions));
    }

    public List<DependencyItem> dependencyList(Map<String, String> args) {
        try {
            String saveFilename = "pipeline-dependency.list";
            FilePath[] saveFiles = getPwdFilePath().list("**/" + saveFilename);

            for (FilePath path : saveFiles) {
                path.delete();
            }
            List<String> params = new ArrayList<>();
            params.add("dependency:list");
            params.add("-DoutputFile=" + saveFilename);
            if (args != null) {
                for (String key : args.keySet()) {
                    params.add("-D" + key + "=" + args.get(key));
                }
            }
            mvn(params);
            saveFiles = getPwdFilePath().list("**/" + saveFilename);
            List<String> depLines = Lambda.tokenize(String.join("", Lambda.map(Arrays.asList(saveFiles), IO::readToString)), "\n");
            List<String> dep = Lambda.unique(Lambda.map(Lambda.findAll(depLines, s -> s.startsWith("  ") && !(s.matches("^ *none$"))), String::trim));

            for (FilePath path : saveFiles) {
                path.delete();
            }

            List<DependencyItem> depItems = new ArrayList<>(dep.size());
            for (String s : dep) {
                List<String> a = Lambda.tokenize(s, ":");
                DependencyItem di = new DependencyItem();
                di.group = a.get(0);
                di.name = a.get(1);
                di.type = a.get(2);
                di.version = a.get(3);
                depItems.add(di);
            }
            return depItems;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static class DependencyItem implements Serializable {
        private String group;
        private String name;
        private String type;
        private String version;

        public boolean isJar() {
            return "jar".equals(type);
        }

        public boolean isSnapshot() {
            return version.endsWith("-SNAPSHOT");
        }

        @Override
        public String toString() {
            return group + ":" + name + ":" + version + ":" + type;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
