package com.yit.deploy.core.info;

import com.yit.deploy.core.dsl.evaluate.PlaybookParameterEvaluationContext;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.inherits.Inherits;
import com.yit.deploy.core.model.*;
import com.yit.deploy.core.records.Assignment;
import com.yit.deploy.core.records.ProjectRecord;
import com.yit.deploy.core.records.RecordTarget;
import com.yit.deploy.core.variables.LayeredVariables;
import com.yit.deploy.core.variables.SimpleVariables;

import java.util.*;
import java.util.function.Function;

public class ProjectInfo implements RecordTarget<ProjectRecord> {
    private String projectName;
    /**
     * project name generator for its children
     * will be evaluated by a key parameter when before creating a child project
     * the key will be the key of the created child project
     */
    private ClosureWrapper<String> projectNameGenerator;
    /**
     * variable group generator
     */
    private ClosureWrapper<String> variableGroupGenerator;
    private String key;
    /**
     * only projects defined by project records will have a non-null id
     */
    private String id;
    private String activeInEnv;
    private String description;
    private List<String> parents = new ArrayList<>();
    private Boolean abstracted;
    private SimpleVariables vars = new SimpleVariables();
    private String playbookName;
    private List<ProjectOverride> overrides = new ArrayList<>();
    private List<ClosureWrapper<Boolean>> when;
    private List<String> includedInEnv;
    private List<String> includedOnlyInEnv;
    private List<String> excludedInEnv;
    private LinkedHashMap<String, String> sharing = new LinkedHashMap<>();

    public ProjectInfo() {
    }

    public ProjectInfo(String projectName) {
        this.projectName = projectName;
    }

    public ProjectInfo(ProjectInfo that) {
        this.projectNameGenerator = that.projectNameGenerator;
        this.variableGroupGenerator = that.variableGroupGenerator;
        this.key = that.key;
        this.id = that.id;
        this.activeInEnv = that.activeInEnv;
        this.description = that.description;
        this.parents = that.parents;
        this.abstracted = that.abstracted;
        this.vars = that.vars;
        this.overrides = that.overrides;
        this.playbookName = that.playbookName;

        this.when = that.when;
        this.includedInEnv = that.includedInEnv;
        this.includedOnlyInEnv = that.includedOnlyInEnv;
        this.excludedInEnv = that.excludedInEnv;
        this.sharing = that.sharing;
    }

    /**
     * apply record to target
     *
     * @param record record to apply
     */
    @Override
    public ProjectInfo withRecord(ProjectRecord record) {
        ProjectInfo target = new ProjectInfo(this);

        target.id = record.getId();
        if (record.getProjectNameGenerator() != null) {
            target.projectNameGenerator = record.getProjectNameGenerator().toClosure();
        }
        if (record.getVariableGroupGenerator() != null) {
            target.variableGroupGenerator = record.getVariableGroupGenerator().toClosure();
        }
        if (record.getActiveInEnv() != null) {
            target.activeInEnv = record.getActiveInEnv();
        }
        if (record.getDescription() != null) {
            target.description = record.getDescription();
        }
        if (record.getParents() != null) {
            target.parents = record.getParents();
        }
        if (record.getAbstracted() != null) {
            target.abstracted = record.getAbstracted();
        }
        if (record.getPlaybookName() != null) {
            target.playbookName = record.getPlaybookName();
        }
        if (record.getWhen() != null) {
            target.when = Lambda.map(record.getWhen(), ClosureInfo::toClosure);
        }
        if (record.getIncludedInEnv() != null) {
            target.includedInEnv = record.getIncludedInEnv();
        }
        if (record.getIncludedOnlyInEnv() != null) {
            target.includedOnlyInEnv = record.getIncludedOnlyInEnv();
        }
        if (record.getExcludedInEnv() != null) {
            target.excludedInEnv = record.getExcludedInEnv();
        }
        if (record.getSharing() != null) {
            target.sharing = record.getSharing();
        }

        return target;
    }

