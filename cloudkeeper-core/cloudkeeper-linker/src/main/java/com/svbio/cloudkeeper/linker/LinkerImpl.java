package com.svbio.cloudkeeper.linker;

import cloudkeeper.serialization.CollectionMarshaler;
import cloudkeeper.serialization.SerializableMarshaler;
import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.BareBundle;
import com.svbio.cloudkeeper.model.bare.element.module.BareModule;
import com.svbio.cloudkeeper.model.bare.execution.BareExecutionTrace;
import com.svbio.cloudkeeper.model.bare.execution.BareOverride;
import com.svbio.cloudkeeper.model.beans.SystemBundle;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.immutable.execution.ExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeRepository;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Class that implements the linker.
 *
 * <p>See class {@link Linker} for the exposed public API. Note that the link-methods are only "compatible" with
 * repositories linked by this class's singleton instance.
 */
final class LinkerImpl extends AbstractFreezable {
    private static final LinkerImpl INSTANCE;

    private final BundleImpl systemBundle;
    private final CloudKeeperTypeReflection typeReflection;
    private final ImmutableList<SerializationDeclarationImpl> defaultSerializationDeclarations;

    static {
        try {
            INSTANCE = new LinkerImpl();
            INSTANCE.complete();
        } catch (LinkerException exception) {
            throw new IllegalStateException("Failed to construct singleton linker instance.", exception);
        }
    }

    static LinkerImpl getInstance() {
        return INSTANCE;
    }

    private LinkerImpl() throws LinkerException {
        super(State.CREATED, CopyContext.rootContext());

        typeReflection = new CloudKeeperTypeReflection();
        systemBundle = new BundleImpl(SystemBundle.newSystemBundle(), getCopyContext());
        defaultSerializationDeclarations = Arrays
            .asList(CollectionMarshaler.class, SerializableMarshaler.class)
            .stream()
            .map(clazz
                -> systemBundle.getElement(SerializationDeclarationImpl.class, Name.qualifiedName(clazz.getName())))
            .collect(ImmutableList.collector());
    }

    private void requireLinkedRepositoryImpl(RuntimeRepository repository) {
        Objects.requireNonNull(repository);
        Name objectDeclarationName = Name.qualifiedName(Object.class.getName());
        if (!(repository instanceof RepositoryImpl)
                || repository.getElement(TypeDeclarationImpl.class, objectDeclarationName)
                    != systemBundle.getElement(TypeDeclarationImpl.class, objectDeclarationName)) {
            throw new IllegalArgumentException("Repository was not created by this linker.");
        }
    }

    AnnotatedExecutionTraceImpl createAnnotatedExecutionTrace(BareExecutionTrace absoluteTrace, BareModule bareModule,
            List<? extends BareOverride> overrides, RuntimeRepository repository, LinkerOptions linkerOptions)
            throws LinkerException {
        Objects.requireNonNull(absoluteTrace);
        Objects.requireNonNull(bareModule);
        overrides.forEach(Objects::requireNonNull);
        requireLinkedRepositoryImpl(repository);

        AnnotatedExecutionTraceImpl rootTrace = new AnnotatedExecutionTraceImpl(
            ExecutionTrace.copyOf(absoluteTrace), bareModule, overrides, CopyContext.rootContext());
        rootTrace.complete(
            new FinishContext(this, (ElementResolver) repository, linkerOptions),
            new VerifyContext(this, linkerOptions)
        );
        return rootTrace;
    }

    RepositoryImpl createRepository(List<? extends BareBundle> bundles, LinkerOptions linkerOptions)
            throws LinkerException {
        bundles.forEach(Objects::requireNonNull);
        Objects.requireNonNull(bundles);
        Objects.requireNonNull(linkerOptions);

        RepositoryImpl repository = new RepositoryImpl(bundles, systemBundle, CopyContext.rootContext());
        repository.complete(
            new FinishContext(this, repository, linkerOptions),
            new VerifyContext(this, linkerOptions)
        );
        return repository;
    }

    CloudKeeperTypeReflection getTypes() {
        return typeReflection;
    }

    ImmutableList<SerializationDeclarationImpl> getDefaultSerializationDeclarations() {
        return defaultSerializationDeclarations;
    }

    @Override
    void collectEnclosed(Collection<AbstractFreezable> freezables) {
        freezables.add(systemBundle);
    }

    @Override
    void preProcessFreezable(FinishContext context) { }

    @Override
    void finishFreezable(FinishContext context) {
        typeReflection.complete(systemBundle);
    }

    @Override
    void verifyFreezable(VerifyContext context) { }

    private void complete() throws LinkerException {
        LinkerOptions linkerOptions = new LinkerOptions.Builder()
            .setClassProvider(name -> Optional.of(Class.forName(name.getBinaryName().toString())))
            .build();
        complete(
            new FinishContext(this, systemBundle, linkerOptions),
            new VerifyContext(this, linkerOptions)
        );
    }
}
