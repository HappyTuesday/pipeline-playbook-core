package com.yit.deploy.core.model;

import com.yit.deploy.core.config.DeployConfig;
import com.yit.deploy.core.dsl.parse.ProjectBaseScript;
import com.yit.deploy.core.dsl.parse.ProjectsBaseScript;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.exceptions.MissingProjectException;
import com.yit.deploy.core.function.Holder;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.ProjectInfo;
import com.yit.deploy.core.info.ProjectInfoAccessor;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class Projects implements Iterable<Project> {

    private static final Logger LOGGER = Logger.getLogger(Projects.class.getName());

    public static final String ROOT_PROJECT_NAME = "$";

    private final LinkedHashMap<String, Project> map;
    private final HashMap<String, List<Project>> variableGroups;

    public Projects(Map<String, ProjectInfo> infoMap, Environments envs) {
        this.map = new LinkedHashMap<>(infoMap.size());

        Holder<Consumer<String>> convert = new Holder<>();
        convert.data = projectName -> {
            for (String p : infoMap.get(projectName).getParents()) {
                if (!this.map.containsKey(p)) {
                    convert.data.accept(p);
                }
            }

            this.map.put(projectName, new Project(projectName, infoMap, envs, this));
        };

        for (String key : infoMap.keySet()) {
            if (!this.map.containsKey(key)) {
                convert.data.accept(key);
            }
        }

        this.variableGroups = new HashMap<>(this.map.size());
        for (Project p : map.values()) {
            if (p.getVariableGroup() == null) continue;

            List<Project> list = this.variableGroups.computeIfAbsent(p.getVariableGroup(), k -> new ArrayList<>(1));
            if (list.isEmpty()) {
                list.add(p);
            } else {
                /*
                 * ensure that if children projects exists, then any parents projects should be ignored deal to the
                 * fact that the variables defined in parents will all be inherited by their children.
                */

                // remove all parents of p, including p itself
                list.removeIf(q -> p.belongsTo(q.getProjectName()));

                // p will be ignored if a child of it exists
                if (Lambda.all(list, q -> !q.belongsTo(p.getProjectName()))) {
                    list.add(p);
                }
            }
        }
    }

    public Project get(String name) {
        if (name == null) {
            return null;
        }

        Project project = map.get(name);
        if (project == null) {
            throw new IllegalConfigException("invalid project name " + name);
        }
        return project;
    }

    public List<Project> findByVariableGroup(String groupName) {
        return this.variableGroups.get(groupName);
    }

    public boolean contains(String name) {
        return map.containsKey(name);
    }

    public int size() {
        return map.size();
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<Project> iterator() {
        return map.values().iterator();
    }

    private static final Map<String, Integer> lastScriptFileSequence = new HashMap<>();

    public static Map<String, ProjectInfo> load(DeployConfig deployConfig,
                                                Environments envs,
                                                Playbooks playbooks) {

        DeployConfig.Folder root = deployConfig.getProjectsFolder();
        List<DeployConfig.DEntry> list = new LinkedList<>();
        list.add(root);
        root.listRecursive(list);

        // sort the list by history knowledge, so that we will use the right order to load each file
        synchronized (lastScriptFileSequence) {
            if (!lastScriptFileSequence.isEmpty()) {
                list.sort(Comparator.comparing(
                    entry -> Lambda.cascade(lastScriptFileSequence.get(entry.path), Integer.MAX_VALUE))
                );
            }
        }

        boolean sequenceChanged = false;
        Map<String, ProjectInfo> projects = new HashMap<>();
        ProjectInfoAccessor accessor = ProjectInfoAccessor.from(projects);

        for (int i = 0; i < list.size(); i++) {
            DeployConfig.DEntry entry = list.get(i);
            Collection<ProjectInfo> more = null;

            try {
                more = loadOne(entry, envs, playbooks, accessor);
            } catch (MissingProjectException e) {

                int j = i + 1;
                while (j < list.size()) {
                    try {
                        more = loadOne(list.get(j), envs, playbooks, accessor);
                        break;
                    } catch (MissingProjectException ignore) {
                        j++; // next
                    }
                }

                if (j >= list.size()) {
                    throw e;
                }

                // since list[i] can not be resolved but list[j] can, so we swap these two
                DeployConfig.DEntry swap = list.get(j);
                list.set(i, swap);
                list.set(j, entry);
                sequenceChanged = true;

                LOGGER.fine(() -> "[load-project] swap project script " + entry.path + " and " + swap.path);
            }

            for (ProjectInfo p : more) {
                if (projects.put(p.getProjectName(), p) != null) {
                    throw new IllegalConfigException("duplicated project " + p.getProjectName() + " found");
                }
            }
        }

        // save current sequence
        if (sequenceChanged) {
            synchronized (lastScriptFileSequence) {
                lastScriptFileSequence.clear();
                for (int i = 0; i < list.size(); i++) {
                    lastScriptFileSequence.put(list.get(i).path, i);
                }
            }
        }

        return projects;
    }

    private static Collection<ProjectInfo> loadOne(DeployConfig.DEntry entry,
                                                    Environments envs,
                                                    Playbooks playbooks,
                                                    ProjectInfoAccessor accessor) {

        String name = entry instanceof DeployConfig.File ? ((DeployConfig.File) entry).getBaseName() : entry.getName();

        if (DeployConfig.PROJECT_INIT_SCRIPT_FILE.equals(name)) {
            return Collections.emptyList();
        }

        String parentName;
        if (DeployConfig.PROJECTS_FOLDER.equals(entry.path)) {
            parentName = null;
        } else if (entry.getParentPath().equals(DeployConfig.PROJECTS_FOLDER)) {
            parentName = ROOT_PROJECT_NAME;
        } else {
            parentName = entry.getParent().getName();
        }

        if (entry instanceof DeployConfig.Folder) {

            DeployConfig.Folder folder = ((DeployConfig.Folder) entry);

            ProjectInfo project = new ProjectInfo();
            project.setAbstracted(true);

            if (parentName == null) {
                project.setProjectName(ROOT_PROJECT_NAME);
                project.setActiveInEnv(Environments.ROOT_ENVIRONMENT_NAME);
            } else {
                project.setProjectName(name);
                project.getParents().add(parentName);
            }

            if (accessor.exist(project.getProjectName())) {
                throw new IllegalConfigException("project " + project.getProjectName() + " is duplicated");
            }

            DeployConfig.File init = folder.childScript(DeployConfig.PROJECT_INIT_SCRIPT_FILE);
            if (init.isExist()) {
                init.getScript(ProjectBaseScript.class).parse(project, envs, playbooks, accessor.with(project));
            }

            return Collections.singletonList(project);
        }  else if (entry instanceof DeployConfig.File) {
            if (parentName == null) {
                throw new IllegalConfigException("invalid project parsing sequence");
            }

            DeployConfig.File file = (DeployConfig.File) entry;
            return file.getScript(ProjectsBaseScript.class).parse(accessor.getProject(parentName), envs, playbooks, accessor);
        } else {
            throw new IllegalArgumentException("invalid file entry " + entry);
        }
    }

    public static String projectName(@Nullable String name) {
        return name == null ? ROOT_PROJECT_NAME : name;
    }
}
