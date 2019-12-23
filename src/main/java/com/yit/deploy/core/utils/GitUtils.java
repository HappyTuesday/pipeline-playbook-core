package com.yit.deploy.core.utils;

import com.yit.deploy.core.exceptions.ExitException;
import com.yit.deploy.core.exceptions.ProcessExecutionException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.GitCommitDetail;
import com.yit.deploy.core.model.PipelineScript;
import com.yit.deploy.core.model.ProcessExecutionStatus;
import com.yit.deploy.core.model.ProcessLauncher;
import com.yit.deploy.core.model.Project;
import hudson.FilePath;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

public class GitUtils {
    private static final String RELEASE_TAG_PREFIX = "release-";
    private static final String FEATURE_BRANCH_PREFIX = "sprint-";

    private final PipelineScript script;

    public GitUtils(PipelineScript script) {
        this.script = script;
    }

    private List<String> executeReturnLines(String ... cmd) {
        return Lambda.tokenize(executeReturnText(cmd), "\n");
    }

    private String executeReturnText(String ... cmd) {
        return new ProcessLauncher(Arrays.asList(cmd)).script(script).executeReturnText();
    }

    private int executeReturnCode(String ... cmd) {
        return new ProcessLauncher(Arrays.asList(cmd)).script(script).executeReturnStatus().getCode();
    }

    public String getPreviousBuildCommit() {
        com.yit.deploy.core.model.Job j = script.getJob();
        Job job = (Job) Jenkins.get().getItem(j.getJobName());
        if (job == null) {
            throw new IllegalArgumentException("could not find job " + j.getJobName());
        }
        Run lastBuild = job.getLastBuild();
        if (lastBuild == null) {
            script.warn("could not find any finished build of job " + j.getJobName() + ", default to master");
            return "master";
        }
        String hash = script.getSteps().getBuildCommitHash(job, lastBuild, j.getGitRepositoryUrl());
        if (hash != null) {
            return hash;
        }

        script.warn("could not find any SCM defined in " + j.getJobName() + " whoes remote url is " + j.getGitRepositoryUrl() + ", default to master");
        return "master";
    }

    public String getCurrentGitCommit() {
        return executeReturnText("git", "rev-parse", "HEAD");
    }

    public GitCommitDetail getLastCommit() {
        GitCommitDetail detail = new GitCommitDetail();
        detail.setHash(executeReturnText("git", "rev-parse", "HEAD"));
        detail.setEmailAddress(executeReturnText("git", "log", "-1", "--format=%ae", "HEAD"));
        detail.setDetail(executeReturnText("git", "show", "-q", "HEAD"));
        String date = executeReturnText("git", "log", "-1", "--format=%at", "HEAD");
        if (date != null && !date.isEmpty()) {
            detail.setDate(Long.parseLong(date));
        }
        return detail;
    }

    public List<String> getHeadRelatedGitTags() {
        return executeReturnLines("git", "tag", "-l", "--points-at=HEAD");
    }

    public String gitCheckout(String projectBranch) {
        return gitCheckout(projectBranch, false);
    }

    public String gitCheckout(String projectBranch, boolean useSCM) {
        if (projectBranch == null || projectBranch.isEmpty()) {
            projectBranch = getPreviousBuildCommit();
        }

        if (script.isRoot() && useSCM) {
            return script.checkoutSCM(projectBranch);
        } else {
            return script.checkout(script.getJob().getGitRepositoryUrl(), projectBranch, script.getWorkspaceFilePath());
        }
    }

    public List<String> getAllGitTags() {
        return executeReturnLines("git", "tag", "-l");
    }

    public List<String> sortReleaseTags(List<String> releaseTags) {
        List<String> sorted = new ArrayList<>(releaseTags);
        sorted.sort((a, b) -> getTimestampFromTag(b).compareTo(getTimestampFromTag(a)));
        return sorted;
    }

    private String getTimestampFromTag(String tag) {
        List<String> ts = Lambda.tokenize(tag, "-");
        if (ts.size() > 3) { // old format: release-2017-08-19-18-16
            return String.join(".", ts.subList(1, 6));
        } else { // new format: release-2017.08.26.16.20 or release-1.2.3-2017.08.26.16.20
            return ts.get(ts.size() - 1);
        }
    }

    public List<String> getAllReleaseTags() {
        return sortReleaseTags(Lambda.findAll(getAllGitTags(), this::isReleaseTag));
    }

    public List<String> getGitCommitTags(String commitHash) {
        return executeReturnLines("git", "tag", "-l", "--points-at" + commitHash);
    }

