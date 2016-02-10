package cloudkeeper.serialization;

import com.svbio.cloudkeeper.model.api.MarshalContext;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.api.UnmarshalContext;
import com.svbio.cloudkeeper.model.immutable.element.NoKey;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * CloudKeeper serialization for {@link String} objects.
 *
 * <p>This serialization implementation does not write out any explicit type information -- instead, it relies on the
 * fact that serialization contexts need to preserve which {@link Marshaler} class was used for serialization. This
 * is enough because {@link String} is a final class.
 */
public final class StringMarshaler implements Marshaler<String> {
    private static final int BUFFER_SIZE = 2048;

    @Override
    public boolean isImmutable(String object) {
        return true;
    }

    @Override
    public void put(String string, MarshalContext context) throws IOException {
        try (OutputStream outputStream = context.newOutputStream(NoKey.instance())) {
            outputStream.write(string.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public String get(UnmarshalContext context) throws IOException {
        StringBuilder stringBuilder = new StringBuilder(BUFFER_SIZE);
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream inputStream = context.getByteSequence(NoKey.instance()).newInputStream()) {
            for (int readCount = inputStream.read(buffer); readCount > 0; readCount = inputStream.read(buffer)) {
                stringBuilder.append(new String(buffer, 0, readCount, StandardCharsets.UTF_8));
            }
        }
        return stringBuilder.toString();
    }
}
