package com.yit.deploy.core.model;

public class DeployResponse<T> implements JsonSupport<DeployResponse<T>> {
    private T data;
    private StatusCode status;

    public DeployResponse() {}

    public DeployResponse(T data, StatusCode status) {
        this.data = data;
        this.status = status;
    }

    public DeployResponse(T data) {
        this(data, StatusCode.OK);
    }

    public DeployResponse(StatusCode status) {
        this(null, status);
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public StatusCode getStatus() {
        return status;
    }

    public void setStatus(StatusCode status) {
        this.status = status;
    }
}