    public boolean isAncestor(String ancestor, String child) {
        ProcessExecutionStatus status = new ProcessLauncher("git", "merge-base", "--is-ancestor", ancestor, child)
            .script(script).executeReturnStatus();

        if (status.getCode() == 0) {
            return true;
        } else if (status.getCode() == 1) {
            return  false;
        } else {
            throw new ProcessExecutionException(status);
        }
    }

    /**
     * get the feature version from a feature branch
     * return null if the branch name is not a valid feature branch
     */
    public String getFeatureVersionFromFeatureBranch(String branchName) {
        if (branchName.startsWith(FEATURE_BRANCH_PREFIX)) {
            return Lambda.tokenize(branchName.substring(FEATURE_BRANCH_PREFIX.length()), "-").get(0);
        }
        return null;
    }

    public boolean isFeatureBranch(String branchName) {
        return branchName.startsWith(FEATURE_BRANCH_PREFIX);
    }

    public String getFeatureBranchName(String featureVersion) {
        return FEATURE_BRANCH_PREFIX + featureVersion;
    }

    public boolean branchExists(String branch) {
        return branchExists(branch, true);
    }

    public boolean branchExists(String branch, boolean includeLocalBranches) {
        return branchExists(branch, includeLocalBranches, true);
    }

    public boolean branchExists(String branch, boolean includeLocalBranches, boolean includeRemoteBranches) {
        if (branch == null || branch.isEmpty()) return false;

        List<String> branches = Lambda.map(executeReturnLines("git", "branch", "-al", "--no-color"), s ->
            s.replaceFirst("/^\\*/", "").trim());

        if (includeLocalBranches && branches.contains(branch)) {
            return true;
        }
        if (includeRemoteBranches && branches.contains("remotes/origin/" +  branch)) {
            return true;
        }
        if (includeRemoteBranches && !executeReturnText("git", "ls-remote", "--heads", "origin", branch).isEmpty()) {
            return true;
        }
        return false;
    }

    public List<String> listRemoteHeads(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> heads = executeReturnLines("git", "ls-remote", "--heads", "--tags", repositoryUrl);
        return Lambda.map(heads, s -> {
            String h = Lambda.last(Lambda.tokenize(s, "\t"));
            if (h.startsWith("refs/heads/")) {
                return h.substring("refs/heads/".length());
            } else if (h.startsWith("refs/tags/")) {
                return h.substring("refs/tags/".length());
            } else {
                throw new IllegalStateException("invalid git output " + s);
            }
        });
    }

    /**
     * get the feature version from a release tag
     */
    public String getFeatureVersionFromReleaseTag(String releaseTag) {
        if (releaseTag.startsWith(RELEASE_TAG_PREFIX)) {
            List<String> ls = Lambda.tokenize(releaseTag.substring(RELEASE_TAG_PREFIX.length()), "-");
            if (ls.size() == 2) {
                return ls.get(0);
            }
        }
        return null;
    }

    public boolean isReleaseTag(String tag) {
        return tag.startsWith(RELEASE_TAG_PREFIX);
    }

    public boolean isFeatureReleaseTag(String tag){
        return tag.matches("/^release(-\\d+(\\.\\d+)+){1,2}$/");
    }

    public String getReleaseTag(String featureVersion) {
        String tag = RELEASE_TAG_PREFIX;
        if (featureVersion != null) {
            tag += featureVersion + "-";
        }
        tag += new SimpleDateFormat("yyyy.MM.dd.HH.mm").format(new Date());
        return tag;
    }

    public void insideBranch(String branch, Consumer<String> consumer) {
        String head = getCurrentGitCommit();

        sh("git fetch origin '" + branch + "'");
        if (branchExists(branch, true, false)) {
            sh("git checkout -fq '" + branch + "'");
            sh("git pull origin '" + branch + "'");
        } else {
            sh("git checkout -fqb '" + branch + "' --track 'origin/" + branch + "'");
        }

        consumer.accept(head);

        sh("git checkout -fq '" + head + "'");
    }

    /**
     * merge all commits in current HEAD into a specified branch and checkout back
     */
    public void mergeTo(String branch) {
        insideBranch(branch, head -> {
            sh("git pull -fq origin '" + branch + "'");
            sh("git merge " + head + " --ff -m '[jenkins] auto-merge from " + head.substring(0, 8) + "'");
            sh("git status");
            sh("git push origin '" + branch + "'");
        });
    }

