package com.yit.deploy.core.model;

import com.yit.deploy.core.algorithm.Graph;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.function.Holder;
import com.yit.deploy.core.info.HostGroupInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public class HostGroups implements Iterable<HostGroup> {

    private final Map<String, HostGroup> map;

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public HostGroups(Map<String, HostGroupInfo> infoMap, Hosts hosts) {
        this.map = new HashMap<>(infoMap.size());

        Holder<Consumer<String>> convert = new Holder<>();
        convert.data = name -> {
            HostGroupInfo info = infoMap.get(name);

            for (String hgi : info.getInherits()) {
                if (!this.map.containsKey(hgi)) {
                    convert.data.accept(hgi);
                }
            }
            for (String hgi : info.getInheritsRetired()) {
                if (!this.map.containsKey(hgi)) {
                    convert.data.accept(hgi);
                }
            }
            HostGroup hg = new HostGroup(info, this, hosts);
            this.put(hg);
        };

        for (String key : infoMap.keySet()) {
            if (!map.containsKey(key)) {
                convert.data.accept(key);
            }
        }

        // host group [retired] inherit graph. parent -> child
        Graph<HostGroup, HostGroupInheritType> graph = new Graph<>();
        for (HostGroup hg : this.map.values()) {
            graph.node(hg.getName(), hg);
        }
        for (HostGroup c : this.map.values()) {
            for (HostGroup p : c.getInherits()) {
                graph.arc(p.getName(), c.getName(), HostGroupInheritType.inherit);
            }
        }

        for (HostGroup hg : this.map.values()) {
            hg.initialize(graph);
        }
    }

    public HostGroup get(String name) {
        if (name == null) {
            return null;
        }

        HostGroup hostGroup = map.get(name);
        if (hostGroup == null) {
            throw new IllegalConfigException("invalid host group " + name);
        }
        return hostGroup;
    }

    public boolean contains(String name) {
        return map.containsKey(name);
    }

    public void put(HostGroup hostGroup) {
        map.put(hostGroup.getName(), hostGroup);
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<HostGroup> iterator() {
        return map.values().iterator();
    }

    public Collection<String> names() {
        return map.keySet();
    }
}
