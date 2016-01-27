package cloudkeeper.serialization;

import cloudkeeper.types.ByteSequence;
import com.svbio.cloudkeeper.model.CloudKeeperSerialization;
import com.svbio.cloudkeeper.model.api.MarshalContext;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.api.MarshalingException;
import com.svbio.cloudkeeper.model.api.UnmarshalContext;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * CloudKeeper serialization class for byte sequences, that is, {@link ByteSequence} and subclasses.
 *
 * <p>This class requires that the {@link ByteSequence#getDecorator()} is properly implemented, so that the actual
 * type of the byte sequence can be recreated during deserialization.
 *
 * <p>This serialization plug-in serializes {@link ByteSequence#getDecorator()} into stream {@code "decorator"} (using
 * Java serialization) and it writes the raw byte sequence into stream {@code "content"}.
 *
 * @see ByteSequence
 */
@CloudKeeperSerialization(StringMarshaler.class)
public final class ByteSequenceMarshaler implements Marshaler<ByteSequence> {
    private static final SimpleName DECORATOR = SimpleName.identifier("decorator");
    private static final SimpleName CONTENT = SimpleName.identifier("content");

    @Override
    public boolean isImmutable(ByteSequence object) {
        return object.isSelfContained();
    }

    private static void requireValidDecoratorClass(Class<?> decoratorClass) throws MarshalingException {
        if (!Decorator.class.isAssignableFrom(decoratorClass) || !decoratorClass.isEnum()
                || decoratorClass.getEnumConstants().length != 1) {
            throw new MarshalingException(String.format(
                "Expected singleton enum implementing %s, but got %s.", Decorator.class, decoratorClass
            ));
        }
    }

    @Override
    public void put(ByteSequence byteSequence, MarshalContext context) throws IOException {
        try (Writer writer = new OutputStreamWriter(context.newOutputStream(DECORATOR), StandardCharsets.UTF_8)) {
            Class<?> decoratorClass = byteSequence.getDecorator().getClass();
            requireValidDecoratorClass(decoratorClass);
            writer.write(decoratorClass.getName());
            writer.write(System.lineSeparator());
        }

        context.putByteSequence(byteSequence, CONTENT);
    }

    @Override
    public ByteSequence get(UnmarshalContext context) throws IOException {
        ByteSequence decoratorByteSequence = context.getByteSequence(DECORATOR);
        @Nullable String decoratorClassName = null;
        @Nullable Class<?> decoratorClass;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(decoratorByteSequence.newInputStream(), StandardCharsets.UTF_8))) {
            decoratorClassName = reader.readLine();
            if (decoratorClassName == null) {
                throw new MarshalingException("Empty decorator class name.");
            }

            decoratorClass = Class.forName(decoratorClassName, true, context.getClassLoader());
            requireValidDecoratorClass(decoratorClass);

            Decorator decorator = (Decorator) decoratorClass.getEnumConstants()[0];
            return decorator.decorate(context.getByteSequence(CONTENT));
        } catch (ClassNotFoundException exception) {
            throw new MarshalingException(String.format(
                "Could not load decorator class '%s'.", decoratorClassName
            ), exception);
        }
    }

    /**
     * Decorator that takes a raw byte sequence and returns an appropriately typed instance of a subclass.
     *
     * <p>This interface is expected to be <em>implemented by</em> singleton {@code enum}s only.
     */
    public interface Decorator {
        /**
         * Returns a decorated (typed) byte sequence representing the given "raw" byte sequence.
         *
         * @param byteSequence the "raw" byte sequence
         * @return the decorated (type) byte sequence
         */
        ByteSequence decorate(ByteSequence byteSequence);
    }

    /**
     * Identity decorator that, given a byte sequence, simply returns the argument itself.
     */
    private enum NoDecorator implements Decorator {
        INSTANCE;

        @Override
        public ByteSequence decorate(ByteSequence byteSequence) {
            return byteSequence;
        }
    }

    /**
     * Returns an identity decorator that, given a byte sequence, simply returns the argument itself.
     */
    public static Decorator noDecorator() {
        return NoDecorator.INSTANCE;
    }
}
