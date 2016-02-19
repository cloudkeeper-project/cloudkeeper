package xyz.cloudkeeper.model.api;

import cloudkeeper.types.ByteSequence;
import xyz.cloudkeeper.model.immutable.element.Key;

import java.io.IOException;

/**
 * Signals that an object cannot be marshaled or unmarshaled because of an incorrect format or implementation errors in
 * user-defined code.
 *
 * <p>For instance, this exception is thrown if user-defined code calls
 * {@link MarshalContext#putByteSequence(ByteSequence, Key)} more than once with the same key. This exception is also
 * meant to be thrown by user-defined code itself if any inconsistency is detected.
 */
public final class MarshalingException extends IOException {
    private static final long serialVersionUID = -4222928487358000951L;

    public MarshalingException(String message) {
        super(message);
    }

    public MarshalingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MarshalingException(Throwable cause) {
        super(cause);
    }
}
