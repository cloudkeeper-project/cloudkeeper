package com.svbio.cloudkeeper.model.api;

import org.testng.Assert;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExecutionExceptionTest {
    private static void throwException() {
        throw new NumberFormatException("AA is not an integer.");
    }

    @Test
    public void test() {
        @Nullable ExecutionException originalException = null;
        @Nullable ImmunizedExecutionException immunizedException = null;
        try {
            throwException();
            Assert.fail("Expected exception.");
        } catch (NumberFormatException exception) {
            originalException
                = new ExecutionException("This is expected.", new IOException("Failed to read from disk.", exception));
            originalException.addSuppressed(new IllegalStateException("Whatever!"));
            immunizedException = (ImmunizedExecutionException) originalException.toImmunizedException();
        }

        StringWriter expectedStringWriter = new StringWriter();
        originalException.printStackTrace(new PrintWriter(expectedStringWriter));
        StringWriter actualStringWriter = new StringWriter();
        immunizedException.printStackTrace(new PrintWriter(actualStringWriter));

        Assert.assertEquals(actualStringWriter.toString(), expectedStringWriter.toString());
    }
}
