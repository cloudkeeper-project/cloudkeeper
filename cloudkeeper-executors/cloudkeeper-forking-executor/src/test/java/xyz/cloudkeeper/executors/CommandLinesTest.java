package xyz.cloudkeeper.executors;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

public class CommandLinesTest {
    @Test
    public void escapeTest() {
        Assert.assertEquals(CommandLines.escape(Collections.singletonList("foo")), "foo");
        Assert.assertEquals(CommandLines.escape(Collections.singletonList("foo bar")), "foo\\ bar");
        Assert.assertEquals(CommandLines.escape(Arrays.asList("foo", "bar")), "foo bar");
        Assert.assertEquals(CommandLines.escape(Arrays.asList("foo", "bar baz")), "foo bar\\ baz");
    }
}
