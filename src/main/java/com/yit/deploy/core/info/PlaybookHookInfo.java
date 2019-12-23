package com.yit.deploy.core.info;

import com.yit.deploy.core.function.ClosureWrapper;

/**
 * Created by nick on 21/09/2017.
 */
public class PlaybookHookInfo {
    private ClosureWrapper setup;
    private ClosureWrapper teardown;

    public ClosureWrapper getSetup() {
        return setup;
    }

    public void setSetup(ClosureWrapper setup) {
        this.setup = setup;
    }

    public ClosureWrapper getTeardown() {
        return teardown;
    }

    public void setTeardown(ClosureWrapper teardown) {
        this.teardown = teardown;
    }
}
