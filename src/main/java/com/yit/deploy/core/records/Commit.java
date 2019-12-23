package com.yit.deploy.core.records;

import java.util.Date;

public class Commit {
    private long id;
    private String author;
    private String comment;
    private Date timestamp;
    private Long parentId;

    public Commit() {
    }

    public Commit(Commit that) {
        this.id = that.id;
        this.author = that.author;
        this.comment = that.comment;
        this.timestamp = that.timestamp;
        this.parentId = that.parentId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
