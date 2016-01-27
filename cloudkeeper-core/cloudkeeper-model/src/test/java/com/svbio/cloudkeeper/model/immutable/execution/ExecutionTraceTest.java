package com.svbio.cloudkeeper.model.immutable.execution;

import com.svbio.cloudkeeper.model.immutable.ParseException;
import com.svbio.cloudkeeper.model.immutable.element.Index;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ExecutionTraceTest {
    static void assertEquals(String string, ExecutionTrace trace) {
        ExecutionTrace fromString = ExecutionTrace.valueOf(string);
        Assert.assertEquals(fromString, trace);
        Assert.assertEquals(trace, fromString);
        Assert.assertEquals(string, trace.toString());

        Assert.assertEquals(fromString.compareTo(trace), 0);
        Assert.assertEquals(trace.compareTo(fromString), 0);
    }

    static void assertFailure(String string) {
        try {
            ExecutionTrace.valueOf(string);
            Assert.fail(String.format("%s should have been detected as invalid string representation.", string));
        } catch (ParseException exception) {
            // Expected
        }
    }

    @Test
    public void parse() {
        assertEquals("", ExecutionTrace.empty());
        assertFailure(":in:");
        assertEquals(":in:foo", ExecutionTrace.empty().resolveInPort(SimpleName.identifier("foo")));
        assertEquals(":out:bar", ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("bar")));
        assertFailure(":in:/foo");
        assertFailure(":in:/foo ");
        assertFailure(":in:/f:oo");
        assertFailure(":in:class");
        assertFailure(":in:foo/");
        assertEquals(
            ":out:foo:1",
            ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("foo")).resolveArrayIndex(Index.index(1))
        );
        assertEquals("/", ExecutionTrace.empty().resolveContent());
        assertEquals("12", ExecutionTrace.empty().resolveIteration(Index.index(12)));
        assertEquals("12/", ExecutionTrace.empty().resolveIteration(Index.index(12)).resolveContent());
        assertEquals("/12", ExecutionTrace.empty().resolveContent().resolveIteration(Index.index(12)));
        assertEquals(
            "/12/",
            ExecutionTrace.empty().resolveContent().resolveIteration(Index.index(12)).resolveContent()
        );
        assertEquals("ab", ExecutionTrace.empty().resolveModule(SimpleName.identifier("ab")));
        assertEquals("ab/", ExecutionTrace.empty().resolveModule(SimpleName.identifier("ab")).resolveContent());
        assertEquals("/ab", ExecutionTrace.empty().resolveContent().resolveModule(SimpleName.identifier("ab")));
        assertEquals(
            "/ab/",
            ExecutionTrace.empty().resolveContent().resolveModule(SimpleName.identifier("ab")).resolveContent()
        );
        assertEquals(
            "/foo/bar/12/",
            ExecutionTrace.empty()
                .resolveContent().resolveModule(SimpleName.identifier("foo"))
                .resolveContent().resolveModule(SimpleName.identifier("bar"))
                .resolveContent().resolveIteration(Index.index(12))
                .resolveContent()
        );
        assertEquals(
            "/foo/bar/12:in:baz:3:12:0",
            ExecutionTrace.empty()
                .resolveContent().resolveModule(SimpleName.identifier("foo"))
                .resolveContent().resolveModule(SimpleName.identifier("bar"))
                .resolveContent().resolveIteration(Index.index(12))
                .resolveInPort(SimpleName.identifier("baz"))
                .resolveArrayIndex(Index.index(3))
                .resolveArrayIndex(Index.index(12))
                .resolveArrayIndex(Index.index(0))
        );
        assertFailure("/foo/bar/:in:port");
        assertFailure("/foo/bar/:in:");
    }

    @Test
    public void validate() {
        try {
            ExecutionTrace.empty().resolveContent().resolveContent();
            Assert.fail();
        } catch (IllegalStateException exception) {
            // Expected
        }

        try {
            ExecutionTrace.empty().resolveContent().resolveArrayIndex(Index.index(3));
            Assert.fail();
        } catch (IllegalStateException exception) {
            // Expected
        }

        ExecutionTrace trace = ExecutionTrace.empty()
                .resolveContent().resolveModule(SimpleName.identifier("foo"))
                .resolveContent().resolveModule(SimpleName.identifier("bar"))
                .resolveContent().resolveIteration(Index.index(12)).resolveContent();
        try {
            trace.resolveInPort(SimpleName.identifier("port"));
            Assert.fail();
        } catch (IllegalStateException exception) {
            // Expected
        }

        trace = trace.resolveModule(SimpleName.identifier("module")).resolveInPort(SimpleName.identifier("port"));
        Assert.assertEquals(
            trace.toString(),
            "/foo/bar/12/module:in:port"
        );

        try {
            trace.resolveInPort(SimpleName.identifier("nested"));
            Assert.fail();
        } catch (IllegalStateException exception) {
            // Expected
        }

        try {
            trace.resolveContent();
            Assert.fail();
        } catch (IllegalStateException exception) {
            // Expected
        }
    }

    static void assertLess(String first, String second) {
        Assert.assertTrue(ExecutionTrace.valueOf(first).compareTo(ExecutionTrace.valueOf(second)) < 0);
        Assert.assertTrue(ExecutionTrace.valueOf(second).compareTo(ExecutionTrace.valueOf(first)) > 0);
    }

    static void assertOrder(String... traces) {
        for (int i = 0; i < traces.length; ++i) {
            ExecutionTrace first = ExecutionTrace.valueOf(traces[i]);
            Assert.assertEquals(first.compareTo(first), 0);
            for (int j = i + 1; j < traces.length; ++j) {
                ExecutionTrace second = ExecutionTrace.valueOf(traces[j]);
                Assert.assertTrue(first.compareTo(second) < 0);
                Assert.assertTrue(second.compareTo(first) > 0);
            }
        }
    }

    @Test
    public void compare() {
        assertOrder(
            "",
            ":1",
            ":10",
            ":in:foo",
            ":out:bar",
            "/",
            "/a/bcd",
            "/ab",
            "/ab:in:foo",
            "/ab:out:bar",
            "/ab/",
            "/ab/c",
            "/ab/1",
            "/ab/1:in:baz",
            "/ab/1:in:foo",
            "/ab/1:out:bar",
            "/ab/2:in:bar",
            "/ab/2:out:foo",
            "/ab/2:out:foo:4",
            "/ab/2:out:foo:4:5",
            "/ab/2:out:foo:4:10",
            "/ab/10:in:foo"
        );
    }

    @Test
    public void callStackTest() {
        Assert.assertEquals(ExecutionTrace.valueOf("/foo:in:bar").getFrames(), ExecutionTrace.valueOf("/foo"));
        Assert.assertEquals(ExecutionTrace.valueOf("/foo:in:bar").getReference(), ExecutionTrace.valueOf(":in:bar"));

        Assert.assertEquals(
            ExecutionTrace.valueOf("/foo/bar:in:baz:2").getFrames(),
            ExecutionTrace.valueOf("/foo/bar")
        );
        Assert.assertEquals(
            ExecutionTrace.valueOf("/foo/bar:in:baz:2").getReference(),
            ExecutionTrace.valueOf(":in:baz:2")
        );

        Assert.assertEquals(ExecutionTrace.valueOf(":1").getFrames(), ExecutionTrace.empty());
        Assert.assertEquals(ExecutionTrace.valueOf(":1").getReference(), ExecutionTrace.empty());

        Assert.assertEquals(ExecutionTrace.valueOf(":1:2").getFrames(), ExecutionTrace.empty());
        Assert.assertEquals(ExecutionTrace.valueOf(":1:2").getReference(), ExecutionTrace.empty());
    }
}
