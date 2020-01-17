package com.yit.deploy.core.config;

import com.yit.deploy.core.compile.DeployCompiler;
import com.yit.deploy.core.dsl.parse.EnvironmentBaseScript;
import com.yit.deploy.core.dsl.parse.PlaybookBaseScript;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.DeployInfoTable;
import com.yit.deploy.core.utils.IO;
import com.yit.deploy.core.utils.Utils;
import groovy.lang.Script;
import hudson.FilePath;
import org.codehaus.groovy.runtime.InvokerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * To allow real deploy logic defined outside the pipeline-playbook-plugin, we introduce a deploy-config project, in which
 * playbooks and environment scripts and projects definition are defined. The so-called deploy-config can be saved in
 * a GIT repository or just in the local filesystem on one Jenkins node.
 *
 * The deploy-config project will be used every time to initialize a project, playbook or environment. In fact,
 * the static method getInstance will be called at every point in which deploy-config is needed. In the getInstance method,
 * we should fetch the lasted code of the deploy-config and load all files in that project.
 *
 * But in most cases, only few files in the deploy-config are changed, and some times, the project is entirely not changed.
 * So, to speed up the getInstance method, we remember the instance we created in the last getInstance call and to reuse
 * this instance if no files are changed.
 *
 * But sometimes, the getInstance method may be called in more than one threads in the same time. So we introduce
 * a master/slave mode to resolve this situation.
 *
 * The master thread is the first thread who got the masterLock, and others are slave threads.
 * The master thread is responsible for fetching source code, creating the instance if no last instance can be reused,
 * and loading files from filesystem.
 *
 * The slave threads are just waiting for the master thread to finish its job and just use the instance created previously by the master thread.
 * NOTE: The master/slave reuse mode is disabled if configProject is not the same.
 * Created by nick on 28/12/2017.
 */
public class DeployConfig {

    private static Logger logger = Logger.getLogger(DeployConfig.class.getName());

    /**
     * only files suffixed with this string will be loaded as scripts
     */
    public static final String SCRIPT_FILE_EXTENSION = ".groovy";

    public static final String SRC_FOLDER = "/src";

    public static final String RESOURCE_FOLDER = "/resources";

    public static final char PATH_SEPARATOR = '/';

    public static final String PROJECTS_FOLDER = SRC_FOLDER + PATH_SEPARATOR + "projects";

    public static final String PROJECT_INIT_SCRIPT_FILE = "$";

    /**
     * when script file named _.groovy in a folder named xxx, then the environment name will be xxx instead of xxx/_
     */
    public static final String UNNAMED_ENV_FILE_NAME = "_";

    /**
     * we introduce a process logic to reduce the useless fetching tasks
     */
    private static final Object masterLock = new Object();
    private static volatile ConfigProject masterConfigProject;
    private static final Object instanceLock = new Object();
    private static volatile DeployConfig instance;

