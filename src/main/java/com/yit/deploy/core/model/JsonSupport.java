package com.yit.deploy.core.model;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

public interface JsonSupport<T extends JsonSupport<T>> extends Serializable {
    Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    default Gson getSerializer() {
        return GSON;
    }

    default String toJson() {
        return getSerializer().toJson(this);
    }

    default void toJson(Writer writer) throws IOException {
        Gson gson = getSerializer();
        gson.toJson(this, getClass(), gson.newJsonWriter(writer));
    }

    @SuppressWarnings("unchecked")
    default T fromJson(String json) {
        if (Strings.isNullOrEmpty(json)) {
            try {
                return (T) this.getClass().newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        } else {
            return (T) getSerializer().fromJson(json, this.getClass());
        }
    }

    default T dump() {
        return fromJson(toJson());
    }
}