    /**
     * merge all commits from sources into target branch
     */
    public void mergeFrom(String targetBranch, List<String> sources) {
        insideBranch(targetBranch, head -> {
            sh("git pull -fq origin '" + targetBranch + "'");
            for (String source : sources) {
                sh("git fetch origin '" + source + "'");
                sh("git merge 'origin/" + source + "' --ff -m '[jenkins] auto-merge from " + source + "'");
            }
            sh("git status");
            sh("git push origin '" + targetBranch + "'");
        });
    }

    public void makeTag(String tagName) {
        sh("git tag -f '" + tagName + "'");
        sh("git push origin '" + tagName + "'");
    }

    public void discardLocalChanges(String branch) {
        if (branchExists(branch, true, false)) {
            sh("git branch -D '" + branch + "'");
        }
        sh("git reset --merge");
        sh("git checkout .");
    }

    public int compareFeatureVersion(String version1, String version2) {
        List<String> vs1 = Lambda.tokenize(version1, "."), vs2 = Lambda.tokenize(version2, ".");
        for (int i = 0; true; i++) {
            if (i < vs1.size() && i < vs2.size()) {
                if (!Objects.equals(vs1.get(i), vs2.get(i))) {
                    try {
                        return Integer.compare(Integer.parseInt(vs1.get(i)), Integer.parseInt(vs2.get(i)));
                    } catch (NumberFormatException ignore) {
                        return vs1.get(i).compareTo(vs2.get(i));
                    }
                } else {
                    // compare next segment of version
                }
            } else if (i < vs1.size()) {
                return 1;
            } else if (i < vs2.size()) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    public List<String> listChangedFiles() {
        return executeReturnLines("git", "ls-files", "-m");
    }

    public void commitFile(String branch, String commitMessage, String ... fileNames) {
        sh("git stash save");
        insideBranch(branch, head -> {
            if (!executeReturnText("git", "stash", "list").isEmpty()) {
                sh("git stash pop");
            }
            String names = Lambda.join(" ", Lambda.map(fileNames, s -> "'" + s + "'"));
            sh("git reset");
            sh("git add " + names);
            if (executeReturnCode("git", "diff-index", "--cached", "--quiet", "HEAD") == 0) {
                script.debug("no file changed");
                return;
            }
            sh("git status");
            sh("git diff -- " + names);
            sh("git commit -m '" + commitMessage + "'");
            sh("git push origin '" + branch + "'");
        });
    }

    private void sh(String command) {
        new ProcessLauncher().script(script).bash(command).executePrintOutput();
    }

    public static String checkout(String gitRepositoryUrl, String branch, FilePath path) {
        try {
            ProcessLauncher launcher = new ProcessLauncher().pwd(path);
            if (path.exists()) {
                if (!path.isDirectory()) {
                    throw new IllegalStateException("path " + path + " is not a directory");
                }
            } else {
                path.mkdirs();
            }
            boolean needClone = false;
            if (path.child(".git").exists()) {
                boolean needRemove = false;
                try {
                    if (!launcher.cmd("git", "config", "--get", "remote.origin.url").executeReturnText().equals(gitRepositoryUrl)) {
                        needRemove = true;
                    }
                } catch (Exception ignore) {
                    needRemove = true;
                }
                if (needRemove) {
                    path.deleteContents();
                    needClone = true;
                }
            } else {
                needClone = true;
                path.deleteContents();
            }
            if (needClone) {
                launcher.cmd("git", "clone", gitRepositoryUrl, path.getRemote()).executePrintOutput();
            }
            launcher.cmd("git", "fetch", "--tags", gitRepositoryUrl, "+refs/heads/*:refs/remotes/origin/*").executePrintOutput();
            String commitHash;
            ProcessExecutionStatus status = launcher.cmd("git", "rev-parse", "origin/" + branch).executeReturnStatus();

            if (status.getCode() == 0) {
                commitHash = status.getText();
            } else if (status.getCode() == 128) {
                try {
                    commitHash = launcher.cmd("git", "rev-parse", branch).executeReturnText();
                } catch (ProcessExecutionException e) {
                    if (e.getCode() == 128) {
                        throw new ExitException("invalid GIT branch/commit/tag " + branch + ", please check if it exists");
                    } else {
                        throw e;
                    }
                }
            } else {
                throw new ProcessExecutionException(status);
            }

            commitHash = commitHash.substring(0, 12);
            launcher.cmd("git", "checkout", "-qf", commitHash).executePrintOutput();

            return commitHash;
        } catch (InterruptedException e) {
            throw new ExitException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
