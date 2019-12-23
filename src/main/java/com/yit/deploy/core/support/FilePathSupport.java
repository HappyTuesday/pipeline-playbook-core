package com.yit.deploy.core.support;

import com.yit.deploy.core.model.PipelineScript;
import hudson.FilePath;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

public interface FilePathSupport {

    PipelineScript getScript();

    /**
     * the internal Jenkins copyRecursiveTo implementation has a limitation that
     * it could not properly handle remote -> remote folder copy
     * @param source
     * @param target
     */
    default int copyRecursiveTo(@Nonnull FilePath source, @Nonnull FilePath target) {
        try {
            if (source.isRemote() && target.isRemote()) {
                FilePath tempRoot = new FilePath(new File(getScript().getWorkspacetmp()));
                if (!tempRoot.exists()) tempRoot.mkdirs();
                FilePath temp = tempRoot.createTempDir("copy", "dir");
                try {
                    source.copyRecursiveTo(temp);
                    return temp.copyRecursiveTo(target);
                } finally {
                    try {
                        temp.deleteRecursive();
                    } catch (IOException | InterruptedException e) {
                        // nothing to do
                    }
                }
            } else {
                return source.copyRecursiveTo(target);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
