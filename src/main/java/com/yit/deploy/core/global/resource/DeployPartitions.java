package com.yit.deploy.core.global.resource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DeployPartitions {

    private final Map<String, Partition> partitions = new ConcurrentHashMap<>();

    public void create(String partition, Collection<String> members) {
        // remove old un-removed partition
        partitions.compute(partition, (name, v) -> {
            if (v != null) {
                v.clear();
            }
            return new Partition(name, new HashSet<>(members));
        });
    }

    public void join(String partition, String member, Consumer<Collection<String>> beforeWait) {
        final Partition p = partitions.get(partition);
        if (p != null) {
            p.join(member, beforeWait);
        }
    }

    public void remove(String name) {
        partitions.computeIfPresent(name, (k, v) -> {
            v.clear();
            return null;
        });
    }

    static class Partition {
        final String name;
        final Set<String> members;
        final Set<String> wait;

        Partition(String name, Set<String> members) {
            this.name = name;
            this.members = members;
            this.wait = new HashSet<>(members);
        }

        synchronized void join(String member, Consumer<Collection<String>> beforeWait) {
            wait.remove(member);
            if (wait.isEmpty()) {
                notifyAll();
                return;
            }
            if (beforeWait != null) {
                beforeWait.accept(wait);
            }
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        synchronized void clear() {
            notifyAll();
        }
    }
}
