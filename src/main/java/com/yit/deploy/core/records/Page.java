package com.yit.deploy.core.records;

import java.util.List;

public class Page<T> {
    private final int pageIndex;
    private final int pageSize;
    private final long totalCount;
    private final List<T> data;

    public Page(int pageIndex, int pageSize, long totalCount, List<T> data) {
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.data = data;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public List<T> getData() {
        return data;
    }
}
