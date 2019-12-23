package com.yit.deploy.core.dsl.execute;

import com.yit.deploy.core.exceptions.ExitPlayException;
import com.yit.deploy.core.model.*;
import com.yit.deploy.core.variables.Variables;

import java.util.List;

public class PlayExecutionContext extends JobExecutionContext {

    protected final Play play;

    public PlayExecutionContext(JobExecutionContext jcx, Play play) {
        super(jcx);

        this.play = play;
        resolveVars(play.getVars());
    }

    @Override
    public PlayExecutionContext withJob(Job job) {
        return new PlayExecutionContext(super.withJob(job), play);
    }

    public HostExecutionContext toHost(Host host, List<Host> hostsInGroup, Variables hostWritable) {
        return new HostExecutionContext(this, host, hostsInGroup, hostWritable);
    }

    public Play getTargetPlay() {
        return play;
    }

    public Play getCurrentPlay() {
        return play;
    }

    public String getCurrentPlayName() {
        return play.getName();
    }

    public void exitPlay() {
        exitPlay("exit play");
    }

    public void exitPlay(String message) {
        throw new ExitPlayException(message);
    }

    @Deprecated
    public Play getCURRENT_PLAY() {
        return play;
    }

    @Deprecated
    public String getCURRENT_PLAY_NAME() {
        return play.getName();
    }

    public boolean isSingleHostMode() {
        return getVariableOrDefault("single_host_mode", false);
    }

    public Double getSerialOverride() {
        Object o = getVariableOrDefault("serial_override", null);
        if (o instanceof String) {
            String s = (String) o;
            if (!s.isEmpty()) {
                return Double.parseDouble(s);
            }
        } else if (o instanceof Number) {
            return ((Number)o).doubleValue();
        }
        return null;
    }
}
