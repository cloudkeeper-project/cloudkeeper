package com.svbio.cloudkeeper.model.api;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifies the default methods in {@link Marshaler}.
 */
public class MarshalerTest {
    private abstract static class AbstractMarshaler<T> implements Marshaler<T> {
        @Override
        public boolean isImmutable(T object) {
            return false;
        }

        @Override
        public final void put(T object, MarshalContext context) throws IOException {
            throw new AssertionError("Should never be called");
        }

        @Override
        public final T get(UnmarshalContext context) throws IOException {
            throw new AssertionError("Should never be called");
        }
    }

    private static class NumberMarshaler extends AbstractMarshaler<Number> { }

    private static class StringListMarshaler extends AbstractMarshaler<List<String>> { }

    @Test
    public void isCapableOfMarshaling() {
        // Integer <: Number
        Assert.assertTrue(new NumberMarshaler().canHandle(4));

        // It does *not* hold that ArrayList <: List<String>
        Assert.assertFalse(new StringListMarshaler().canHandle(new ArrayList<>()));

        // Number and String are not in a subtype relationship
        Assert.assertFalse(new NumberMarshaler().canHandle("foo"));

        // Reject null arguments
        try {
            new NumberMarshaler().canHandle(null);
            Assert.fail("Expected exception");
        } catch (NullPointerException ignore) { }
    }
}