    @Nonnull
    public static DeployConfig getInstance(@Nonnull ConfigProject configProject) {
        boolean masterThread = false;
        boolean differentConfigProject = false;
        DeployConfig creating;

        synchronized (masterLock) {
            if (masterConfigProject == null) { // master thread
                masterThread = true;
                masterConfigProject = configProject;
            } else { // slave thread
                if (!configProject.equals(masterConfigProject)) { // in case a different configProject is provided
                    differentConfigProject = true;
                }
            }
        }

        // fetch source code of the config project in three cases:
        // 1. here we are in the master mode, that is that this is the first thread among all threads
        //    which are executing this method at this time.
        // 2. or we are not in the master mode, that means there are already a thread that will fetch the source code for me,
        //    but the configProject does not equal to the configProject of that master thread.
        // 3. otherwise, we only allow one thread to fetch source code at one time, so that we can reduce the processing time.

        if (masterThread || differentConfigProject) {
            long start = new Date().getTime();
            logger.info("fetching and initializing configuration project");
            synchronized (instanceLock) {
                creating = instance;
                instance = null;
                try {
                    if (creating != null &&
                        !configProject.equals(creating.configProject)) {

                        creating = null;
                    }

                    // try to reuse the instance created within configProject.maxDelay
                    if (creating == null ||
                        creating.timestamp < System.currentTimeMillis() - configProject.maxDelay) {

                        String hash = configProject.fetch();
                        if (creating == null) {
                            creating = new DeployConfig(hash, configProject);
                        } else if (hash == null || !hash.equals(creating.commitHash)) {
                            creating = new DeployConfig(hash, creating); // reload source files
                        } else {
                            // no new commit found, reuse the instance entirely
                        }

                        // record the timestamp when we refreshed or created the instance
                        creating.timestamp = System.currentTimeMillis();
                    }
                } finally {
                    synchronized (masterLock) {
                        if (masterThread) {
                            masterConfigProject = null;
                        }
                    }
                }
                instance = creating;
            }
            logger.info("configuration initialization finished in " + (new Date().getTime() - start) / 1000 + "s.");
        } else {
            logger.info("waiting for another job to initialize configuration");
            synchronized (instanceLock) { // just to wait master thread to finish
                creating = instance;
                logger.info("configuration initialization finished");
            }
        }

        if (creating == null) {
            throw new IllegalStateException("create instance failed");
        }

        return creating;
    }

    /**
     * the config project
     */
    private final ConfigProject configProject;
    /**
     * the commit hash of the config project from which the scripts are loaded.
     * it could be null after restart since this field is declared with transient.
     * and also it could be null if the config project is not stored in git repository,
     * when debugging, for instance.
     * set this value to null means we will disable the cache logic, since we could not
     * identify which version are we using now.
     */
    private final String commitHash;

    /**
     * used to record the time when this instance is created
     */
    private long timestamp;

    /**
     * file name -> file content mapping, all files in deploy-config project should be put into this map
     */
    private final Map<String, byte[]> files;
    /**
     * folder -> children of the folder mapping, all folders in deploy-config project should go into this map
     */
    private final Map<String, Set<String>> folders;
    /**
     * the scripts cache: file path -> script class
     */
    private final ConcurrentHashMap<String, Class<Script>> scriptClasses;

    private final DeployInfoTable infoTable;

    /**
     * create a new deploy config entirely
     */
    private DeployConfig(@Nullable String commitHash, @Nonnull ConfigProject configProject) {
        this.configProject = configProject;
        this.commitHash = commitHash;

        Map<String, byte[]> files = new HashMap<>();
        for (FilePath f : listAllFilesFromDisk()) {
            files.put(generateFilename(f), IO.readBytes(f));
        }

        this.files = files;
        this.folders = parseFoldersMap(files.keySet());
        this.scriptClasses = new ConcurrentHashMap<>();

        this.infoTable = new DeployInfoTable(this);
    }

    /**
     * create a deploy config from other deploy config with different commit hash
     */
    private DeployConfig(String commitHash, @Nonnull DeployConfig other) {
        this.configProject = other.configProject;
        this.commitHash = commitHash;

        Map<String, byte[]> oldFiles = new HashMap<>(other.files), files = new HashMap<>();
        Map<String, Class<Script>> oldScriptClasses = new HashMap<>(other.scriptClasses), scriptClasses = new HashMap<>();

        for (FilePath f : listAllFilesFromDisk()) {
            String filename = generateFilename(f);
            if (oldFiles.containsKey(filename)) {
                byte[] oldContent = oldFiles.get(filename);
                byte[] newContent = IO.readBytes(f);
                if (Arrays.equals(newContent, oldContent)) { // no change
                    // content does not change, discard newContent variable to give convenience to GC
                    files.put(filename, oldContent);
                    if (oldScriptClasses.containsKey(filename)) {
                        scriptClasses.put(filename, oldScriptClasses.get(filename));
                    }
                } else {
                    files.put(filename, newContent);
                }
            } else {
                files.put(filename, IO.readBytes(f));
            }
        }

        this.files = files;
        this.folders = parseFoldersMap(files.keySet());
        this.scriptClasses = new ConcurrentHashMap<>(scriptClasses);

        this.infoTable = new DeployInfoTable(this);
    }

