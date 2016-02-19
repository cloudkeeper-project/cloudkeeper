package xyz.cloudkeeper.simple;

import org.testng.Assert;
import org.testng.annotations.Test;
import xyz.cloudkeeper.simple.CharacterStreamCommunication.Splitter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public class CharacterStreamCommunicationTest {
    @Test
    public void testWriteBoundary() throws IOException {
        StringBuilder stringBuilder = new StringBuilder(256);
        CharacterStreamCommunication.writeBoundary("abc", stringBuilder);
        Assert.assertEquals(stringBuilder.toString(), "X-Object-Boundary: abc\n");

        try {
            CharacterStreamCommunication.writeBoundary("foo ", stringBuilder);
            Assert.fail();
        } catch (IllegalArgumentException ignored) { }

        try {
            CharacterStreamCommunication.writeBoundary("garçon", stringBuilder);
            Assert.fail();
        } catch (IllegalArgumentException ignored) { }
    }

    @Test
    public void testRoundTrip() throws IOException {
        Integer value = 24;

        byte[] bytes;
        try (
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintWriter printWriter
                = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8.name()))
        ) {
            CharacterStreamCommunication.writeBoundary("foo", printWriter);
            printWriter.println("Look at this!");
            CharacterStreamCommunication.writeObject(value, printWriter, "foo");
            printWriter.println("Done!");
            printWriter.flush();
            bytes = outputStream.toByteArray();
        }

        try (
            Splitter<Integer> splitter = new Splitter<>(
                Integer.class,
                new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))
            )
        ) {
            Assert.assertEquals(splitter.readLine(), "Look at this!");
            Assert.assertEquals(splitter.readLine(), "Done!");
            Assert.assertEquals(splitter.readLine(), null);
            Assert.assertEquals(splitter.readLine(), null);
            Integer recovered = splitter.getResult();
            Assert.assertEquals(recovered, value);
        }
    }

    @Test
    public void testSplitterWithoutObject() throws IOException {
        boolean calledReadLine = false;
        try (Splitter<Integer> splitter
                 = new Splitter<>(Integer.class, new BufferedReader(new StringReader("Foo: bar\nb")))) {
            Assert.assertEquals(splitter.readLine(), "Foo: bar");
            Assert.assertEquals(splitter.readLine(), "b");
            Assert.assertEquals(splitter.readLine(), null);
            Assert.assertEquals(splitter.readLine(), null);
            calledReadLine = true;
            splitter.getResult();
            Assert.fail("Expected IOException");
        } catch (IOException exception) {
            Assert.assertTrue(exception.getMessage().toLowerCase().contains("reached end of stream"));
        }
        Assert.assertTrue(calledReadLine);
    }
}
