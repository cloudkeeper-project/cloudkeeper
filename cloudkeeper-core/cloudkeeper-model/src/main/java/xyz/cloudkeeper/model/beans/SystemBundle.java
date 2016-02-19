package xyz.cloudkeeper.model.beans;

import cloudkeeper.annotations.CloudKeeperElementReference;
import cloudkeeper.annotations.CloudKeeperSerialization;
import cloudkeeper.serialization.ByteSequenceMarshaler;
import cloudkeeper.serialization.CollectionMarshaler;
import cloudkeeper.serialization.IntegerMarshaler;
import cloudkeeper.serialization.SerializableMarshaler;
import cloudkeeper.serialization.StringMarshaler;
import cloudkeeper.types.ByteSequence;
import xyz.cloudkeeper.model.ModelEquivalent;
import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.beans.element.MutableBundle;
import xyz.cloudkeeper.model.beans.element.MutablePackage;
import xyz.cloudkeeper.model.beans.element.MutablePluginDeclaration;
import xyz.cloudkeeper.model.beans.element.annotation.MutableAnnotation;
import xyz.cloudkeeper.model.beans.element.annotation.MutableAnnotationEntry;
import xyz.cloudkeeper.model.beans.element.annotation.MutableAnnotationTypeDeclaration;
import xyz.cloudkeeper.model.beans.element.serialization.MutableSerializationDeclaration;
import xyz.cloudkeeper.model.beans.element.type.MutableTypeDeclaration;
import xyz.cloudkeeper.model.util.BuildInformation;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * CloudKeeper system-bundle factory, providing a static factory method that returns a bare system bundle.
 *
 * <p>The system bundle contains built-in CloudKeeper plug-in declarations, and it is implicitly contained in every
 * CloudKeeper repository. This class provides the static factory method {@link #newSystemBundle()} that returns a bare
 * (not linked) copy of the system bundle.
 */
public final class SystemBundle {
    private SystemBundle() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Bundle identifier of the system bundle. It is always of form {@code x-ck-system-bundle:<version>} where
     * {@code <version>} contains {@link BuildInformation#PROJECT_VERSION}.
     */
    public static final URI SYSTEM_BUNDLE_IDENTIFIER
        = URI.create("x-ck-system-bundle:" + BuildInformation.PROJECT_VERSION);

    private static MutableAnnotation serialization(Class<? extends Marshaler<?>> clazz) {
        return new MutableAnnotation()
            .setDeclaration(CloudKeeperSerialization.class.getName())
            .setEntries(Collections.singletonList(
                new MutableAnnotationEntry()
                    .setKey("value")
                    .setValue(new String[] {
                        clazz.getName()
                    })
            ));
    }

    private enum CopyOptions implements AnnotationCopyOption, IncludeNestedCopyOption {
        INSTANCE;

        @Override
        public boolean isCloudKeeperAnnotation(Annotation annotation, AnnotatedElement annotatedElement) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            return annotationType.isAnnotationPresent(ModelEquivalent.class)
                || annotationType.getName().startsWith("cloudkeeper.");
        }

        @Override
        public boolean shouldInclude(Class<?> clazz) {
            return false;
        }
    }

    /**
     * Returns a new mutable (not linked) system repository.
     */
    public static MutableBundle newSystemBundle() {
        CopyOption[] copyOptions = {CopyOptions.INSTANCE};
        return new MutableBundle()
            .setBundleIdentifier(SYSTEM_BUNDLE_IDENTIFIER)
            .setPackages(Arrays.asList(
                new MutablePackage()
                    .setQualifiedName("cloudkeeper.annotations")
                    .setDeclarations(Arrays.<MutablePluginDeclaration<?>>asList(
                        MutableAnnotationTypeDeclaration.fromClass(CloudKeeperElementReference.class, copyOptions),
                        MutableAnnotationTypeDeclaration.fromClass(CloudKeeperSerialization.class, copyOptions)
                    )),
                new MutablePackage()
                    .setQualifiedName("java.lang")
                    .setDeclarations(Arrays.<MutablePluginDeclaration<?>>asList(
                        // Type declarations
                        // Classes necessary for the type system (Cloneable is a direct supertype of any array type, see
                        // JLS §4.10.3)
                        MutableTypeDeclaration.fromClass(Object.class, copyOptions),
                        MutableTypeDeclaration.fromClass(Cloneable.class, copyOptions),
                        MutableTypeDeclaration.fromClass(Enum.class, copyOptions),

                        // Boxed types (referenced by JLS §5.1.7 "Boxing Conversion")
                        MutableTypeDeclaration.fromClass(Boolean.class, copyOptions),
                        MutableTypeDeclaration.fromClass(Character.class, copyOptions),
                        MutableTypeDeclaration.fromClass(Byte.class, copyOptions),
                        MutableTypeDeclaration.fromClass(Short.class, copyOptions),
                        MutableTypeDeclaration.fromClass(Integer.class, copyOptions)
                            .setDeclaredAnnotations(Collections.singletonList(
                                serialization(IntegerMarshaler.class)
                            )),
                        MutableTypeDeclaration.fromClass(Long.class, copyOptions),
                        MutableTypeDeclaration.fromClass(Float.class, copyOptions),
                        MutableTypeDeclaration.fromClass(Double.class, copyOptions),

                        // Other elementary classes and interfaces
                        MutableTypeDeclaration.fromClass(CharSequence.class, copyOptions),
                        MutableTypeDeclaration.fromClass(Comparable.class, copyOptions),
                        MutableTypeDeclaration.fromClass(Iterable.class, copyOptions),
                        MutableTypeDeclaration.fromClass(Number.class, copyOptions),
                        MutableTypeDeclaration.fromClass(String.class, copyOptions)
                            .setDeclaredAnnotations(Collections.singletonList(
                                serialization(StringMarshaler.class)
                            ))
                    )),
                new MutablePackage()
                    .setQualifiedName("java.io")
                    .setDeclarations(Collections.<MutablePluginDeclaration<?>>singletonList(
                        // Necessary for the type system (Serializable is a direct supertype of any array type, see
                        // JLS §4.10.3)
                        MutableTypeDeclaration.fromClass(Serializable.class, copyOptions)
                    )),
                new MutablePackage()
                    .setQualifiedName("java.util")
                    .setDeclarations(Collections.<MutablePluginDeclaration<?>>singletonList(
                        MutableTypeDeclaration.fromClass(Collection.class, copyOptions)
                    )),
                new MutablePackage()
                    .setQualifiedName("cloudkeeper.types")
                    .setDeclarations(Collections.<MutablePluginDeclaration<?>>singletonList(
                        MutableTypeDeclaration.fromClass(ByteSequence.class, copyOptions)
                    )),
                new MutablePackage()
                    .setQualifiedName("cloudkeeper.serialization")
                    .setDeclarations(Arrays.<MutablePluginDeclaration<?>>asList(
                        MutableSerializationDeclaration.fromClass(CollectionMarshaler.class, copyOptions),
                        MutableSerializationDeclaration.fromClass(IntegerMarshaler.class, copyOptions),
                        MutableSerializationDeclaration.fromClass(SerializableMarshaler.class, copyOptions),
                        MutableSerializationDeclaration.fromClass(StringMarshaler.class, copyOptions),
                        MutableSerializationDeclaration.fromClass(ByteSequenceMarshaler.class, copyOptions)
                    ))
            ));
    }
}
