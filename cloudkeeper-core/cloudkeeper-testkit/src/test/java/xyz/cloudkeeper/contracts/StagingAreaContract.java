package xyz.cloudkeeper.contracts;

import cloudkeeper.serialization.ByteSequenceMarshaler;
import cloudkeeper.serialization.IntegerMarshaler;
import cloudkeeper.serialization.StringMarshaler;
import cloudkeeper.types.ByteSequence;
import org.testng.Assert;
import org.testng.ITest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import scala.concurrent.Future;
import xyz.cloudkeeper.dsl.ModuleFactory;
import xyz.cloudkeeper.dsl.SimpleModule;
import xyz.cloudkeeper.dsl.SimpleModulePlugin;
import xyz.cloudkeeper.examples.modules.Fibonacci;
import xyz.cloudkeeper.linker.Linker;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.api.staging.StagingException;
import xyz.cloudkeeper.model.immutable.element.Index;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.element.RuntimeRepository;
import xyz.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationDeclaration;
import xyz.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationRoot;
import xyz.cloudkeeper.model.runtime.execution.IllegalExecutionTraceException;
import xyz.cloudkeeper.model.util.ByteSequences;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * Contract test for basic staging-area functionality.
 *
 * <p>This contract test verifies all fundamental functionality of a
 * {@link StagingArea} implementation. Implementations passing this contract
 * test are expected to be successfully usable in a single-JVM setting. This contract test does <em>not</em>, however,
 * verify that the provider returned by
 * {@link StagingArea#getStagingAreaProvider()} can be serialized, deserialized
 * and then be used to reconstruct the staging area. This functionality is verified by the contract test
 * {@link RemoteStagingAreaContract}.
 */
public final class StagingAreaContract implements ITest {
    private static final ByteSequence EMPTY_BYTE_SEQUENCE = ByteSequences.arrayBacked(new byte[]{ });
    private static final ByteSequence FOX_BYTE_SEQUENCE = ByteSequences.arrayBacked(
        "The quick brown fox\njumps.\n".getBytes(StandardCharsets.UTF_8)
    );
    private static final ByteSequence LOREM_BYTE_SEQUENCE = ByteSequences.arrayBacked(
        ("Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor\n"
            + "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis\n"
            + "nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.\n"
            + "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu\n"
            + "fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in\n"
            + "culpa qui officia deserunt mollit anim id est laborum.")
        .getBytes(StandardCharsets.UTF_8)
    );

    private final StagingAreaContractProvider provider;
    @Nullable private StagingAreaContractHelper fibonacciModuleHelper;
    @Nullable private StagingArea stagingAreaByteSequence;

    public StagingAreaContract(StagingAreaContractProvider provider) {
        this.provider = Objects.requireNonNull(provider);
    }

    @Override
    public String getTestName() {
        return provider.getClass().getName();
    }

    private <T> T await(Future<T> awaitable) throws Exception {
        return provider.await(awaitable);
    }

    @BeforeClass
    public void setup() throws IOException {
        provider.preContract();

        fibonacciModuleHelper = new StagingAreaContractHelper(provider, Fibonacci.class);

        StagingAreaContractHelper fileModuleHelper = new StagingAreaContractHelper(provider, FileModule.class);
        stagingAreaByteSequence = fileModuleHelper.createStagingArea("stagingAreaByteSequence");
    }

    @Test
    public void deleteTestBadArguments() throws StagingException {
        StagingArea stagingArea = fibonacciModuleHelper.createStagingArea("deleteTestBadArguments");

        try {
            stagingArea.delete(null);
            Assert.fail();
        } catch (NullPointerException ignored) { /* expected */ }

        try {
            stagingArea.delete(ExecutionTrace.empty());
            Assert.fail();
        } catch (IllegalArgumentException ignored) { /* expected */ }

        try {
            stagingArea.delete(ExecutionTrace.empty().resolveArrayIndex(Index.index(1)));
            Assert.fail();
        } catch (IllegalArgumentException ignored) { /* expected */ }

        try {
            // Value reference cannot have array indices
            ExecutionTrace trace
                = ExecutionTrace.empty().resolveInPort(SimpleName.identifier("foo")).resolveArrayIndex(Index.index(1));
            stagingArea.delete(trace);
            Assert.fail();
        } catch (IllegalArgumentException ignored) { /* expected */ }

        try {
            stagingArea.delete(ExecutionTrace.empty().resolveInPort(SimpleName.identifier("foo")));
            Assert.fail();
        } catch (IllegalExecutionTraceException ignored) { }

        try {
            stagingArea.delete(ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("n")));
            Assert.fail();
        } catch (IllegalExecutionTraceException ignored) { }
    }

    @Test
    public void copyTestBadArguments() throws StagingException {
        StagingArea stagingArea = fibonacciModuleHelper.createStagingArea("copyTestBadArguments");

        try {
            // NullPointerException are thrown before other exception!
            stagingArea.copy(null, ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("foo")));
            Assert.fail();
        } catch (NullPointerException ignored) { /* expected */ }

        try {
            stagingArea.copy(ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("foo")), null);
            Assert.fail();
        } catch (NullPointerException ignored) { /* expected */ }

        try {
            // IllegalArgumentException is thrown before IllegalExecutionTraceException!
            stagingArea.copy(
                ExecutionTrace.empty().resolveInPort(SimpleName.identifier("foo")),
                ExecutionTrace.empty().resolveModule(SimpleName.identifier("bar"))
            );
            Assert.fail();
        } catch (IllegalArgumentException ignored) { /* expected */ }

        try {
            // IllegalArgumentException is thrown before IllegalExecutionTraceException!
            stagingArea.copy(
                ExecutionTrace.empty().resolveModule(SimpleName.identifier("foo")),
                ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("bar"))
            );
            Assert.fail();
        } catch (IllegalArgumentException ignored) { /* expected */ }

        try {
            stagingArea.copy(
                ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("n")),
                ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("foo"))
            );
            Assert.fail();
        } catch (IllegalExecutionTraceException ignored) { }

        try {
            // Arguments are checked for syntactical correctness before the staging area is accessed. Hence, the
            // following must throw an IllegalExecutionTraceException because of "foo".
            stagingArea.copy(
                ExecutionTrace.empty().resolveInPort(SimpleName.identifier("n")),
                ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("foo"))
            );
            Assert.fail();
        } catch (IllegalExecutionTraceException ignored) { }
    }

    @Test
    public void putObjectTestBadArguments() throws StagingException {
        StagingArea stagingArea = fibonacciModuleHelper.createStagingArea("putObjectTestBadArguments");

        try {
            // The null-check comes first, so this must produce a NullPointerException
            stagingArea.putObject(ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("foo")), null);
            Assert.fail();
        } catch (NullPointerException ignored) { /* expected */ }

        try {
            stagingArea.putObject(null, 1);
            Assert.fail();
        } catch (NullPointerException ignored) { /* expected */ }

        try {
            stagingArea.putObject(
                ExecutionTrace.empty().resolveModule(SimpleName.identifier("foo")),
                1
            );
            Assert.fail();
        } catch (IllegalArgumentException ignored) { /* expected */ }

        try {
            stagingArea.putObject(
                ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("n")),
                2
            );
            Assert.fail();
        } catch (IllegalExecutionTraceException ignored) { }

        try {
            stagingArea.putObject(
                ExecutionTrace.empty().resolveInPort(SimpleName.identifier("foo")),
                1
            );
            Assert.fail();
        } catch (IllegalExecutionTraceException ignored) { }
    }

    @Test
    public void putSerializationTreeTestBadArguments() throws IOException {
        StagingArea stagingArea
            = fibonacciModuleHelper.createStagingArea("putSerializationTreeTestBadArguments");

        RuntimeRepository repository = fibonacciModuleHelper.getRepository();
        RuntimeSerializationDeclaration integerSerializationDecl = repository.getElement(
            RuntimeSerializationDeclaration.class, Name.qualifiedName(IntegerMarshaler.class.getName()));
        RuntimeSerializationDeclaration stringSerializationDecl = repository.getElement(
            RuntimeSerializationDeclaration.class, Name.qualifiedName(StringMarshaler.class.getName()));
        RuntimeSerializationRoot serializationTree
            = Linker.marshalToTree(1, Arrays.asList(integerSerializationDecl, stringSerializationDecl));

        try {
            stagingArea.putSerializationTree(
                ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("foo")),
                null
            );
            Assert.fail();
        } catch (NullPointerException ignored) { /* expected */ }

        try {
            stagingArea.putSerializationTree(null, serializationTree);
            Assert.fail();
        } catch (NullPointerException ignored) { /* expected */ }

        try {
            stagingArea.putSerializationTree(
                ExecutionTrace.empty().resolveModule(SimpleName.identifier("foo")),
                serializationTree
            );
            Assert.fail();
        } catch (IllegalArgumentException ignored) { /* expected */ }

        try {
            stagingArea.putSerializationTree(
                ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("n")),
                serializationTree
            );
            Assert.fail();
        } catch (IllegalExecutionTraceException ignored) { }

        try {
            stagingArea.putSerializationTree(
                ExecutionTrace.empty().resolveInPort(SimpleName.identifier("foo")),
                serializationTree
            );
            Assert.fail();
        } catch (IllegalExecutionTraceException ignored) { }
    }

    @Test
    public void getTestBadArguments() throws StagingException {
        StagingArea stagingArea = fibonacciModuleHelper.createStagingArea("getTestBadArguments");

        try {
            stagingArea.getObject(null);
            Assert.fail();
        } catch (NullPointerException ignored) { /* expected */ }

        try {
            stagingArea.getObject(ExecutionTrace.empty().resolveModule(SimpleName.identifier("foo")));
            Assert.fail();
        } catch (IllegalArgumentException ignored) { /* expected */ }

        try {
            stagingArea.getObject(ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("n")));
            Assert.fail();
        } catch (IllegalExecutionTraceException ignored) { }

        try {
            stagingArea.getObject(ExecutionTrace.empty().resolveInPort(SimpleName.identifier("foo")));
            Assert.fail();
        } catch (IllegalExecutionTraceException ignored) { }
    }

    @Test
    public void existsTestBadArguments() throws StagingException {
        StagingArea stagingArea = fibonacciModuleHelper.createStagingArea("existsTestBadArguments");

        try {
            stagingArea.exists(null);
            Assert.fail();
        } catch (NullPointerException ignored) { /* expected */ }

        try {
            stagingArea.exists(ExecutionTrace.empty().resolveModule(SimpleName.identifier("foo")));
            Assert.fail();
        } catch (IllegalArgumentException ignored) { /* expected */ }

        try {
            stagingArea.exists(ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("n")));
            Assert.fail();
        } catch (IllegalExecutionTraceException ignored) { }

        try {
            stagingArea.exists(ExecutionTrace.empty().resolveInPort(SimpleName.identifier("foo")));
            Assert.fail();
        } catch (IllegalExecutionTraceException ignored) { }
    }

    @Test
    public void getMaximumIndexTestBadArguments() throws StagingException {
        StagingArea stagingArea = fibonacciModuleHelper.createStagingArea("getMaximumIndexTestBadArguments");

        try {
            stagingArea.getMaximumIndex(null, null);
            Assert.fail();
        } catch (NullPointerException ignored) { /* expected */ }

        try {
            stagingArea.getMaximumIndex(ExecutionTrace.empty().resolveModule(SimpleName.identifier("foo")), null);
            Assert.fail();
        } catch (IllegalArgumentException ignored) { /* expected */ }

        try {
            stagingArea.getMaximumIndex(ExecutionTrace.empty().resolveIteration(Index.index(1)), null);
            Assert.fail();
        } catch (IllegalArgumentException ignored) { /* expected */ }

        try {
            // The following will not trigger an InvalidTraceException (as in the other tests), but the argument is
            // syntactically invalid.
            stagingArea.getMaximumIndex(ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("n")), null);
            Assert.fail();
        } catch (IllegalArgumentException ignored) { }
    }

    @Test
    public void resolveDescendantTestBadArguments() throws StagingException {
        StagingArea stagingArea
            = fibonacciModuleHelper.createStagingArea("resolveDescendantTestBadArguments");

        try {
            stagingArea.resolveDescendant(null);
            Assert.fail();
        } catch (NullPointerException ignored) { /* expected */ }

        try {
            // The following will not trigger an InvalidTraceException (as in the other tests), but the argument is
            // syntactically invalid.
            stagingArea.resolveDescendant(ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("n")));
            Assert.fail();
        } catch (IllegalArgumentException ignored) { }
    }

    @Test(invocationCount = 2)
    public void putSerializationTree() throws Exception {
        StagingArea stagingArea = fibonacciModuleHelper.createStagingArea("putSerializationTree");
        RuntimeRepository repository = fibonacciModuleHelper.getRepository();

        RuntimeSerializationDeclaration integerSerialization = repository.getElement(
            RuntimeSerializationDeclaration.class, Name.qualifiedName(IntegerMarshaler.class.getName()));
        RuntimeSerializationDeclaration stringSerialization = repository.getElement(
            RuntimeSerializationDeclaration.class, Name.qualifiedName(StringMarshaler.class.getName()));
        RuntimeSerializationRoot serializationTree
            = Linker.marshalToTree(5, Arrays.asList(integerSerialization, stringSerialization));

        ExecutionTrace trace = ExecutionTrace.empty().resolveInPort(SimpleName.identifier("n"));
        await(stagingArea.putSerializationTree(trace, serializationTree));

        Assert.assertEquals(await(stagingArea.getObject(trace)), 5);
    }

    @Test
    public void fibonacciTest() throws Exception {
        StagingArea stagingArea = fibonacciModuleHelper.createStagingArea("fibonacciTest");

        ExecutionTrace empty = ExecutionTrace.empty();
        ExecutionTrace rootInPortN = empty.resolveInPort(SimpleName.identifier("n"));
        await(stagingArea.putObject(rootInPortN, 5));
        Assert.assertEquals(await(stagingArea.getObject(rootInPortN)), 5);
        Assert.assertTrue(await(stagingArea.exists(rootInPortN)));

        ExecutionTrace loop = empty.resolveContent().resolveModule(SimpleName.identifier("loop"));
        ExecutionTrace loopInPortCount = loop.resolveInPort(SimpleName.identifier("count"));
        ExecutionTrace loopInPortLast = loop.resolveInPort(SimpleName.identifier("last"));
        ExecutionTrace loopInPortSecondLast = loop.resolveInPort(SimpleName.identifier("secondLast"));

        await(stagingArea.copy(rootInPortN, loopInPortCount));
        Assert.assertEquals(await(stagingArea.getObject(loopInPortCount)), 5);
        Assert.assertTrue(await(stagingArea.exists(loopInPortCount)));
        await(stagingArea.putObject(loopInPortLast, 1));
        await(stagingArea.putObject(loopInPortSecondLast, 0));

        await(stagingArea.delete(rootInPortN));
        Assert.assertFalse(await(stagingArea.exists(rootInPortN)));

        ExecutionTrace loop0 = loop.resolveContent().resolveIteration(Index.index(0));
        ExecutionTrace loop0InPortCount = loop0.resolveInPort(SimpleName.identifier("count"));
        ExecutionTrace loop0InPortLast = loop0.resolveInPort(SimpleName.identifier("last"));
        ExecutionTrace loop0InPortSecondLast = loop0.resolveInPort(SimpleName.identifier("secondLast"));
        await(stagingArea.copy(loopInPortCount, loop0InPortCount));
        await(stagingArea.copy(loopInPortLast, loop0InPortLast));
        await(stagingArea.copy(loopInPortSecondLast, loop0InPortSecondLast));

        ExecutionTrace sum = loop0.resolveContent().resolveModule(SimpleName.identifier("sum"));
        ExecutionTrace sumInPortNum1 = sum.resolveInPort(SimpleName.identifier("num1"));
        ExecutionTrace sumInPortNum2 = sum.resolveInPort(SimpleName.identifier("num2"));
        await(stagingArea.copy(loop0InPortLast, sumInPortNum1));
        await(stagingArea.copy(loopInPortSecondLast, sumInPortNum2));

        ExecutionTrace sumOutPortSum = sum.resolveOutPort(SimpleName.identifier("sum"));
        await(
            stagingArea.putObject(
                sumOutPortSum,
                (Integer) await(stagingArea.getObject(sumInPortNum1))
                    + (Integer) await(stagingArea.getObject(sumInPortNum2))
            )
        );
        Assert.assertEquals(await(stagingArea.getObject(sumOutPortSum)), 1);

        ExecutionTrace loop0OutPortLast = loop0.resolveOutPort(SimpleName.identifier("last"));
        await(stagingArea.copy(sumOutPortSum, loop0OutPortLast));

        await(stagingArea.delete(sum));
        Assert.assertFalse(await(stagingArea.exists(sumInPortNum1)));
        Assert.assertFalse(await(stagingArea.exists(sumInPortNum2)));
        Assert.assertFalse(await(stagingArea.exists(sumOutPortSum)));
    }


    // Tests involving ByteSequence

    @SimpleModulePlugin("Dummy module only used for this test.")
    public abstract static class FileModule extends SimpleModule<FileModule> {
        public abstract InPort<ByteSequence> inputFile();
        public abstract OutPort<Collection<ByteSequence>> outputFiles();
    }

    @Test(invocationCount = 2)
    public void putObjectTestByteSequence() throws Exception {
        assert stagingAreaByteSequence != null;

        FileModule module = ModuleFactory.getDefault().create(FileModule.class);
        MutableByteSequence mutableByteSequence = new MutableByteSequence(FOX_BYTE_SEQUENCE);
        await(
            stagingAreaByteSequence.putObject(
                ExecutionTrace.empty().resolveInPort(module.inputFile().getSimpleName()),
                mutableByteSequence
            )
        );
        // Even if we modify the byte sequence, the original content must not be lost -- because it has been committed
        // to the staging area.
        mutableByteSequence.setByteSequence(EMPTY_BYTE_SEQUENCE);
    }

    @Test(dependsOnMethods = "putObjectTestByteSequence")
    public void getObjectTest() throws Exception {
        assert stagingAreaByteSequence != null;

        FileModule module = ModuleFactory.getDefault().create(FileModule.class);
        ByteSequence byteSequence = (ByteSequence) await(
            stagingAreaByteSequence.getObject(ExecutionTrace.empty().resolveInPort(module.inputFile().getSimpleName()))
        );

        assertEqualByteSequences(FOX_BYTE_SEQUENCE, byteSequence);
    }

    private static void assertEqualByteSequences(ByteSequence expected, ByteSequence actual)
        throws IOException, URISyntaxException {

        try (
            BufferedInputStream expectedInputStream = new BufferedInputStream(expected.newInputStream());
            BufferedInputStream actualInputStream = new BufferedInputStream(actual.newInputStream())
        ) {
            int expectedByte;
            int actualByte;
            do {
                expectedByte = expectedInputStream.read();
                actualByte = actualInputStream.read();
                Assert.assertEquals(actualByte, expectedByte, "ByteSequence does not have expected content");
            } while (expectedByte != -1);
        }
    }

    @Test(invocationCount = 2)
    public void putObjectTestCollection() throws Exception {
        assert stagingAreaByteSequence != null;

        FileModule module = ModuleFactory.getDefault().create(FileModule.class);
        await(
            stagingAreaByteSequence.putObject(
                ExecutionTrace.empty().resolveOutPort(module.outputFiles().getSimpleName())
                    .resolveArrayIndex(Index.index(2)),
                LOREM_BYTE_SEQUENCE
            )
        );
    }

    /**
     * This method depends on {@link #putObjectTestCollection()} because we want to make sure that collection elements
     * can be put in reverse order.
     */
    @Test(dependsOnMethods = {"putObjectTestByteSequence", "putObjectTestCollection" }, invocationCount = 2)
    public void copyObjectTestCollection() throws Exception {
        assert stagingAreaByteSequence != null;

        FileModule module = ModuleFactory.getDefault().create(FileModule.class);
        await(
            stagingAreaByteSequence.copy(
                ExecutionTrace.empty().resolveInPort(module.inputFile().getSimpleName()),
                ExecutionTrace.empty().resolveOutPort(module.outputFiles().getSimpleName())
                    .resolveArrayIndex(Index.index(1))
            )
        );
    }

    @Test(dependsOnMethods = "copyObjectTestCollection")
    public void getObjectTestCollection() throws Exception {
        assert stagingAreaByteSequence != null;

        FileModule module = ModuleFactory.getDefault().create(FileModule.class);
        ByteSequence actualFoxByteSequence = (ByteSequence) await(
            stagingAreaByteSequence.getObject(
                ExecutionTrace.empty().resolveOutPort(module.outputFiles().getSimpleName())
                    .resolveArrayIndex(Index.index(1))
            )
        );
        ByteSequence actualLoremByteSequence = (ByteSequence) await(
            stagingAreaByteSequence.getObject(
                ExecutionTrace.empty().resolveOutPort(module.outputFiles().getSimpleName())
                    .resolveArrayIndex(Index.index(2))
            )
        );

        assertEqualByteSequences(FOX_BYTE_SEQUENCE, actualFoxByteSequence);
        assertEqualByteSequences(LOREM_BYTE_SEQUENCE, actualLoremByteSequence);
    }

    private static final class MutableByteSequence implements ByteSequence {
        private ByteSequence byteSequence;

        private MutableByteSequence(ByteSequence byteSequence) {
            this.byteSequence = byteSequence;
        }

        private void setByteSequence(ByteSequence byteSequence) {
            this.byteSequence = byteSequence;
        }

        @Override
        public ByteSequenceMarshaler.Decorator getDecorator() {
            return byteSequence.getDecorator();
        }

        @Nullable
        @Override
        public URI getURI() {
            return byteSequence.getURI();
        }

        @Override
        public boolean isSelfContained() {
            return false;
        }

        @Override
        public long getContentLength() throws IOException {
            return byteSequence.getContentLength();
        }

        @Override
        public String getContentType() throws IOException {
            return byteSequence.getContentType();
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return byteSequence.newInputStream();
        }
    }
}
