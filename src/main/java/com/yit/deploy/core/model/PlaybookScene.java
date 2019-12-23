package com.yit.deploy.core.model;

import com.yit.deploy.core.info.PlaybookSceneInfo;

import java.util.List;

/**
 * Describe a scene to use a playbookName
 */
public class PlaybookScene {
    private final String name;
    private final List<String> plays;
    private final List<String> tasksToSkip;

    public PlaybookScene(PlaybookSceneInfo info) {
        this.name = info.getName();
        this.plays = info.getPlays();
        this.tasksToSkip = info.getTasksToSkip();
    }

    public String getName() {
        return name;
    }

    public List<String> getPlays() {
        return plays;
    }

    public List<String> getTasksToSkip() {
        return tasksToSkip;
    }
}
