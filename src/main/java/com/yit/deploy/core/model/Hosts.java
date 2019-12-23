package com.yit.deploy.core.model;

import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.HostInfo;
import com.yit.deploy.core.variables.variable.ContextualVariable;
import groovy.lang.GString;

import java.util.*;

public class Hosts implements Iterable<Host> {

    private final Map<String, Host> map;

    public Hosts(Map<String, HostInfo> infoMap, Environment env) {
        this.map = new HashMap<>(infoMap.size());
        for (String key : infoMap.keySet()) {
            this.map.put(key, new Host(infoMap.get(key), env));
        }
    }

    public Host get(String name) {
        if (name == null) {
            return null;
        }

        Host host = map.get(name);
        if (host == null) {
            throw new IllegalConfigException("invalid host " + name);
        }
        return host;
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
    public Iterator<Host> iterator() {
        return map.values().iterator();
    }

    public Collection<String> names() {
        return map.keySet();
    }

    public List<Host> filterByLabels(Map<String, Object> labelsToMatch) {
        return Lambda.findAll(map.values(),
            h -> Lambda.all(labelsToMatch.entrySet(),
                label -> compareLabelValue(
                    label.getValue(),
                    h.getLabels().get(label.getKey())
                )
            )
        );
    }

    public List<Host> filterByLabels(List<String> labelsBeIncluded) {
        return Lambda.findAll(map.values(),
            h-> Lambda.all(labelsBeIncluded, l -> h.getLabels().containsKey(l))
        );
    }

    public static boolean compareLabelValue(Object x, Object y) {
        if (x == y) {
            return true;
        }

        if (x instanceof GString) {
            x = x.toString();
        }
        if (y instanceof GString) {
            y = y.toString();
        }

        if (x instanceof ContextualVariable) {
            x = ((ContextualVariable) x).concrete();
        }
        if (y instanceof ContextualVariable) {
            y = ((ContextualVariable) y).concrete();
        }

        if (x == y) {
            return true;
        }

        if (x == null || y == null) {
            return false;
        }

        if (x.equals(y)) {
            return true;
        }

        if (x instanceof Iterable && y instanceof Iterable) {
            if (x instanceof Collection &&
                y instanceof Collection &&
                ((Collection) x).size() != ((Collection) y).size()) {

                return false;
            }

            Iterator<?> xi = ((Iterable) x).iterator(), yi = ((Iterable) y).iterator();
            while (true) {
                if (xi.hasNext()) {
                    if (!yi.hasNext() || !compareLabelValue(xi.next(), yi.next())) {
                        return false;
                    }
                } else if (yi.hasNext()) {
                    return false;
                } else {
                    break;
                }
            }

            return true;
        }

        if (x instanceof Map.Entry && y instanceof Map.Entry) { // in case the map is considered as an iterable
            Map.Entry<?, ?> xe = (Map.Entry) x, ye = (Map.Entry) y;
            return compareLabelValue(xe.getKey(), ye.getKey()) && compareLabelValue(xe.getValue(), ye.getValue());
        }

        if (x instanceof Map && y instanceof Map) {
            Map<?, ?> xm = (Map) x, ym = (Map) y;
            if (xm.size() != ym.size()) {
                return false;
            }

            for (Map.Entry<?, ?> entry : xm.entrySet()) {
                if (!compareLabelValue(entry.getValue(), ym.get(entry.getKey()))) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }
}
