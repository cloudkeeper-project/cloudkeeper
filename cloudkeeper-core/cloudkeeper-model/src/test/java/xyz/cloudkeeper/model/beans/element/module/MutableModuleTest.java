package xyz.cloudkeeper.model.beans.element.module;

import org.testng.annotations.Factory;
import xyz.cloudkeeper.model.beans.MutableLocatableContract;
import xyz.cloudkeeper.model.beans.XmlRootElementContract;
import xyz.cloudkeeper.model.beans.element.serialization.MutableByteSequence;
import xyz.cloudkeeper.model.beans.element.serialization.MutableSerializationNode;
import xyz.cloudkeeper.model.beans.element.serialization.MutableSerializationRoot;
import xyz.cloudkeeper.model.beans.element.serialization.MutableSerializedString;
import xyz.cloudkeeper.model.immutable.element.Index;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MutableModuleTest {
    @Factory
    public Object[] contracts() {
        List<Object> contractsList = new ArrayList<>();
        contractsList.addAll(Arrays.asList(MutableLocatableContract.contractsFor(MutableCompositeModule.class,
            MutableInputModule.class, MutableLoopModule.class, MutableProxyModule.class)));
        contractsList.add(new XmlRootElementContract(
            new MutableInputModule()
                .setRaw(
                    new MutableSerializationRoot()
                        .setDeclaration("test.FooSerialization")
                        .setEntries(Arrays.<MutableSerializationNode<?>>asList(
                            new MutableSerializationRoot()
                                .setKey(SimpleName.identifier("a"))
                                .setDeclaration("test.BarSerialization")
                                .setEntries(Collections.<MutableSerializationNode<?>>singletonList(
                                    new MutableSerializedString()
                                        .setString("Hello")
                                )),
                            new MutableByteSequence()
                                .setKey(SimpleName.identifier("c"))
                                .setArray("World".getBytes()),
                            new MutableSerializationRoot()
                                .setKey(Index.index(4))
                                .setDeclaration("test.BazSerialization")
                                .setEntries(Collections.<MutableSerializationNode<?>>singletonList(
                                    new MutableSerializationRoot()
                                        .setKey(SimpleName.identifier("e"))
                                        .setEntries(Collections.<MutableSerializationNode<?>>singletonList(
                                            new MutableByteSequence()
                                                .setKey(SimpleName.identifier("f"))
                                                .setArray("!".getBytes())
                                        ))
                                ))
                        ))
                )
        ));
        return contractsList.toArray();
    }
}
