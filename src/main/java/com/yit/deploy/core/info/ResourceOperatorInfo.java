package com.yit.deploy.core.info;

import com.yit.deploy.core.function.ClosureWrapper;

public class ResourceOperatorInfo {
    private ClosureWrapper acquire;
    private ClosureWrapper release;

    public ClosureWrapper getAcquire() {
        return acquire;
    }

    public void setAcquire(ClosureWrapper acquire) {
        this.acquire = acquire;
    }

    public ClosureWrapper getRelease() {
        return release;
    }

    public void setRelease(ClosureWrapper release) {
        this.release = release;
    }
}