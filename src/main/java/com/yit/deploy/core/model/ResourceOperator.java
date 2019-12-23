package com.yit.deploy.core.model;

import com.yit.deploy.core.dsl.BaseContext;
import com.yit.deploy.core.dsl.evaluate.EvaluationContext;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.ResourceOperatorInfo;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class ResourceOperator {
    private final ClosureWrapper acquire;
    private final ClosureWrapper release;

    public ResourceOperator(ResourceOperatorInfo info) {
        this.acquire = info.getAcquire();
        this.release = info.getRelease();
    }

    public ClosureWrapper getAcquire() {
        return acquire;
    }

    public ClosureWrapper getRelease() {
        return release;
    }

    @SuppressWarnings("unchecked")
    private static void acquireResource(Map<String, ResourceOperator> resourceOperators, BaseContext context, String key) {
        ResourceOperator op = resourceOperators.get(key);
        if (op == null || op.acquire == null) {
            throw new IllegalStateException("acquire method for resource " + key + " is not defined");
        }
        op.acquire.with(context);
    }

    @SuppressWarnings("unchecked")
    private static void releaseResource(Map<String, ResourceOperator> resourceOperators, BaseContext context, String key) {
        ResourceOperator op = resourceOperators.get(key);
        if (op == null || op.release == null) {
            throw new IllegalStateException("release method for resource " + key + " is not defined");
        }
        op.release.with(context);
    }

    public static <T> void using(List<T> payloads,
                                 Map<String, ResourceOperator> resourceOperators,
                                 EvaluationContext context,
                                 Function<T, List<String>> requiredResourcesGetter,
                                 Consumer<T> executor) {

        List<List<String>> index2Resources =Lambda.map(payloads, requiredResourcesGetter);

        // at which index in the tasks a resource needed to be acquired
        Map<String, Integer> acquireResourceIndexMap = new HashMap<>();
        for (int i = 0; i < index2Resources.size(); i++) {
            List<String> resources = index2Resources.get(i);
            if (resources == null) continue;
            for (String r : resources) {
                if (!acquireResourceIndexMap.containsKey(r)) {
                    acquireResourceIndexMap.put(r, i);
                }
            }
        }

        // at which index in the tasks a resource needed to be released
        Map<String, Integer> releaseResourceIndexMap = new HashMap<>();
        for (int i = index2Resources.size() - 1; i >= 0; i--) {
            List<String> resources = index2Resources.get(i);
            if (resources == null) continue;
            for (String r : resources) {
                if (!releaseResourceIndexMap.containsKey(r)) {
                    releaseResourceIndexMap.put(r, i);
                }
            }
        }

        // the keys of these two map must be the same
        assert acquireResourceIndexMap.size() == releaseResourceIndexMap.size();

        /*
         * returns which resources need to be acquired / released in the task at an index
         * resources required by child task will be regarded as required by its top level task,
         * despite whether the child task will actually be executed.
         */

        Map<Integer, Set<String>> acquireResourceMap = Lambda.reverseMap(acquireResourceIndexMap);
        Map<Integer, Set<String>> releaseResourceMap = Lambda.reverseMap(releaseResourceIndexMap);

        List<String> acquiredResources = new ArrayList<>();
        try {
            for (int i = 0; i < payloads.size(); i++) {
                Set<String> resourcesToAcquire = acquireResourceMap.get(i);
                if (resourcesToAcquire != null) {
                    // try to avoid dead lock by sorting the orders in which to acquire resources
                    for (String r : Lambda.sort(resourcesToAcquire)) {
                        acquireResource(resourceOperators, context, r);
                        acquiredResources.add(r);
                    }
                }
                try {
                    executor.accept(payloads.get(i));
                } finally {
                    Set<String> resourcesToRelease = releaseResourceMap.get(i);
                    if (resourcesToRelease != null) {
                        for (String r : resourcesToRelease) {
                            acquiredResources.remove(r);
                            releaseResource(resourceOperators, context, r);
                        }
                    }
                }
            }
        } finally {
            for (int i = acquiredResources.size() - 1; i >= 0; i--) {
                String r = acquiredResources.get(i);
                try {
                    releaseResource(resourceOperators, context, r);
                } catch (Exception e) {
                    context.getScript().warn("release resource %s failed: %s", r, e);
                }
            }
        }
    }
}