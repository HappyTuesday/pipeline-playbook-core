package com.yit.deploy.core.utils;

import com.cloudbees.diff.Diff;
import com.cloudbees.diff.Difference;
import com.google.gson.Gson;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.parameters.inventory.DeployPlanService;
import groovy.time.TimeCategory;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Run;
import hudson.model.ParametersAction;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import jenkins.model.Jenkins;
import org.apache.commons.codec.Charsets;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.StringGroovyMethods;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class Utils {
    public static final Charset DefaultCharset = Charsets.UTF_8;

    public static String getRootPath() {
        return new File(".").getAbsolutePath();
    }

    public static String diff(String s1, String s2) {
        String[] lines1 = s1.split("\n");
        String[] lines2 = s2.split("\n");
        Diff diff = Diff.diff(Arrays.asList(lines1), Arrays.asList(lines2), false);
        List<String> res = Lambda.map(diff, d -> {
            if (d.getType() == Difference.DELETE) {
                return String.join("\n", Lambda.map(d.getFirstText().split("\n"), s -> "- " + s)) + "\n";
            } else if (d.getType() == Difference.ADD) {
                return String.join("\n", Lambda.map(d.getSecondText().split("\n"), s -> "+ " + s)) + "\n";
            } else if (d.getType() == Difference.CHANGE) {
                return String.join("\n", Lambda.map(d.getFirstText().split("\n"), s -> "- " + s)) + "\n"
                    + String.join("\n", Lambda.map(d.getSecondText().split("\n"), s -> "+ " + s)) + "\n";
            } else {
                throw new IllegalArgumentException("diff type");
            }
        });
        return String.join("", res);
    }

    public static String diff(byte[] bytes1, byte[] bytes2) {
        CharsetDecoder decoder = Charsets.UTF_8.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

        String[] textList = new String[2];
        byte[][] bytesList = new byte[][]{bytes1, bytes2};
        for (int i = 0; i < bytesList.length; i++) {
            try {
                textList[i] = decoder.decode(ByteBuffer.wrap(bytesList[i])).toString();
            } catch (CharacterCodingException ignore) {
                if (Arrays.equals(bytes1, bytes2)) {
                    return "binary files are identical";
                } else {
                    return "binary files are differ";
                }
            }
        }
        return diff(textList[0], textList[1]);
    }

    /**
     * return the seconds between two times
     * @param time1
     * @param time2
     * @return
     */
    public static double getTimeDiff(Date time1, Date time2) {
        return TimeCategory.minus(time1, time2).toMilliseconds() / 1000.0;
    }

    private static Job getJenkinsJob(String jobName) {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            throw new IllegalStateException("jenkins is null");
        }
        Job job = (Job) jenkins.getItem(jobName);
        if (job == null) {
            throw new IllegalArgumentException("invalid job " + jobName);
        }
        return job;
    }

    public static Object getJobLastRunParameter(String jobName, String parameterName) {
        Run run = getJenkinsJob(jobName).getLastSuccessfulBuild();
        if (run == null) return null;
        ParametersAction pa = run.getAction(ParametersAction.class);
        ParameterValue pv = pa.getParameter(parameterName);
        if (pv == null) {
            throw new IllegalArgumentException("invalid parameter name " + parameterName + " of job " + jobName);
        }
        return pv.getValue();
    }

    public static String getJenkinsJobBuildURL(String jobName) {
        return getJenkinsJob(jobName).getAbsoluteUrl() + "build?delay=0sec";
    }

    public static DeployPlanService getDeployPlanService(String jobName, String parameterName) {
        Job jenkinsJob = getJenkinsJob(jobName);
        ParametersDefinitionProperty definitionProperty = (ParametersDefinitionProperty) jenkinsJob.getProperty(ParametersDefinitionProperty.class);
        ParameterDefinition pd = definitionProperty.getParameterDefinition(parameterName);
        if (pd == null) throw new IllegalArgumentException("parameter with name " + parameterName + " does not exist in job " + jobName);
        return (DeployPlanService) pd;
    }

    public static List<ParameterValue> getJenkinsJobParameters(String jobName, Map<String, Object> parameters) {
        Job jenkinsJob = getJenkinsJob(jobName);
        ParametersDefinitionProperty definitionProperty = (ParametersDefinitionProperty) jenkinsJob.getProperty(ParametersDefinitionProperty.class);
        List<ParameterDefinition> parameterDefs = definitionProperty == null ? Collections.emptyList() : definitionProperty.getParameterDefinitions();

        List<ParameterValue> values = new ArrayList<>(parameterDefs.size());

        for (ParameterDefinition pd : parameterDefs) {
            Object originValue = parameters.get(pd.getName());
            if (originValue != null) {
                if (originValue instanceof List) {
                    originValue = DefaultGroovyMethods.asType(originValue, String[].class);
                } else if (originValue instanceof Boolean) {
                    originValue = originValue.toString();
                }
            }

            ParameterValue value;
            if (originValue != null) {
                value = (ParameterValue) DefaultGroovyMethods.invokeMethod(pd, "createValue", originValue);
            } else {
                value = pd.getDefaultParameterValue();
            }

            if (value != null) {
                values.add(value);
            }
        }

        return values;
    }

    public static String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
    public static boolean compareObject(Object a, Object b) {
        return a == null && b == null || a != null && a.equals(b);
    }

    public static Object getFieldValue(Field field, Object obj) {
        try {
            field.setAccessible(true);
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
