package com.yit.deploy.core.config;

import com.google.common.base.Strings;
import com.yit.deploy.core.model.PipelineScript;
import com.yit.deploy.core.utils.GitUtils;
import hudson.FilePath;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

/**
 * the deploy.core needs a standalone project, which defines all playbooks
 * and all environment scripts and projects definition.
 * folder structure:
 * root:
 *
 *  envs/                   -- all environment initialization scripts can be defined here
 *  playbooks/              -- all playbook definition scripts can be defined here
 *  Projects.groovy         -- the script file for projects definition
 *
 * Created by nick on 28/12/2017.
 */
public class ConfigProject implements Serializable {
    /**
     * the repository url of the config project,
     * set to null if this project is not stored in a git repository.
     */
    public final String repositoryUrl;
    /**
     * the branch or commit hash or other valid git ref,
     * which specify the commit or branch we use to pull the source code.
     */
    public final String branch;
    /**
     * the download target path of the project source code.
     * if repositoryUrl is not specified, this fold must already contains all needed code.
     */
    public final FilePath rootPath;

    /**
     * the max milliseconds slept between two checkout trying,
     * set to a non-zero number to launch performance
     */
    public final long maxDelay;

    private int _hashCode = 0;

    /**
     * fetch source code and return current commit hash
     *
     * @return the commit hash if repositoryUrl is specified, or null otherwise.
     */
    public String fetch() {
        if (Strings.isNullOrEmpty(repositoryUrl) || Strings.isNullOrEmpty(branch)) {
            return null;
        }

        return GitUtils.checkout(repositoryUrl, branch, rootPath);
    }

    /**
     * source code is stored on local filesystem
     * @param rootPath
     */
    public ConfigProject(String rootPath) {
        this.repositoryUrl = null;
        this.branch = null;
        this.rootPath = new FilePath(new File(rootPath).getAbsoluteFile());
        this.maxDelay = 0;
    }

    /**
     * source code is stored on a git repository
     * @param repositoryUrl
     * @param branch
     * @param rootPath
     */
    public ConfigProject(String repositoryUrl, String branch, FilePath rootPath, long maxDelay) {
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.rootPath = rootPath;
        this.maxDelay = maxDelay;
    }

    @Override
    public int hashCode() {
        if (_hashCode == 0) {
            int h = 1;
            if (repositoryUrl != null) h ^= repositoryUrl.hashCode();
            if (branch != null) h ^= branch.hashCode();
            if (rootPath != null) h ^= rootPath.hashCode();
            _hashCode = h;
        }
        return _hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConfigProject) {
            ConfigProject op = (ConfigProject) obj;
            return Objects.equals(repositoryUrl, op.repositoryUrl) && Objects.equals(branch, op.branch) && Objects.equals(rootPath, op.rootPath);
        } else {
            return false;
        }
    }
}
