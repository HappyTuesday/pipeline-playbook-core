package com.yit.deploy.core.info;

import java.util.ArrayList;
import java.util.List;

/**
 * Describe a scene to use a playbookName
 */
public class PlaybookSceneInfo {
    private final String name;
    private final List<String> plays = new ArrayList<>();
    private final List<String> tasksToSkip = new ArrayList<>();

    public PlaybookSceneInfo(String name) {
        this.name = name;
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
