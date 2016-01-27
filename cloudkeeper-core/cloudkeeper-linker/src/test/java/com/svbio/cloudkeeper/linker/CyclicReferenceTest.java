package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.module.BareProxyModule;
import com.svbio.cloudkeeper.model.beans.element.MutableBundle;
import com.svbio.cloudkeeper.model.beans.element.MutablePackage;
import com.svbio.cloudkeeper.model.beans.element.MutablePluginDeclaration;
import com.svbio.cloudkeeper.model.beans.element.module.MutableChildOutToParentOutConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableCompositeModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableCompositeModuleDeclaration;
import com.svbio.cloudkeeper.model.beans.element.module.MutableConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableInPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableOutPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableParentInToChildInConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutablePort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableProxyModule;
import com.svbio.cloudkeeper.model.beans.type.MutableDeclaredType;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeRepository;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeCompositeModuleDeclaration;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import static com.svbio.cloudkeeper.model.immutable.element.Name.qualifiedName;
import static com.svbio.cloudkeeper.model.immutable.element.SimpleName.identifier;

public class CyclicReferenceTest {
    @Test
    public void cyclicDependenciesInCompositeModules() throws LinkerException {
        MutableBundle mutableBundle = new MutableBundle()
            .setBundleIdentifier(URI.create("x-test:" + getClass().getName()))
            .setPackages(Collections.singletonList(
                new MutablePackage()
                    .setQualifiedName("test")
                    .setDeclarations(Arrays.<MutablePluginDeclaration<?>>asList(
                        new MutableCompositeModuleDeclaration()
                            .setSimpleName("Composite1")
                            .setTemplate(
                                new MutableCompositeModule()
                                    .setDeclaredPorts(Arrays.<MutablePort<?>>asList(
                                        new MutableInPort()
                                            .setSimpleName("in")
                                            .setType(new MutableDeclaredType().setDeclaration(Object.class.getName())),
                                        new MutableOutPort()
                                            .setSimpleName("out")
                                            .setType(new MutableDeclaredType().setDeclaration(Object.class.getName()))
                                    ))
                                    .setModules(Collections.<MutableModule<?>>singletonList(
                                        new MutableProxyModule()
                                            .setSimpleName("f")
                                            .setDeclaration("test.Composite2")
                                    ))
                                    .setConnections(Arrays.<MutableConnection<?>>asList(
                                        new MutableParentInToChildInConnection()
                                            .setToModule("f")
                                            .setFromPort("in")
                                            .setToPort("in"),
                                        new MutableChildOutToParentOutConnection()
                                            .setFromModule("f")
                                            .setFromPort("out")
                                            .setToPort("out")
                                    ))
                            ),
                        new MutableCompositeModuleDeclaration()
                            .setSimpleName("Composite2")
                            .setTemplate(
                                new MutableCompositeModule()
                                    .setDeclaredPorts(Arrays.<MutablePort<?>>asList(
                                        new MutableInPort()
                                            .setSimpleName("in")
                                            .setType(new MutableDeclaredType().setDeclaration(Object.class.getName())),
                                        new MutableOutPort()
                                            .setSimpleName("out")
                                            .setType(new MutableDeclaredType().setDeclaration(Object.class.getName()))
                                    ))
                                    .setModules(Collections.<MutableModule<?>>singletonList(
                                        new MutableProxyModule()
                                            .setSimpleName("g")
                                            .setDeclaration("test.Composite1")
                                    ))
                                    .setConnections(Arrays.<MutableConnection<?>>asList(
                                        new MutableParentInToChildInConnection()
                                            .setToModule("g")
                                            .setFromPort("in")
                                            .setToPort("in"),
                                        new MutableChildOutToParentOutConnection()
                                            .setFromModule("g")
                                            .setFromPort("out")
                                            .setToPort("out")
                                    ))
                            )
                    ))
            ));


        RuntimeRepository repository
            = Linker.createRepository(Collections.singletonList(mutableBundle), LinkerOptions.nonExecutable());

        @Nullable RuntimeCompositeModuleDeclaration composite1Decl
            = repository.getElement(RuntimeCompositeModuleDeclaration.class, qualifiedName("test.Composite1"));
        @Nullable RuntimeCompositeModuleDeclaration composite2Decl
            = repository.getElement(RuntimeCompositeModuleDeclaration.class, qualifiedName("test.Composite2"));
        Assert.assertTrue(composite1Decl.getTemplate() != null);
        Assert.assertTrue(composite2Decl.getTemplate() != null);

        Assert.assertSame(
            ((BareProxyModule) composite1Decl.getTemplate().getModule(identifier("f"))).getDeclaration(),
            composite2Decl
        );
        Assert.assertSame(
            ((BareProxyModule) composite2Decl.getTemplate().getModule(identifier("g"))).getDeclaration(),
            composite1Decl
        );
    }
}
