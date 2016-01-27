package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.linker.CopyContext.CopyContextSupplier;
import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.BareBundle;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeElement;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeRepository;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class RepositoryImpl extends AbstractFreezable implements RuntimeRepository, ElementResolver {
    private final BundleImpl systemBundle;
    private final ImmutableList<BundleImpl> bundles;

    /**
     * Constructor (from unverified bundles and a verified and frozen system bundle). Instance needs to be explicitly
     * frozen before use.
     *
     * @param originalBundles list of bare bundles; should not contain the system bundle
     * @param systemBundle system bundle that is implicitly included
     * @param parentContext parent copy context
     */
    RepositoryImpl(List<? extends BareBundle> originalBundles, BundleImpl systemBundle, CopyContext parentContext)
            throws LinkerException {
        super("repository", parentContext);
        Objects.requireNonNull(systemBundle);

        this.systemBundle = systemBundle;
        Map<Name, BundleImpl> topLevelDeclarations = new LinkedHashMap<>();
        for (PackageImpl systemBundlePackage: systemBundle.getPackages()) {
            for (PluginDeclarationImpl pluginDeclaration: systemBundlePackage.getDeclarations()) {
                topLevelDeclarations.put(pluginDeclaration.getQualifiedName(), systemBundle);
            }
        }

        List<BundleImpl> newBundles = new ArrayList<>(originalBundles.size());
        CopyContextSupplier bundleContextSupplier = getCopyContext().newContextForListProperty("bundles").supplier();
        for (BareBundle originalBundle: originalBundles) {
            BundleImpl bundle = new BundleImpl(originalBundle, bundleContextSupplier.get());
            newBundles.add(bundle);
            for (PackageImpl bundlePackage: bundle.getPackages()) {
                for (PluginDeclarationImpl pluginDeclaration: bundlePackage.getDeclarations()) {
                    Name name = bundlePackage.getQualifiedName().join(pluginDeclaration.getSimpleName());
                    @Nullable BundleImpl existingBundle = topLevelDeclarations.get(name);
                    @Nullable URI existingBundleIdentifier = existingBundle == null
                        ? null
                        : existingBundle.getBundleIdentifier();
                    Preconditions.requireCondition(existingBundle == null, bundlePackage.getCopyContext(),
                        "Duplicate top-level plug-in declaration '%s' (bundles '%s' and '%s').",
                        name, bundle.getBundleIdentifier(), existingBundleIdentifier);
                    topLevelDeclarations.put(name, bundle);
                }
            }
        }
        bundles = ImmutableList.copyOf(newBundles);
    }

    @Override
    public ImmutableList<BundleImpl> getBundles() {
        return bundles;
    }

    @Override
    @Nullable
    public <T extends RuntimeElement> T getElement(Class<T> clazz, Name name) {
        @Nullable T element = systemBundle.getElement(clazz, name);
        if (element != null) {
            return element;
        }
        for (BundleImpl bundle: bundles) {
            element = bundle.getElement(clazz, name);
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    @Override
    void collectEnclosed(Collection<AbstractFreezable> freezables) {
        freezables.addAll(bundles);
    }

    @Override
    void preProcessFreezable(FinishContext context) { }

    @Override
    void finishFreezable(FinishContext context) { }

    @Override
    void verifyFreezable(VerifyContext context) { }
}
