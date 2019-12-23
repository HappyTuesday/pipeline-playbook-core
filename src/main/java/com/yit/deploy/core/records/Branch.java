package com.yit.deploy.core.records;

public class Branch {
    private String name;
    private long head;

    public Branch() {
    }

    public Branch(String name, long head) {
        this.name = name;
        this.head = head;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getHead() {
        return head;
    }

    public void setHead(long head) {
        this.head = head;
    }
}
