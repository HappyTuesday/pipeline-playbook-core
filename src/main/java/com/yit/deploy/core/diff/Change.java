package com.yit.deploy.core.diff;

import java.io.Serializable;

public class Change implements Serializable {

    private static final String SEP = "/";

    private String path;
    private ChangeType type;
    /**
     * used when type is setProperty, insertElement or setElement
     */
    private String value;
    /**
     * used when type is swapElement
     */
    private int from;

    public boolean match(String pattern) {
        return match(pattern, 0, 0);
    }

    public boolean match(String pattern, int i, int j) {
        for (;; i++, j++) {
            if (i == path.length() ^ j == pattern.length()) {
                return false;
            }
            if (i == path.length()) {
                return true;
            }

            char b = pattern.charAt(j);
            char c = j + 1 < pattern.length() ? pattern.charAt(j + 1) : 0;

            if (b == '*') {
                if (c == '*') {
                    for (j += 2; i <= path.length(); i++) {
                        if (match(pattern, i, j)) return true;
                    }
                    return false;
                }

                for (j++; i <= path.length(); i++) {
                    if (i < path.length() && path.charAt(i) == '/') return false;
                    if (match(pattern, i, j)) return true;
                }
                return false;
            }

            char a = path.charAt(i);
            if (!(a == b || a != '/' && b == '?')) {
                return false;
            }
        }
    }

    public static Change changed(String path, String newValue) {
        Change c = new Change();
        c.path = path;
        c.type = ChangeType.change;
        c.value = newValue;
        return c;
    }

    public static Change inserted(String path, Object index, String element) {
        Change c = new Change();
        c.path = path + SEP + index;
        c.type = ChangeType.insert;
        c.value = element;
        return c;
    }

    public static Change removed(String path, Object index) {
        Change c = new Change();
        c.path = path + SEP + index;
        c.type = ChangeType.remove;
        return c;
    }

    public static Change swapped(String path, int index, int from) {
        Change c = new Change();
        c.path = path + SEP + index;
        c.type = ChangeType.swap;
        c.from = from;
        return c;
    }

    public ChangeType getType() {
        return type;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getFrom() {
        return from;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}