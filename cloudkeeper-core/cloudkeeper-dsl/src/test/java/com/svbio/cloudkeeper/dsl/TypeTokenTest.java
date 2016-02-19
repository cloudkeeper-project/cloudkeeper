package com.svbio.cloudkeeper.dsl;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Collection;

public class TypeTokenTest {
    @Test
    public void test() {
        TypeToken<?> typeToken = new TypeToken<Collection<String>>() { };
        Assert.assertTrue(typeToken.getJavaType() instanceof ParameterizedType);
        Assert.assertEquals(((ParameterizedType) typeToken.getJavaType()).getRawType(), Collection.class);
        Assert.assertEquals(((ParameterizedType) typeToken.getJavaType()).getActualTypeArguments().length, 1);
        Assert.assertEquals(((ParameterizedType) typeToken.getJavaType()).getActualTypeArguments()[0], String.class);
    }

    @Test
    public <D> void typeVariableTest() {
        TypeToken<?> typeToken = new TypeToken<D>() { };
        Assert.assertTrue(typeToken.getJavaType() instanceof TypeVariable);
        Assert.assertEquals(((TypeVariable) typeToken.getJavaType()).getName(), "D");
    }

    @Test
    public void illegalTest() {
        @SuppressWarnings("rawtypes")
        class Foo extends TypeToken { }

        try {
            new Foo();
            Assert.fail();
        } catch (IllegalStateException exception) {
            Assert.assertTrue(exception.getMessage().contains("without type parameter"));
        }

        class Bar<T> extends TypeToken<T> { }

        try {
            new Bar<Integer>() { };
            Assert.fail();
        } catch (IllegalStateException exception) {
            Assert.assertTrue(exception.getMessage().contains("direct subclasses"));
        }
    }
}
