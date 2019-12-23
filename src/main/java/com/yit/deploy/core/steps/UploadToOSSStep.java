package com.yit.deploy.core.steps;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.ObjectMetadata;
import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.support.ExceptionSupport;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import hudson.FilePath;

import java.io.InputStream;

/**
 * Created by nick on 14/09/2017.
 */
public class UploadToOSSStep extends AbstractStep {
    private String bucketName;
    private FilePath localFolder;
    private String ossFolder;

    private transient OSS _oss;

    public UploadToOSSStep(JobExecutionContext context) {
        super(context);
    }

    public UploadToOSSStep setup(@DelegatesTo(value = DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    /**
     * Execute this step, providing a pipeline-script and a variable context
     */
    @Override
    protected Object executeOverride() throws Exception {
        assert bucketName != null && localFolder != null && localFolder.isDirectory() && ossFolder != null;

        if (!getOss().doesBucketExist(bucketName)) {
            throw new IllegalArgumentException("OSS bucket " + bucketName + " does not exist");
        }

        String prefix = localFolder.getRemote();
        if (!prefix.endsWith("/")) prefix += "/";
        for (FilePath file : localFolder.list("**/*")) {
            assert file.getRemote().startsWith(prefix);
            String filename = file.getRemote().substring(prefix.length());
            String ossKey = ossFolder + "/" + filename;

            getScript().debug("upload %s to OSS %s[%s]", filename, bucketName, ossKey);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setCacheControl("public, max-age=31536000");
            try (InputStream stream = file.read()) {
                getOss().putObject(bucketName, ossKey, stream, metadata);
            }
        }
        return null;
    }

    private OSS getOss() {
        if (_oss == null) {
            String accessKey = getVariable("OSS_ACCESS_KEY_ID", String.class);
            String keySecret = getVariable("OSS_ACCESS_KEY_SECRET", String.class);
            String endPoint = getVariable("OSS_ENDPOINT", String.class);
            _oss = new OSSClient(endPoint, accessKey, keySecret);
        }
        return _oss;
    }

    public static class DslContext implements ExceptionSupport {

        private UploadToOSSStep step;

        public DslContext(UploadToOSSStep step) {
            this.step = step;
        }

        public DslContext bucketName(String value) {
            step.bucketName = value;
            return this;
        }

        public DslContext localFolder(FilePath value) {
            step.localFolder = unchecked(value, FilePath::absolutize);
            return this;
        }

        public DslContext ossFolder(String value) {
            step.ossFolder = value;
            return this;
        }
    }
}