    /**
     * generate a file name used to store in the files hash map, all file names are starting with '/'
     */
    private String generateFilename(FilePath f) {
        if (!f.getRemote().startsWith(configProject.rootPath.getRemote())) {
            throw new IllegalArgumentException("file " + f.getRemote() + " must start with " + configProject.rootPath.getRemote());
        }
        String name = f.getRemote().substring(configProject.rootPath.getRemote().length());
        if (name.charAt(0) != PATH_SEPARATOR) {
            name = PATH_SEPARATOR + name;
        }
        return name;
    }

    private List<FilePath> listAllFilesFromDisk() {
        List<FilePath> files = new ArrayList<>();
        FilePath root = configProject.rootPath;
        try {
            files.addAll(Arrays.asList(root.list("resources/**")));
            files.addAll(Arrays.asList(root.list("src/**/*" + SCRIPT_FILE_EXTENSION)));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return files;
    }

    private static Map<String, Set<String>> parseFoldersMap(Collection<String> files) {
        Map<String, Set<String>> folders = new HashMap<>();
        for (String path : files) {
            String folder = String.valueOf(PATH_SEPARATOR);
            for (int index = path.indexOf(PATH_SEPARATOR, 1);; index = path.indexOf(PATH_SEPARATOR, index + 1)) {
                String subPath = index < 0 ? path : path.substring(0, index);
                folders.computeIfAbsent(folder, x -> new HashSet<>()).add(subPath);
                if (index < 0) break;
                folder = subPath;
            }
        }
        return folders;
    }

    @Nonnull
    public List<String> listResourcesInFolder(@Nonnull String resourceFolderPath, boolean recursive) {
        Folder folder = getResourceFolder().childFolder(resourceFolderPath);
        List<File> files = recursive ? folder.listFilesRecursive() : folder.listFiles();
        return Lambda.map(files, f -> f.path.substring(RESOURCE_FOLDER.length() + 1));
    }

    /**
     * get the content in bytes of a resource by its resource name
     * @param resourceName file name relative to resources/ folder
     * @return
     */
    public byte[] getResource(@Nonnull String resourceName) {
        return getResourceFolder().childFile(resourceName).getBytes();
    }

    public EnvironmentBaseScript getEnvironmentScript(@Nonnull String envName) {
        String filename = envName.replace('.', PATH_SEPARATOR);
        Folder folder = getEnvsFolder();
        File file = folder.childScript(filename);
        if (!file.isExist()) {
            // a script file named xxx/_.groovy is regarded as xxx
            file = folder.childScript(filename + PATH_SEPARATOR + UNNAMED_ENV_FILE_NAME);
            if (!file.isExist()) {
                throw new IllegalArgumentException("could not find environment " + envName);
            }
        }
        return file.getScript(EnvironmentBaseScript.class);
    }

    public PlaybookBaseScript getPlaybookScript(@Nonnull String playbookName) {
        return getPlaybooksFolder()
            .childScript(playbookName.replace('.', PATH_SEPARATOR))
            .getScript(PlaybookBaseScript.class);
    }

    public Folder getResourceFolder() {
        return new Folder(RESOURCE_FOLDER);
    }

    public Folder getSourceFolder() {
        return new Folder(SRC_FOLDER);
    }

    public Folder getEnvsFolder() {
        return getSourceFolder().childFolder("envs");
    }

    public Folder getPlaybooksFolder() {
        return getSourceFolder().childFolder("playbooks");
    }

    public Folder getProjectsFolder() {
        return new Folder(PROJECTS_FOLDER);
    }

    public String getCommitHash() {
        return commitHash;
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    private <T extends Script> T parseScript(@Nonnull String path, Class<T> clazz) {
        if (!path.endsWith(SCRIPT_FILE_EXTENSION)) {
            throw new IllegalArgumentException("file " + path + " is not a valid groovy script file");
        }

        Class<Script> scriptClass = scriptClasses.computeIfAbsent(path, p -> {
            byte[] content  = files.get(path);
            if (content == null) {
                throw new IllegalArgumentException("invalid script path " + path);
            }
            Script script = DeployCompiler.getInstance().getGroovyShell().parse(new String(content, Utils.DefaultCharset), path);
            return (Class<Script>) script.getClass();
        });

        return (T) InvokerHelper.createScript(scriptClass, DeployCompiler.getInstance().getGroovyShell().getContext());
    }

    public DeployInfoTable getInfoTable() {
        return infoTable;
    }

    /**
     * express a file or directory
     */
    public abstract class DEntry {
        /**
         * the path of the file or folder, always starting with '/' and never ends with '/' unless it is root directory
         */
        public final String path;

        /**
         * the path of the directory entry
         * the root folder should be / or empty
         */
        DEntry(@Nonnull String path) {
            if (path.isEmpty()) {
                this.path = String.valueOf(PATH_SEPARATOR);
            } else if (path.length() == 1 && path.charAt(0) == PATH_SEPARATOR) {
                this.path = path;
            } else if (path.charAt(path.length() - 1) == PATH_SEPARATOR) {
                this.path = path.substring(0, path.length() - 1);
            } else {
                this.path = path;
            }
        }

        /**
         * get the name of the entry
         */
        public String getName() {
            return isRoot() ? path : path.substring(path.lastIndexOf(PATH_SEPARATOR) + 1);
        }

        /**
         * get the parent path of the entry, return null if this is the root directory
         */
        public String getParentPath() {
            if (isRoot()) {
                return null;
            }
            return path.substring(0, path.lastIndexOf(PATH_SEPARATOR));
        }

        public String getParentPathRelativeTo(String folder) {
            return getParentPathRelativeTo(new Folder(folder));
        }

        /**
         * get the parent path from here to the specified folder, do not started with /
         * @param folder
         * @return
         */
        public String getParentPathRelativeTo(Folder folder) {
            if (isRoot()) {
                return null;
            }
            if (folder.isRoot()) {
                return getParentPath().substring(1);
            }

            if (!belongsTo(folder.path)) {
                throw new IllegalArgumentException("path " + path + " is not under folder " + folder.path);
            }

            int begin = folder.path.length() + 1;
            int end = path.lastIndexOf(PATH_SEPARATOR);
            return begin < end ? path.substring(begin, end) : null;
        }

        /**
         * get the parent directory entry, return null if this is the root directory
         */
        public Folder getParent() {
            String parentPath = getParentPath();
            return parentPath == null ? null : new Folder(parentPath);
        }

        public boolean isRoot() {
            return path.charAt(path.length() - 1) == PATH_SEPARATOR;
        }

        public abstract boolean isExist();

        public boolean belongsTo(String parent) {
            if (isRoot()) {
                return false;
            } else if (new Folder(parent).isRoot()) {
                return true;
            } else {
                return path.startsWith(parent)
                    && path.length() > parent.length()
                    && path.charAt(parent.length()) == PATH_SEPARATOR;
            }
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DEntry && path.equals(((DEntry) obj).path);
        }

        @Override
        public String toString() {
            return "file:/" + path;
        }
    }

    /**
     * represent a folder
     */
    public class Folder extends DEntry {
        Folder(@Nonnull String path) {
            super(path);
        }

        public File childFile(@Nonnull String path) {
            return new File(this.path + PATH_SEPARATOR + path);
        }

        public Folder childFolder(@Nonnull String path) {
            return new Folder(this.path + PATH_SEPARATOR + path);
        }

        /**
         *
         * @param scriptPath without .groovy extension
         * @return
         */
        public File childScript(@Nonnull String scriptPath) {
            return new File(this.path + PATH_SEPARATOR + scriptPath + SCRIPT_FILE_EXTENSION);
        }

        /**
         * list all files (no sub folders) in current folder
         */
        public List<File> listFiles() {
            return Lambda.findAll(list(), x -> x instanceof File, x -> (File)x);
        }

        public List<File> listScripts() {
            return Lambda.findAll(listFiles(), x -> x.path.endsWith(SCRIPT_FILE_EXTENSION));
        }

        /**
         * list all files (no sub folders) in current folder and its sub folders recursively
         */
        public List<File> listFilesRecursive() {
            return Lambda.findAll(listRecursive(), x -> x instanceof File, x -> (File)x);
        }

        public List<File> listScriptsRecursive() {
            return Lambda.findAll(listRecursive(), x -> x instanceof File && x.path.endsWith(SCRIPT_FILE_EXTENSION), x -> (File)x);
        }

        /**
         * list all files and sub folders in current folder
         */
        public List<DEntry> list() {
            Collection<String> children = folders.get(path);
            if (children == null) return Collections.emptyList();
            List<DEntry> list = new ArrayList<>(children.size());
            for (String f : children) {
                if (files.containsKey(f)) {
                    list.add(new File(f));
                }
                if (folders.containsKey(f)) {
                    list.add(new Folder(f));
                }
            }
            return list;
        }

        /**
         * list all files and sub folders in current folder and its sub folders recursively
         */
        public List<DEntry> listRecursive() {
            List<DEntry> list = new ArrayList<>();
            listRecursive(list);
            return list;
        }

        public void listRecursive(List<DEntry> list) {
            for (DEntry entry : list()) {
                list.add(entry);
                if (entry instanceof Folder) {
                    ((Folder) entry).listRecursive(list);
                }
            }
        }

        @Override
        public boolean isExist() {
            return folders.containsKey(path);
        }
    }

    /**
     * represent a file
     */
    public class File extends DEntry {
        File(@Nonnull String path) {
            super(path);
        }

        /**
         * get the text from the file, using UTF-8 encoding
         */
        public String getText() {
            byte[] bytes = getBytes();
            return bytes == null ? null : new String(bytes, Utils.DefaultCharset);
        }

        /**
         * return file bytes
         */
        public byte[] getBytes() {
            return files.get(path);
        }

        /**
         * return the parsed script object, if it is a groovy script file
         */
        public <T extends Script> T getScript(Class<T> clazz) {
            return parseScript(path, clazz);
        }

        public Script getScript() {
            return getScript(Script.class);
        }

        @Override
        public boolean isExist() {
            return files.containsKey(path);
        }

        /**
         * base name of the file, without file extension
         * @return base name
         */
        public String getBaseName() {
            String name = getName();
            int index = name.lastIndexOf('.');
            return index < 0 ? name : name.substring(0, index);
        }

        public String getPlaybookName() {
            String path = getParentPathRelativeTo(getPlaybooksFolder());
            if (path == null || path.isEmpty()) {
                return getBaseName();
            }
            return path.replace('/', '.') + "." + getBaseName();
        }

        public String getEnvironmentName() {
            String path = getParentPathRelativeTo(getEnvsFolder());
            String name;
            if (path == null || path.isEmpty()) {
                name = getBaseName();
            } else {
                name = path.replace('/', '.') + "." + getBaseName();
            }
            return normalizeEnvName(name);
        }

        private String normalizeEnvName(String name) {
            // a script file named xxx/$.groovy is regarded as xxx
            String suffix = "." + UNNAMED_ENV_FILE_NAME;
            if (name.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length());
            }
            return name;
        }
    }
}
