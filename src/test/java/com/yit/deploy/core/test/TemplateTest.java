package com.yit.deploy.core.test;

import com.yit.deploy.core.model.JinjaTemplate;
import com.yit.deploy.core.variables.SimpleVariables;
import com.yit.deploy.core.variables.resolvers.SimpleVariableResolver;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import org.junit.Assert;
import org.junit.Test;

public class TemplateTest {
    @Test
    public void testGenerateJ2() {
        VariableResolver resolver = new SimpleVariableResolver();
        resolver.resolveWritableVars(new SimpleVariables());
        resolver.setVariable("a", 1);
        JinjaTemplate template = new JinjaTemplate(resolver);
        Assert.assertEquals("1", template.render("{{a}}"));
    }
}
