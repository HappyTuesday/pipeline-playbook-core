package com.yit.deploy.core.test;

import com.yit.deploy.core.algorithm.QueryExpression;
import com.yit.deploy.core.compile.DeployCompiler;
import org.junit.Assert;
import org.junit.Test;

public class CompileTest {
    @Test
    public void tetQueryExpression() {
        QueryExpression q = QueryExpression.parse("a:b&c&(d:e)");
        Assert.assertEquals("a : b & c & (d : e)", q.toString());
    }
}
