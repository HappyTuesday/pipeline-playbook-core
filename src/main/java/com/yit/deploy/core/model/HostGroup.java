package com.yit.deploy.core.model;

import com.yit.deploy.core.algorithm.Graph;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.HostGroupInfo;

import java.util.*;
import java.util.function.Consumer;

public class HostGroup {
    private final String name;
    private final String description;
    private final transient List<Host> selfHosts;
    private final transient List<HostGroup> inherits;
    private final transient List<HostGroup> inheritsRetired;

    private List<Host> inclusiveHosts; // only be updated in initialize
    private List<Host> exclusiveHosts; // only be updated in initialize

    public HostGroup(HostGroupInfo info, HostGroups hostGroups, Hosts hosts) {
        this.name = info.getName();
        this.description = info.getDescription();
        this.selfHosts = Lambda.map(Lambda.unique(info.getHosts()), hosts::get);

        for (String name : info.getHostsRetired()) {
            boolean found = false;
            for (int i = 0; i < this.selfHosts.size(); i++) {
                Host h = this.selfHosts.get(i);
                if (h.getName().equals(name)) {
                    if (!h.isRetired()) {
                        this.selfHosts.set(i, h.toRetired(true));
                    }
                    found = true;
                }
            }
            if (!found) {
                this.selfHosts.add(hosts.get(name).toRetired(true));
            }
        }

        this.inherits = Lambda.map(info.getInherits(), hostGroups::get);
        this.inheritsRetired = Lambda.map(info.getInheritsRetired(), hostGroups::get);
    }

    public void initialize(Graph<HostGroup, HostGroupInheritType> g) {
        getExclusiveHosts(g);
    }

    public List<Host> getHosts() {
        if (exclusiveHosts == null) {
            throw new IllegalStateException("host group is not initialized");
        }
        return exclusiveHosts;
    }

    private List<Host> getInclusiveHosts() {
        if (inclusiveHosts != null) {
            return inclusiveHosts;
        }

        Map<String, Integer> index = new HashMap<>();
        List<Host> list = new ArrayList<>();

        Consumer<Host> insert = h -> {
            Integer i = index.get(h.getName());
            if (i == null) {
                index.put(h.getName(), list.size());
                list.add(h);
            } else {
                list.set(i, h);
            }
        };

        for (HostGroup hg : inherits) {
            Lambda.foreach(hg.getInclusiveHosts(), insert);
        }
        for (HostGroup hg : inheritsRetired) {
            Lambda.foreach(hg.getInclusiveHosts(), h -> insert.accept(h.toRetired(true)));
        }
        Lambda.foreach(selfHosts, insert);

        inclusiveHosts = list;
        return list;
    }

    private List<Host> getExclusiveHosts(Graph<HostGroup, HostGroupInheritType> g) {
        if (exclusiveHosts != null) {
            return exclusiveHosts;
        }

        List<Host> list = new ArrayList<>();
        Map<String, Integer> index = new HashMap<>();
        Consumer<Host> insert = h -> {
            Integer i = index.get(h.getName());
            if (i == null) {
                index.put(h.getName(), list.size());
                list.add(h);
            } else {
                list.set(i, h.toRetired(list.get(i).isRetired() && h.isRetired()));
            }
        };

        Lambda.foreach(getInclusiveHosts(), insert);

        for (String nextName : g.next(name)) {
            Lambda.foreach(g.getAt(nextName).getExclusiveHosts(g), insert);
        }

        exclusiveHosts = list;
        return exclusiveHosts;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HostGroup && name.equals(((HostGroup)obj).name);
    }

    @Override
    public String toString() {
        String s = name;
        if (!selfHosts.isEmpty()) s += " " + Lambda.toString(selfHosts);
        if (!inherits.isEmpty()) s += " < " + Lambda.toString(inherits);
        if (!inheritsRetired.isEmpty()) s += " <- " + Lambda.toString(inheritsRetired);
        return s;
    }

    public String getName() {
        return name;
    }

    public List<HostGroup> getInherits() {
        return inherits;
    }

    public List<HostGroup> getInheritsRetired() {
        return inheritsRetired;
    }
}
