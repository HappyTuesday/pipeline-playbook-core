package com.yit.deploy.core.model;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.Context;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.exptest.ExpTest;
import com.hubspot.jinjava.lib.filter.Filter;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.exceptions.RenderTemplateException;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.function.Lambda;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.StringGroovyMethods;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Created by nick on 28/09/2017.
 */
public class JinjaTemplate implements Serializable {
    private static final Field GLOBAL_CONTEXT_FIELD;

    static {
        try {
            GLOBAL_CONTEXT_FIELD = Jinjava.class.getDeclaredField("globalContext");
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
        GLOBAL_CONTEXT_FIELD.setAccessible(true);
    }

    private Jinjava engine;

    public JinjaTemplate(VariableResolver resolver) {
        JinjavaConfig config = JinjavaConfig.newBuilder().withFailOnUnknownTokens(true).build();
        engine = new Jinjava(config);
        try {
            GLOBAL_CONTEXT_FIELD.set(engine, new PipelineTemplateContext(resolver));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        for (Filter filter : PredefinedFilters) {
            engine.getGlobalContext().registerFilter(filter);
        }
    }

    public void registerFilter(final String name, final BiFunction<Object, String[], Object> f) {
        engine.getGlobalContext().registerFilter(new Filter() {
            @Override
            public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
                return f.apply(var, args);
            }

            @Override
            public String getName() {
                return name;
            }
        });
    }

    public void registerFunction(String name, Class<?> methodClass, String methodName, Class<?>... parameterTypes) {
        engine.getGlobalContext().registerFunction(new ELFunctionDefinition("", name, methodClass, methodName, parameterTypes));
    }

    public String render(String template) {
        return render(template, null, Collections.emptyMap());
    }

    public String render(String template, String templateName) {
        return render(template, templateName, Collections.emptyMap());
    }

    public String render(String template, String templateName, Map<String, Object> bindings) {
        try {
            return engine.render(template, bindings);
        } catch (Exception e) {
            throw new RenderTemplateException(templateName, e);
        }
    }

    private static class PipelineTemplateContext extends Context {
        VariableResolver resolver;

        PipelineTemplateContext(VariableResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        public Object get(Object key) {
            Object value = super.get(key);
            if (value == null) {
                value = DefaultGroovyMethods.getMetaClass(resolver).getProperty(resolver, (String) key);
            }
            return value;
        }

        @Override
        public Filter getFilter(String name) {
            Filter filter = super.getFilter(name);
            if (filter == null) {
                throw new IllegalArgumentException("invalid jinja filter " + name);
            }
            return filter;
        }

        @Override
        public ExpTest getExpTest(String name) {
            ExpTest test = super.getExpTest(name);
            if (test == null) {
                throw new IllegalArgumentException("invalid jinja test " + name);
            }
            return test;
        }

        @Override
        public ELFunctionDefinition getFunction(String name) {
            ELFunctionDefinition f = super.getFunction(name);
            if (f == null) {
                throw new IllegalArgumentException("invalid jinja function " + name);
            }
            return f;
        }
    }

    private static final List<Filter> PredefinedFilters = Arrays.asList(
            new Filter() {
                @Override
                public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
                    assert args.length == 2;
                    return Lambda.toBoolean(var) ? args[0] : args[1];
                }

                @Override
                public String getName() {
                    return "ternary";
                }
            },
            new Filter() {
                String ks = "()[]{}?*+-|^$\\.# \t\n\r\f";

                @Override
                public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
                    return StringGroovyMethods.collectReplacements((String) var, Closures.closure(this, (Character c) -> ks.indexOf(c) >= 0 ? "\\" + c : c.toString()));
                }

                @Override
                public String getName() {
                    return "regex_escape";
                }
            },
            new Filter() {
                /**
                 * Filter the specified template variable within the context of a render process. {{ myvar|myfiltername(arg1,arg2) }}
                 *
                 * @param var         the variable which this filter should operate on
                 * @param interpreter current interpreter context
                 * @param args        any arguments passed to this filter invocation
                 * @return the filtered form of the given variable
                 */
                @Override
                public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
                    return var != null;
                }

                @Override
                public String getName() {
                    return "defined";
                }
            },
            new Filter() {
                /**
                 * Filter the specified template variable within the context of a render process. {{ myvar|myfiltername(arg1,arg2) }}
                 *
                 * @param var         the variable which this filter should operate on
                 * @param interpreter current interpreter context
                 * @param args        any arguments passed to this filter invocation
                 * @return the filtered form of the given variable
                 */
                @Override
                public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
                    return var != null && !"".equals(var) && (!(var instanceof Collection) || !((Collection) var).isEmpty());
                }

                @Override
                public String getName() {
                    return "not_empty";
                }
            },
            new Filter() {
                /**
                 * Filter the specified template variable within the context of a render process. {{ myvar|myfiltername(arg1,arg2) }}
                 *
                 * @param var         the variable which this filter should operate on
                 * @param interpreter current interpreter context
                 * @param args        any arguments passed to this filter invocation
                 * @return the filtered form of the given variable
                 */
                @Override
                public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
                    return var == null || "".equals(var) || var instanceof Collection && ((Collection) var).isEmpty();
                }

                @Override
                public String getName() {
                    return "is_empty";
                }
            }
            );

}
