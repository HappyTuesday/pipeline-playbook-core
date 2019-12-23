package com.yit.deploy.core.support;

import com.yit.deploy.core.model.PipelineScript;
import com.yit.deploy.core.utils.IO;
import com.yit.deploy.core.utils.Utils;
import hudson.FilePath;

import javax.annotation.Nonnull;
import java.io.IOException;

public interface WorkspaceSupport extends JinjaSupport {

    PipelineScript getScript();

    default FilePath getFilePath(String nodeName, String path) {
        return PipelineScript.getFilePath(nodeName, path);
    }

    default FilePath parseFilePathString(String path) {
        return PipelineScript.parseFilePathString(path);
    }

    default String toFilePathString(FilePath path) {
        return PipelineScript.toFilePathString(path);
    }

    default FilePath getFilePath(String path) {
        return getScript().getFilePath(path);
    }

    default String getWorkspace() {
        return getScript().getWorkspace();
    }

    default String getWorkspacetmp() {
        return getScript().getWorkspacetmp();
    }

    default FilePath getWorkspaceFilePath() {
        return getFilePath(getWorkspace());
    }

    default FilePath getWorkspacetmpFilePath() {
        return getFilePath(getWorkspacetmp());
    }

    default String getProjectsRoot() {
        return getScript().getProjectsRoot();
    }

    default FilePath getProjectsRootFilePath() {
        return getScript().getProjectsRootFilePath();
    }

    default boolean isAutoTriggered() {
        return getScript().isAutoTriggered();
    }

    default void writeToWorkspace(@Nonnull String path, @Nonnull String text) throws IOException, InterruptedException {
        writeToWorkspace(path, text.getBytes(Utils.DefaultCharset));
    }

    default void writeToWorkspace(@Nonnull String path, @Nonnull byte[] bytes) throws IOException, InterruptedException {
        getScript().getWorkspaceFilePath().child(path).write().write(bytes);
    }

    default byte[] readFromWorkspace(@Nonnull String path) {
        return IO.readBytes(getScript().getWorkspaceFilePath().child(path));
    }

    default String readTextFromWorkspace(@Nonnull String path) {
        return IO.readToString(getScript().getWorkspaceFilePath().child(path));
    }

    default String loadTemplateFromWorkspace(@Nonnull String templatePath) {
        return createJinjaTemplate().render(readTextFromWorkspace(templatePath), templatePath);
    }

    default String loadRestrictedTemplateFromWorkspace(@Nonnull String templatePath) {
        return createRestrictedJinjaTemplate().render(readTextFromWorkspace(templatePath), templatePath);
    }
}
