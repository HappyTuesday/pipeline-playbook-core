package com.yit.deploy.core.dsl.parse;

import com.yit.deploy.core.dsl.BaseContext;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.info.PlaybookInfo;
import com.yit.deploy.core.info.PlaybookSceneInfo;

import java.util.Arrays;
import java.util.List;

public class PlaybookSceneContext extends BaseContext {
    private final PlaybookSceneInfo scene;

    public PlaybookSceneContext(PlaybookInfo playbook, String name, List<String> plays) {
        if (playbook.getScenes().containsKey(name)) {
            throw new IllegalConfigException("playbook scene is already defined in playbook " + playbook.getName());
        }

        this.scene = new PlaybookSceneInfo(name);
        this.scene.getPlays().addAll(plays);
        playbook.getScenes().put(name, this.scene);
    }

    public PlaybookSceneContext skip(String ... tasks) {
        scene.getTasksToSkip().addAll(Arrays.asList(tasks));
        return this;
    }
}