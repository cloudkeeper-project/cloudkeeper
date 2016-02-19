package xyz.cloudkeeper.model.util;

import cloudkeeper.serialization.ByteSequenceMarshaler;
import cloudkeeper.types.ByteSequence;
import org.testng.Assert;
import org.testng.ITest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteSequencesTest {
    private interface ByteSequenceProvider {
        ByteSequence getByteSequence(byte[] content);
    }

    public static final class ByteSequenceContract implements ITest {
        private final String name;
        private final ByteSequenceProvider provider;

        public ByteSequenceContract(String name, ByteSequenceProvider provider) {
            assert name != null && provider != null;
            this.name = name;
            this.provider = provider;
        }

        @Override
        public String getTestName() {
            return name;
        }

        @Test
        public void tenBytes() throws IOException {
            byte[] array = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            ByteSequence byteSequence = provider.getByteSequence(array);

            Assert.assertSame(byteSequence.getDecorator(), ByteSequenceMarshaler.noDecorator());
            Assert.assertEquals(byteSequence.getContentType(), ByteSequence.DEFAULT_CONTENT_TYPE);
            Assert.assertEquals(byteSequence.getContentLength(), array.length);

            // Verify that we can read more than once from the byte sequence
            for (int i = 0; i < 2; ++i) {
                byte[] readBuffer = new byte[array.length + 1];
                try (InputStream inputStream = byteSequence.newInputStream()) {
                    int readLength = inputStream.read(readBuffer);
                    Assert.assertEquals(readLength, array.length);
                    Assert.assertEquals(ByteBuffer.wrap(readBuffer, 0, readLength), ByteBuffer.wrap(array));
                }
            }
        }
    }

    @Factory
    public Object[] byteSequences() {
        return new Object[] {
            new ByteSequenceContract("arrayBacked", new ByteSequenceProvider() {
                @Override
                public ByteSequence getByteSequence(byte[] content) {
                    return ByteSequences.arrayBacked(content);
                }
            })
        };
    }
}