    public ProjectInfo withAssignRecords(List<Assignment> assigns) {

        List<VariableInfo> variableInfos = this.vars.toInfo();

        Map<String, List<Assignment>> map = new HashMap<>();
        for (Assignment assign : assigns) {
            if (assign.getEnvName() == null) {
                assign.insertToVariableInfoList(variableInfos);
            } else {
                map.computeIfAbsent(assign.getEnvName(), k -> new LinkedList<>()).add(assign);
            }
        }

        ProjectInfo target = new ProjectInfo(this);
        target.overrides = new ArrayList<>(this.overrides);
        target.vars = new SimpleVariables(variableInfos);

        for (Map.Entry<String, List<Assignment>> entry : map.entrySet()) {
            EnvironmentQuery query = new EnvironmentQuery(entry.getKey());
            int index = Lambda.findIndexOf(target.overrides, o -> o.getQuery().equals(query));
            List<VariableInfo> list;
            if (index < 0) {
                list = new ArrayList<>();
            } else {
                list = target.getOverrides().get(index).getVars().toInfo();
            }
            for (Assignment assign : entry.getValue()) {
                assign.insertToVariableInfoList(list);
            }

            ProjectOverride override = new ProjectOverride(new EnvironmentQuery(entry.getKey()), new SimpleVariables(list));
            if (index < 0) {
                target.overrides.set(index, override);
            } else {
                target.overrides.add(override);
            }
        }

        return target;
    }

