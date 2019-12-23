package com.yit.deploy.core.dsl.parse;

import com.yit.deploy.core.dsl.BaseScript;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.exceptions.MissingProjectException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.ProjectInfo;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.info.ProjectInfoAccessor;
import com.yit.deploy.core.inherits.Inherits;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.model.Environments;
import com.yit.deploy.core.model.Playbooks;
import com.yit.deploy.core.support.VariableValueTypesSupport;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public abstract class ProjectsBaseScript extends BaseScript implements VariableValueTypesSupport {
    private ProjectInfo parent;
    private Environments envs;
    private Playbooks playbooks;
    private ProjectInfoAccessor accessor;
    private List<Scope> scopes;
    private Map<String, ProjectInfo> draft;
    private List<HistoryItem> history;

    public Collection<ProjectInfo> parse(ProjectInfo parent, Environments envs, Playbooks playbooks, ProjectInfoAccessor accessor) {
        this.parent = parent;
        this.envs = envs;
        this.playbooks = playbooks;
        this.scopes = Collections.emptyList();
        this.draft = new HashMap<>();
        this.accessor = accessor.with(this.draft);
        this.history = new LinkedList<>();

        resolveVars(parent.getFinalActiveInEnv(accessor, envs).getVars());

        run();

        return this.draft.values();
    }

    public void $(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = KeyedProjectDSL.class) Closure closure) {
        scopes = new ArrayList<>(scopes);
        scopes.add(new Scope(null, closure));
    }

    public void scope(
            @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = KeyedProjectDSL.class) Closure scopeDefaultClosure,
            Runnable runnable) {

        List<Scope> origin = scopes;
        scopes = new ArrayList<>(scopes);
        scopes.add(new Scope(null, scopeDefaultClosure));
        runnable.run();
        scopes = origin;
    }

    public void scope(Runnable runnable) {
        scope(null, runnable);
    }

    public DefaultsToContext defaults(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = KeyedProjectDSL.class) Closure scopeDefaultClosure) {
        return new DefaultsToContext(scopeDefaultClosure);
    }

    public void section(String sectionName, Runnable runnable) {
        scope(delegateClosure(c -> c.section(sectionName)), runnable);
    }

    public void group(String groupName, Runnable runnable) {
        scope(delegateClosure(c -> c.group(groupName)), runnable);
    }

    public void jobOrder(int order, Runnable runnable) {
        scope(delegateClosure(c -> c.jobOrder(order)), runnable);
    }

    public void playbook(String playbookName, Runnable runnable) {
        scope(delegateClosure(c -> c.playbook(playbookName)), runnable);
    }

    public void inherits(String parent, Closure closure) {
        accessor.getProject(parent); // check if parent exists
        List<Scope> origin = scopes;
        scopes = new ArrayList<>(scopes);
        scopes.add(new Scope(parent, null));
        closure.run();
        scopes = origin;
    }

    public InheritsForeachContext inherits(@Nullable @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = InheritsFromContext.class) Closure closure) {
        InheritsFromContext context = new InheritsFromContext();
        Closures.delegateOnly(context, closure);
        return new InheritsForeachContext(context.froms);
    }

    private static class InheritSpec {
        final String parent;
        final Closure closure;

        InheritSpec(String parent, Closure closure) {
            this.parent = parent;
            this.closure = closure;
        }
    }

    public class InheritsFromContext {
        final List<InheritSpec> froms = new LinkedList<>();

        public void from(String parent) {
            from(parent, null);
        }

        public void from(String parent, @Nullable @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = KeyedProjectDSL.class) Closure closure) {
            accessor.getProject(parent); // check if parent exists
            froms.add(new InheritSpec(parent, closure));
        }
    }

    public class InheritsForeachContext {

        final List<InheritSpec> froms;

        private InheritsForeachContext(List<InheritSpec> froms) {
            this.froms = froms;
        }

        public void foreach(Closure closure) {
            List<Scope> origin = scopes;
            scopes = new ArrayList<>(scopes);
            for (InheritSpec spec : froms) {
                scopes.add(new Scope(spec.parent, spec.closure));
                closure.run();
                scopes.remove(scopes.size() - 1);
            }
            scopes = origin;
        }
    }

    public KeyedProjectDSL project(String key) {
        return project(key, null);
    }

    public void project(String... keys) {
        for (String key : keys) {
            project(key);
        }
    }

    public KeyedProjectDSL project(String key, @Nullable @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = KeyedProjectDSL.class) Closure closure) {
        if (key == null) {
            throw new IllegalConfigException("key must not be null while creating child project for " + parent.getProjectName());
        }

        ProjectInfo child = new ProjectInfo();
        child.setKey(key);

        ProjectInfoAccessor childAccessor = this.accessor.with(child);

        child.getParents().add(parent.getProjectName());
        for (Scope scope : scopes) {
            if (scope.parentToInherit != null) {
                child.getParents().add(scope.parentToInherit);
            }
        }

        // note: projectName is not set yet
        ClosureWrapper<String> generator = Inherits.nearestInParents(
            child,
            p -> Lambda.map(p.getParents().iterator(), childAccessor::getProject),
            ProjectInfo::getProjectNameGenerator
        );

        if (generator == null) {
            throw new IllegalConfigException("could not find any project name generator in project " + parent.getProjectName() + " or its parents");
        }

        String projectName = String.valueOf(generator.call(key));
        child.setProjectName(projectName);

        ProjectContext context = new ProjectContext(child, envs, playbooks, childAccessor);
        for (Scope scope : scopes) {
            if (scope.closure != null) {
                Closures.delegateOnly(context, scope.closure);
            }
        }
        if (closure != null) {
            Closures.delegateOnly(context, closure);
        }

        if (this.accessor.exist(child.getProjectName())) {
            throw new IllegalConfigException("project " + projectName + " is already defined previously");
        }

        history.add(new HistoryItem(child, scopes));
        this.draft.put(child.getProjectName(), child);

        return context;
    }

    public KeyedProjectDSL abstractProject(String key) {
        KeyedProjectDSL dsl = project(key);
        dsl.getProject().setAbstracted(true);
        return dsl;
    }

    public KeyedProjectDSL abstractProject(String key, @Nullable @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = KeyedProjectDSL.class) Closure closure) {
        KeyedProjectDSL dsl = project(key, closure);
        dsl.getProject().setAbstracted(true);
        return dsl;
    }

    public KeyedProjectDSL extendProject(String projectName) {
        return extendProject(projectName, null);
    }

    public KeyedProjectDSL extendProject(String projectName, @Nullable @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = KeyedProjectDSL.class) Closure closure) {
        if (projectName == null) {
            throw new IllegalArgumentException("project name could not be null");
        }

        HistoryItem item = Lambda.find(history, x -> projectName.equals(x.project.getProjectName()));
        if (item == null) {
            throw new IllegalConfigException("project " + projectName + " is not defined in this script");
        }

        for (Scope scope : scopes) {
            if (scope.parentToInherit != null && !item.appliedScopes.contains(scope)) {
                item.project.getParents().add(scope.parentToInherit);
            }
        }

        ProjectContext context = new ProjectContext(item.project, envs, playbooks, this.accessor);

        for (Scope scope : scopes) {
            if (scope.closure != null && !item.appliedScopes.contains(scope)) {
                Closures.delegateOnly(context, scope.closure);
            }
        }

        if (closure != null) {
            Closures.delegateOnly(context, closure);
        }

        return context;
    }

    public class DefaultsToContext {
        private final Closure scopeDefaultClosure;

        DefaultsToContext(Closure scopeDefaultClosure) {
            this.scopeDefaultClosure = scopeDefaultClosure;
        }

        public void to(Closure closure) {
            scope(scopeDefaultClosure, closure::call);
        }
    }

    private static Closure<?> delegateClosure(Function<ProjectContext, ?> def) {
        return Closures.delegateClosure(def);
    }

    private static class HistoryItem {
        final ProjectInfo project;
        final List<Scope> appliedScopes;

        HistoryItem(ProjectInfo project, List<Scope> appliedScopes) {
            this.project = project;
            this.appliedScopes = appliedScopes;
        }
    }

    private static class Scope {
        final String parentToInherit;
        final Closure closure;

        public Scope(String parentToInherit, Closure closure) {
            this.parentToInherit = parentToInherit;
            this.closure = closure;
        }
    }
}