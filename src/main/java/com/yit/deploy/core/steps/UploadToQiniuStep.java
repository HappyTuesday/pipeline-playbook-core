package com.yit.deploy.core.steps;

import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.support.ExceptionSupport;
import com.yit.deploy.core.utils.IO;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import hudson.FilePath;

/**
 * Created by nick on 14/09/2017.
 */
public class UploadToQiniuStep extends AbstractStep {

    private String accessKey;
    private String keySecret;

    private String bucketName;
    private FilePath localFile;
    private String cloudFile;

    public UploadToQiniuStep(JobExecutionContext context) {
        super(context);
    }

    public UploadToQiniuStep setup(@DelegatesTo(value = DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    /**
     * Execute this step, providing a pipeline-script and a variable context
     */
    @Override
    protected Object executeOverride() throws Exception {
        Auth auth = Auth.create(accessKey, keySecret);
        UploadManager uploadManager = new UploadManager();
        uploadManager.put(IO.readBytes(localFile), cloudFile, auth.uploadToken(bucketName));
        return null;
    }

    public static class DslContext implements ExceptionSupport {

        private UploadToQiniuStep step;

        public DslContext(UploadToQiniuStep step) {
            this.step = step;
        }

        public DslContext accessKey(String value) {
            step.accessKey = value;
            return this;
        }

        public DslContext keySecret(String value) {
            step.keySecret = value;
            return this;
        }

        public DslContext bucketName(String value) {
            step.bucketName = value;
            return this;
        }

        public DslContext localFile(FilePath value) {
            step.localFile = value;
            return this;
        }

        public DslContext cloudFile(String value) {
            step.cloudFile = value;
            return this;
        }
    }
}