    public ProjectOverride getOrCreateOverride(EnvironmentQuery query) {
        for (ProjectOverride o : this.overrides) {
            if (o.getQuery().equals(query)) {
                return o;
            }
        }

        ProjectOverride override = new ProjectOverride(query, new SimpleVariables());
        this.overrides.add(override);
        return override;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public ClosureWrapper<String> getVariableGroupGenerator() {
        return variableGroupGenerator;
    }

    public void setVariableGroupGenerator(ClosureWrapper<String> variableGroupGenerator) {
        this.variableGroupGenerator = variableGroupGenerator;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getActiveInEnv() {
        return activeInEnv;
    }

    public void setActiveInEnv(String activeInEnv) {
        this.activeInEnv = activeInEnv;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getParents() {
        return parents;
    }

    public Boolean isAbstracted() {
        return abstracted;
    }

    public void setAbstracted(Boolean abstracted) {
        this.abstracted = abstracted;
    }

    public SimpleVariables getVars() {
        return vars;
    }

    public List<ProjectOverride> getOverrides() {
        return overrides;
    }

    public String getPlaybookName() {
        return playbookName;
    }

    public void setPlaybookName(String playbookName) {
        this.playbookName = playbookName;
    }

    /**
     * if not null, only the result of the closure be true then create a job to a specified job
     */
    public List<ClosureWrapper<Boolean>> getWhen() {
        return when;
    }

    public void setWhen(List<ClosureWrapper<Boolean>> when) {
        this.when = when;
    }

    public List<String> getIncludedInEnv() {
        return includedInEnv;
    }

    public void setIncludedInEnv(List<String> includedInEnv) {
        this.includedInEnv = includedInEnv;
    }

    public List<String> getIncludedOnlyInEnv() {
        return includedOnlyInEnv;
    }

    public void setIncludedOnlyInEnv(List<String> includedOnlyInEnv) {
        this.includedOnlyInEnv = includedOnlyInEnv;
    }

    public List<String> getExcludedInEnv() {
        return excludedInEnv;
    }

    public void setExcludedInEnv(List<String> excludedInEnv) {
        this.excludedInEnv = excludedInEnv;
    }

    public LinkedHashMap<String, String> getSharing() {
        return sharing;
    }

    public ClosureWrapper<String> getProjectNameGenerator() {
        return projectNameGenerator;
    }

    public void setProjectNameGenerator(ClosureWrapper<String> projectNameGenerator) {
        this.projectNameGenerator = projectNameGenerator;
    }

    public List<ProjectInfo> descending(ProjectInfoAccessor accessor) {
        return Lambda.map(Inherits.descending(
            projectName,
            p -> accessor.getProject(p).parents.iterator()),
            accessor::getProject
        );
    }

    public List<ProjectInfo> descendingParents(ProjectInfoAccessor accessor) {
        return Lambda.map(Inherits.descendingParents(
            projectName,
            p -> accessor.getProject(p).parents.iterator()),
            accessor::getProject
        );
    }

    public <T> T nearest(ProjectInfoAccessor accessor, Function<ProjectInfo, T> f) {
        return Inherits.nearest(
            projectName,
            p -> accessor.getProject(p).parents.iterator(),
            p -> f.apply(accessor.getProject(p))
        );
    }

    public Environment getFinalActiveInEnv(ProjectInfoAccessor accessor, Environments envs) {
        String envName = nearest(accessor, ProjectInfo::getActiveInEnv);
        if (envName == null) {
            throw new IllegalConfigException("activeInEnv is not configured for project " + projectName);
        }
        return envs.get(envName);
    }

    public String getFinalPlaybookName(ProjectInfoAccessor accessor) {
        return nearest(accessor, ProjectInfo::getPlaybookName);
    }

    public LayeredVariables getProjectVars(ProjectInfoAccessor accessor, Environments envs) {
        LayeredVariables vars = new LayeredVariables();
        refreshProjectVars(vars, accessor, envs);
        return vars;
    }

    public LayeredVariables getProjectVars(ProjectInfoAccessor accessor,
                               EnvironmentQuery query,
                               Environments envs) {
        LayeredVariables vars = new LayeredVariables();
        refreshProjectVars(vars, accessor, query, envs);
        return vars;
    }

    public void refreshProjectVars(LayeredVariables projectVars, ProjectInfoAccessor accessor, Environments envs) {
        refreshProjectVars(
            projectVars,
            accessor,
            new EnvironmentQuery(getFinalActiveInEnv(accessor, envs).getName()), envs);
    }

    public void refreshProjectVars(LayeredVariables projectVars,
                                   ProjectInfoAccessor accessor,
                                   EnvironmentQuery query,
                                   Environments envs) {
        projectVars.clearLayers();

        for (ProjectInfo p : descending(accessor)) {
            p.refreshDeclaredProjectVars(projectVars, query, envs);
        }
    }

    private void refreshDeclaredProjectVars(LayeredVariables projectVars,
                                        EnvironmentQuery query,
                                        Environments envs) {
        projectVars.layer(this.vars);

        for (ProjectOverride override : this.overrides) {
            if (override.getQuery().includes(query, envs)) {
                projectVars.layer(override.getVars());
            }
        }
    }

    public Map<String, Object> getPlaybookParams(LayeredVariables projectVars) {
        PlaybookParameterEvaluationContext pcx = new PlaybookParameterEvaluationContext(projectVars);
        return pcx.getVariableOrDefault(Project.PLAYBOOK_PARAMETERS_VARIABLE, Collections.emptyMap());
    }

    public void refreshPlaybookVars(Map<String, Object> parameters,
                                    LayeredVariables playbookVars,
                                    Playbooks playbooks,
                                    ProjectInfoAccessor accessor) {

        playbookVars.clearLayers();

        String finalPlaybookName = getFinalPlaybookName(accessor);
        if (finalPlaybookName == null) {
            return;
        }

        Playbook playbook = playbooks.getOrCreate(finalPlaybookName, parameters);
        playbookVars.layer(playbook.getVars());
    }

    public ProjectPlaybookInfo getProjectPlaybookInfo(ProjectInfoAccessor accessor, Environments envs) {
        ProjectPlaybookInfo info = new ProjectPlaybookInfo();
        info.setPlaybookName(getFinalPlaybookName(accessor));
        info.setPlaybookParams(getPlaybookParams(getProjectVars(accessor, envs)));

        List<EnvironmentQuery> queries = new ArrayList<>();

        for (ProjectInfo p : descending(accessor)) {
            for (ProjectOverride po : p.overrides) {
                if (!queries.contains(po.getQuery())) {
                    queries.add(po.getQuery());
                }
            }
        }

        info.setOverrides(Lambda.map(queries, q ->
            new ProjectPlaybookOverrideInfo(q, getPlaybookParams(getProjectVars(accessor, q, envs)))));

        return info;
    }
}
