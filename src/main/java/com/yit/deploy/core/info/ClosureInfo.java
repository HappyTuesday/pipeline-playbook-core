package com.yit.deploy.core.info;

import com.yit.deploy.core.compile.DeployCompiler;
import com.yit.deploy.core.function.ClosureWrapper;

import java.io.Reader;

public class ClosureInfo {
    private String groovy;

    public ClosureInfo(String text) {
        this.groovy = text;
    }

    public String getGroovy() {
        return groovy;
    }

    public void setGroovy(String groovy) {
        this.groovy = groovy;
    }

    public <T> ClosureWrapper<T> toClosure() {
        if (groovy == null) {
            return null;
        }
        return DeployCompiler.getInstance().parseClosure(groovy);
    }

    public String toJson() {
        return DeployTableResponse.GSON.toJson(this);
    }

    public static ClosureInfo fromJson(Reader reader) {
        if (reader == null) {
            return null;
        }
        return DeployTableResponse.GSON.fromJson(reader, ClosureInfo.class);
    }

    public static ClosureInfo fromJson(String json) {
        if (json == null) {
            return null;
        }
        return DeployTableResponse.GSON.fromJson(json, ClosureInfo.class);
    }
}
