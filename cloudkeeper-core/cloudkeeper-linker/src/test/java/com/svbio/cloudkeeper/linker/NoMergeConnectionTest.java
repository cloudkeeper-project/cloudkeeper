package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.linker.examples.BinarySum;
import com.svbio.cloudkeeper.model.ConstraintException;
import com.svbio.cloudkeeper.model.bare.element.module.BareInputModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableChildOutToParentOutConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableCompositeModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableInPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableInputModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableOutPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableParentInToChildInConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutablePort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableProxyModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableSiblingConnection;
import com.svbio.cloudkeeper.model.beans.type.MutableDeclaredType;
import com.svbio.cloudkeeper.model.beans.type.MutableTypeMirror;
import com.svbio.cloudkeeper.model.immutable.Location;
import com.svbio.cloudkeeper.model.immutable.execution.ExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeRepository;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class NoMergeConnectionTest {
    private static final Location ERROR_LOCATION = new Location("test", 24, 5);

    @Test
    public void missingMergeConnectionTest() throws Exception {
        MutableCompositeModule module = new MutableCompositeModule()
            .setDeclaredPorts(Arrays.<MutablePort<?>>asList(
                new MutableInPort()
                    .setSimpleName("inArrayPort")
                    .setType(
                        new MutableDeclaredType()
                            .setDeclaration(Collection.class.getName())
                            .setTypeArguments(Collections.<MutableTypeMirror<?>>singletonList(
                                new MutableDeclaredType()
                                    .setDeclaration(Integer.class.getName())
                            ))
                    ),
                new MutableOutPort()
                    .setSimpleName("outPort")
                    .setType(
                        new MutableDeclaredType()
                            .setDeclaration(Integer.class.getName())
                    )
            ))
            .setModules(Arrays.<MutableModule<?>>asList(
                new MutableInputModule()
                    .setSimpleName("one")
                    .setOutPortType(new MutableDeclaredType().setDeclaration(Integer.class.getName()))
                    .setValue(1),
                new MutableProxyModule()
                    .setDeclaration(BinarySum.class.getName())
                    .setSimpleName("sum")
            ))
            .setConnections(Arrays.<MutableConnection<?>>asList(
                new MutableParentInToChildInConnection()
                    .setFromPort("inArrayPort")
                    .setToModule("sum").setToPort("num1"),
                new MutableSiblingConnection()
                    .setFromModule("one").setFromPort(BareInputModule.OUT_PORT_NAME)
                    .setToModule("sum").setToPort("num2"),
                new MutableChildOutToParentOutConnection()
                    .setFromModule("sum").setFromPort("result")
                    .setToPort("outPort")
                    .setLocation(ERROR_LOCATION)
            ));

        try {
            LinkerOptions linkerOptions = LinkerOptions.nonExecutable();
            RuntimeRepository repository = Linker.createRepository(
                Collections.singletonList(Examples.simpleBundle()), linkerOptions);
            Linker.createAnnotatedExecutionTrace(ExecutionTrace.empty(), module, Collections.emptyList(), repository,
                linkerOptions);
            Assert.fail("Expected exception because of missing merge connection.");
        } catch (ConstraintException exception) {
            Assert.assertSame(exception.getLinkerTrace().get(0).getLocation(), ERROR_LOCATION);
        }
    }
}
