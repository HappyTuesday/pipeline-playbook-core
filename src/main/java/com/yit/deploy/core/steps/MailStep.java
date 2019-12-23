package com.yit.deploy.core.steps;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.function.Closures;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nick on 14/09/2017.
 */
public class MailStep extends AbstractStep {
    private Map<String, String> data = new HashMap<>();

    public MailStep(JobExecutionContext context) {
        super(context);
        data.put("from", context.getVariable("EMAIL_CI_USERNAME").toString());
        data.put("subject", "[jenkins] Jenkins Notification");
    }

    public MailStep setup(@DelegatesTo(value = DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    /**
     * Execute this step, providing a pipeline-script and a variable context
     */
    @Override
    protected Object executeOverride() {
        getScript().info("send mail %s to %s cc %s", data.get("subject"), data.get("to"), data.get("cc"));
        if (!getVariable("JENKINS_SEND_NOTIFICATION_EMAIL", Boolean.class)) {
            return null;
        }

        try {
            getScript().getSteps().mail(data);
        } catch (Exception e) {
            getScript().warn("sending mail %s to %s failed deal to error %s", data.get("subject"), data.get("to"), e.getMessage());
        }

        return null;
    }

    public static class DslContext {

        private MailStep step;

        public DslContext(MailStep step) {
            this.step = step;
        }

        public DslContext to(String value) {
            step.data.put("to", value == null || value.isEmpty() ? null : value);
            return this;
        }

        public DslContext toCurrentUser() {
            step.data.put("to", step.getScript().getCurrentUser().getEmailAddress());
            return this;
        }

        public DslContext cc(String value) {
            step.data.put("cc", value == null || value.isEmpty() ? null : value);
            return this;
        }

        public DslContext bcc(String value) {
            step.data.put("bcc", value == null || value.isEmpty() ? null : value);
            return this;
        }

        public DslContext subject(String value) {
            step.data.put("subject", value);
            return this;
        }

        public DslContext body(String value) {
            step.data.put("body", value);
            return this;
        }

        public DslContext htmlBody(String value) {
            html();
            step.data.put("body", value);
            return this;
        }

        public DslContext from(String value) {
            step.data.put("from", value);
            return this;
        }

        public DslContext replyTo(String value) {
            step.data.put("replyTo", value);
            return this;
        }

        public DslContext html() {
            return mimeType("text/html");
        }

        public DslContext mimeType(String value) {
            step.data.put("mimeType", value);
            return this;
        }

        public DslContext charset(String value) {
            step.data.put("charset", value);
            return this;
        }
    }
}
