package com.yit.deploy.core.info;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.yit.deploy.core.algorithm.QueryExpression;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.model.DeployModelTable;
import com.yit.deploy.core.model.JsonSupport;
import com.yit.deploy.core.variables.Variables;
import groovy.lang.GString;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class DeployTableResponse implements JsonSupport<DeployInfoTable> {
    private final String storageBranch;
    private final DeployInfoTable infoTable;
    private final Map<String, ProjectPlaybookInfo> projectPlaybooks;

    public DeployTableResponse(String storageBranch, DeployModelTable modelTable) {
        this.storageBranch = storageBranch;
        this.infoTable = modelTable.getInfoTable();

        projectPlaybooks = new HashMap<>(infoTable.getProjects().size());
        ProjectInfoAccessor accessor = ProjectInfoAccessor.from(infoTable.getProjects());
        for (Map.Entry<String, ProjectInfo> entry : infoTable.getProjects().entrySet()) {
            projectPlaybooks.put(
                entry.getKey(),
                entry.getValue().getProjectPlaybookInfo(accessor, modelTable.getEnvs())
            );
        }
    }

    public String getStorageBranch() {
        return storageBranch;
    }

    public DeployInfoTable getInfoTable() {
        return infoTable;
    }

    public Map<String, ProjectPlaybookInfo> getProjectPlaybooks() {
        return projectPlaybooks;
    }

    @Override
    public Gson getSerializer() {
        return GSON;
    }

    public static final JsonSerializer<Variables> VARIABLES_SERIALIZER = (src, type, context) -> {
        try {
            return context.serialize(src.toInfo());
        } catch (StackOverflowError e) {
            throw e;
        }
    };

    public static final JsonSerializer<ClosureWrapper> CLOSURE_WRAPPER_SERIALIZER =
        (src, type, context) -> context.serialize(src.toInfo());

    public static final JsonSerializer<GString> G_STRING_SERIALIZER =
        (src, type, context) -> context.serialize(src.toString());

    public static final JsonSerializer<QueryExpression> QUERY_EXPRESSION_JSON_SERIALIZER =
        ((src, typeOfSrc, context) -> context.serialize(src.toString()));

    public static final Gson GSON = new GsonBuilder()
        .registerTypeHierarchyAdapter(Variables.class, VARIABLES_SERIALIZER)
        .registerTypeHierarchyAdapter(ClosureWrapper.class, CLOSURE_WRAPPER_SERIALIZER)
        .registerTypeHierarchyAdapter(GString.class, G_STRING_SERIALIZER)
        .registerTypeHierarchyAdapter(QueryExpression.class, QUERY_EXPRESSION_JSON_SERIALIZER)
        .create();


    public static class GetInfoTableForm implements JsonSupport<GetInfoTableForm> {
        private String targetBranch;

        public String getTargetBranch() {
            return targetBranch;
        }

        public void setTargetBranch(String targetBranch) {
            this.targetBranch = targetBranch;
        }

        public static GetInfoTableForm fromJson(Reader reader) {
            return GSON.fromJson(reader, GetInfoTableForm.class);
        }
    }

    public static class LoadCommitsForm implements JsonSupport<LoadCommitsForm> {
        private long from;
        private int count;

        public long getFrom() {
            return from;
        }

        public void setFrom(long from) {
            this.from = from;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public static LoadCommitsForm fromJson(Reader reader) {
            return GSON.fromJson(reader, LoadCommitsForm.class);
        }
    }
}
