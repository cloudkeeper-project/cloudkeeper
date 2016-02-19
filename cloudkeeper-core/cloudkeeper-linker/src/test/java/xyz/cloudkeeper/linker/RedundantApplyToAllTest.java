package xyz.cloudkeeper.linker;

import org.testng.Assert;
import org.testng.annotations.Test;
import xyz.cloudkeeper.linker.examples.BinarySum;
import xyz.cloudkeeper.model.ConstraintException;
import xyz.cloudkeeper.model.beans.element.module.MutableCompositeModule;
import xyz.cloudkeeper.model.beans.element.module.MutableConnection;
import xyz.cloudkeeper.model.beans.element.module.MutableInPort;
import xyz.cloudkeeper.model.beans.element.module.MutableModule;
import xyz.cloudkeeper.model.beans.element.module.MutableOutPort;
import xyz.cloudkeeper.model.beans.element.module.MutableParentInToChildInConnection;
import xyz.cloudkeeper.model.beans.element.module.MutablePort;
import xyz.cloudkeeper.model.beans.element.module.MutableProxyModule;
import xyz.cloudkeeper.model.beans.type.MutableDeclaredType;
import xyz.cloudkeeper.model.beans.type.MutableTypeMirror;
import xyz.cloudkeeper.model.immutable.Location;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.element.RuntimeRepository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class RedundantApplyToAllTest {
    private static final Location ERROR_LOCATION = new Location("test", 24, 5);

    @Test
    public void test() throws Exception {
        MutableDeclaredType integerCollections = new MutableDeclaredType()
            .setDeclaration(Collection.class.getName())
            .setTypeArguments(Collections.<MutableTypeMirror<?>>singletonList(
                new MutableDeclaredType()
                    .setDeclaration(Integer.class.getName())
            ));
        MutableCompositeModule module = new MutableCompositeModule()
            .setDeclaredPorts(Arrays.<MutablePort<?>>asList(
                new MutableInPort()
                    .setSimpleName("inFirstArrayPort")
                    .setType(integerCollections),
                new MutableInPort()
                    .setSimpleName("inSecondArrayPort")
                    .setType(integerCollections),
                new MutableOutPort()
                    .setSimpleName("outPort")
                    .setType(integerCollections)
            ))
            .setModules(Collections.<MutableModule<?>>singletonList(
                new MutableProxyModule()
                    .setDeclaration(BinarySum.class.getName())
                    .setSimpleName("sum")
            ))
            .setConnections(Arrays.<MutableConnection<?>>asList(
                new MutableParentInToChildInConnection()
                    .setFromPort("inFirstArrayPort")
                    .setToModule("sum").setToPort("num1"),
                new MutableParentInToChildInConnection()
                    .setFromPort("inSecondArrayPort")
                    .setToModule("sum").setToPort("num2")
                    .setLocation(ERROR_LOCATION)
            ));

        try {
            LinkerOptions linkerOptions = LinkerOptions.nonExecutable();
            RuntimeRepository repository = Linker.createRepository(
                Collections.singletonList(Examples.simpleBundle()), linkerOptions);
            Linker.createAnnotatedExecutionTrace(ExecutionTrace.empty(), module, Collections.emptyList(), repository,
                linkerOptions);
            Assert.fail("Expected exception because of more than one apply-to-all connections.");
        } catch (ConstraintException exception) {
            Assert.assertSame(exception.getLinkerTrace().get(0).getLocation(), ERROR_LOCATION);
        }
    }
}
