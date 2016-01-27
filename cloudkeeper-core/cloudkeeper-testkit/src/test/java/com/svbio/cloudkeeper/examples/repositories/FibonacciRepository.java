package com.svbio.cloudkeeper.examples.repositories;

import com.svbio.cloudkeeper.examples.modules.Fibonacci;
import com.svbio.cloudkeeper.model.bare.element.BareBundle;
import com.svbio.cloudkeeper.model.beans.element.MutableBundle;
import com.svbio.cloudkeeper.model.beans.element.MutablePackage;
import com.svbio.cloudkeeper.model.beans.element.MutablePluginDeclaration;

import java.net.URI;
import java.util.Collections;

public final class FibonacciRepository implements TestKitBundleProvider {
    public static final URI BUNDLE_ID = TestKitRuntimeContextFactory.bundleIdentifier(FibonacciRepository.class);

    @Override
    public BareBundle get() {
        return new MutableBundle()
            .setBundleIdentifier(BUNDLE_ID)
            .setPackages(Collections.singletonList(
                new MutablePackage()
                    .setQualifiedName(Fibonacci.class.getPackage().getName())
                    .setDeclarations(Collections.<MutablePluginDeclaration<?>>singletonList(
                        Shared.declarationFromClass(Fibonacci.class)
                    ))
            ));
    }
}
