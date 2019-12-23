package com.yit.deploy.core.test;

import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.variables.SimpleVariables;
import com.yit.deploy.core.variables.resolvers.SimpleVariableResolver;
import com.yit.deploy.core.variables.variable.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class VariablesTest {
    @Test
    public void resolveSimpleVar() {
        SimpleVariableResolver resolver = new SimpleVariableResolver();
        resolver.resolveWritableVars(new SimpleVariables());

        resolver.setVariable("a", 1);
        Object value = resolver.getVariable("a");
        Assert.assertEquals(1, value);
    }

    @Test
    public void resolveLazyVar() {
        SimpleVariableResolver resolver = new SimpleVariableResolver();
        resolver.resolveWritableVars(new SimpleVariables());

        resolver.setVariable("a", Closures.closure(this, () -> 1));
        Object value = resolver.getVariable("a");
        Assert.assertEquals(1, value);
    }

    @Test
    public void resolveListVar() {
        SimpleVariableResolver resolver = new SimpleVariableResolver();
        resolver.resolveWritableVars(new SimpleVariables());

        resolver.setVariable("a", Arrays.asList("1", "2", "3"));

        Object value = resolver.getVariable("a");
        Assert.assertArrayEquals(new String[]{"1", "2", "3"}, ((List) value).toArray());

        resolver.setVariable("a.0", "4");
        Assert.assertEquals(3, ((List) resolver.getVariable("a")).size());
        Assert.assertEquals("4", ((List) resolver.getVariable("a")).get(0));

        resolver.setVariable("a.*", new AppendedListVariable<>(new SimpleVariable<>("4")));
        Assert.assertArrayEquals(new String[]{"4", "2", "3", "4"}, ((List) value).toArray());

        resolver.setVariable("a.*", new ExpandableListVariable<>(new SimpleListVariable<>(Arrays.asList("5", "6"))));
        Assert.assertArrayEquals(new String[]{"4", "2", "3", "4", "5", "6"}, ((List) value).toArray());
        Assert.assertEquals(6, ((List)resolver.getVariable("a")).size());
        Assert.assertEquals("6", ((List)resolver.getVariable("a")).get(5));

        resolver.resolveWritableVars(new SimpleVariables());

        resolver.setVariable("a", Arrays.asList("7", "8"));
        resolver.setVariable("a.*", new AppendedListVariable<>(new SimpleVariable<>("9")));
        Assert.assertArrayEquals(new String[]{"7", "8", "9"}, ((List)resolver.getVariable("a")).toArray());

        resolver.resolveWritableVars(new SimpleVariables());

        resolver.setVariable("a.*", new AppendedListVariable<>(new SimpleVariable<>("10")));
        Assert.assertArrayEquals(new String[]{"7", "8", "9", "10"}, ((List)resolver.getVariable("a")).toArray());

        List<Object> list = (List<Object>) resolver.getVariable("a");
        list.add("11");
        Assert.assertEquals(5, list.size());

        list.addAll(new LazyListVariable<>(new LazyVariable<>(new ClosureWrapper<>(Closures.closure(this, () -> Arrays.asList("12", "13"))))));
        Assert.assertEquals(7, list.size());

        list.set(4, Arrays.asList("a", "b"));
        Assert.assertEquals(7, list.size());
        Assert.assertEquals("b", ((List<Object>) list.get(4)).get(1));

        resolver.resolveWritableVars(new SimpleVariables());
        ((List<Object>) list.get(4)).add("c");
        Assert.assertEquals("c", ((List<Object>) list.get(4)).get(2));

        list.addAll(new FilterListVariable<>(new SimpleListVariable<>(Arrays.asList("14", "15")), new ClosureWrapper<>(Closures.closure(this, "15"::equals))));
        Assert.assertEquals("15", list.get(7));
    }

    @Test
    public void resolveMapVar() {
        SimpleVariableResolver resolver = new SimpleVariableResolver();
        resolver.resolveWritableVars(new SimpleVariables());

        resolver.setVariable("a", Lambda.asMap("a", 1, "b", Lambda.asMap("c", 2, "d", 3)));
        Assert.assertEquals(2, ((Map)resolver.getVariable("a")).size());

        resolver.setVariable("a.e", 4);
        Assert.assertEquals(4, ((Map)resolver.getVariable("a")).get("e"));

        resolver.resolveWritableVars(new SimpleVariables());

        Map<String, Object> map = (Map<String, Object>) resolver.getVariable("a");

        map.put("e", 5);
        Assert.assertEquals(5, map.get("e"));
        Assert.assertEquals(3, map.size());
        Assert.assertFalse(map.isEmpty());
        Assert.assertEquals(3, ((Map)map.get("b")).get("d"));

        map.putAll(new LazyMapVariable<>(new LazyVariable<>(new ClosureWrapper<>(Closures.closure(this, () -> Lambda.asMap("f", 6, "g", 7))))));
        Assert.assertEquals(5, map.size());
        Assert.assertEquals(6, map.get("f"));

        resolver.setVariable("a.b", Lambda.asMap("c", 3));
        Assert.assertEquals(3, ((Map)map.get("b")).get("c"));
        Assert.assertNull(((Map)map.get("b")).get("d"));

        resolver.setVariable("a.*", new ExpandableMapVariable<>((MapVariable<?>) map.get("b")));
        resolver.setVariable("a.b.i", 8);
        Assert.assertEquals(3, map.get("c"));
        Assert.assertEquals(8, map.get("i"));
        Assert.assertEquals(7, map.size());
        Assert.assertFalse(map.isEmpty());

        resolver.setVariable("b", new HashMap<>());
        map.putAll((Map<String, Object>) resolver.getVariable("b"));
        resolver.resolveWritableVars(new SimpleVariables());
        resolver.setVariable("b.1", 1);
        resolver.setVariable("b.2", 2);
        Assert.assertEquals(1, map.get("1"));
    }

    @Test
    public void resolveCachedVar() {
        SimpleVariableResolver resolver = new SimpleVariableResolver();
        resolver.resolveWritableVars(new SimpleVariables());

        resolver.setVariable("a", new CachedVariable<>(new LazyVariable<>(new ClosureWrapper<>(Closures.closure(null, () -> new Random().nextInt())))));
        Assert.assertEquals(resolver.getVariable("a"), resolver.getVariable("a"));
    }
}
