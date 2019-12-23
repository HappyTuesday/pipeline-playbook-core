package com.yit.deploy.core.exceptions;

import com.hubspot.jinjava.interpret.FatalTemplateErrorsException;
import com.hubspot.jinjava.interpret.InterpretException;
import com.hubspot.jinjava.interpret.TemplateError;

/**
 * Created by nick on 12/09/2017.
 */
public class RenderTemplateException extends RuntimeException {
    private final String templateName;

    public RenderTemplateException(String templateName, Throwable e) {
        super(prepareMessage(templateName, e), e);
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }

    private static String prepareMessage(String templateName, Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append("render template ").append(templateName).append(" failed: ");
        if (t instanceof FatalTemplateErrorsException) {
            FatalTemplateErrorsException ft = (FatalTemplateErrorsException) t;
            for (TemplateError error : ft.getErrors()) {
                sb.append('\n');
                Exception e = error.getException();
                if (e instanceof InterpretException) {
                    InterpretException ie = (InterpretException) e;
                    sb.append("LINE #").append(ie.getLineNumber()).append(" ").append(ie.getMessage());
                } else {
                    sb.append(error);
                }
            }
        } else {
            sb.append(t);
        }
        return sb.toString();
    }
}
