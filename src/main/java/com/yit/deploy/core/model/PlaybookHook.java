package com.yit.deploy.core.model;

import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.info.PlaybookHookInfo;

import java.io.Serializable;

/**
 * Created by nick on 21/09/2017.
 */
public class PlaybookHook implements Serializable {
    private final ClosureWrapper setup;
    private final ClosureWrapper teardown;

    public PlaybookHook(PlaybookHookInfo info) {
        this.setup = info.getSetup();
        this.teardown = info.getTeardown();
    }

    public ClosureWrapper getSetup() {
        return setup;
    }

    public ClosureWrapper getTeardown() {
        return teardown;
    }
}
