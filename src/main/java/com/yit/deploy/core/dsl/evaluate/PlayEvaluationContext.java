package com.yit.deploy.core.dsl.evaluate;

import com.yit.deploy.core.model.Job;
import com.yit.deploy.core.model.Play;

public class PlayEvaluationContext extends JobEvaluationContext {

    private final Play play;

    public PlayEvaluationContext(JobEvaluationContext cx, Play play) {
        super(cx);
        this.play = play;
        resolveVars(play.getVars());
    }

    public Play getPlay() {
        return play;
    }

    @Override
    public JobEvaluationContext withJob(Job job) {
        return new PlayEvaluationContext(super.withJob(job), play);
    }
}
