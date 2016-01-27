package com.svbio.cloudkeeper.dsl.types;

import cloudkeeper.serialization.ByteSequenceMarshaler;
import cloudkeeper.types.ByteSequence;
import com.svbio.cloudkeeper.dsl.TypePlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@TypePlugin("Binary Sequence Alignment/Map format")
public class BAMFile implements ByteSequence {
    private final ByteSequence byteSequence;

    private enum BAMDecorator implements ByteSequenceMarshaler.Decorator {
        INSTANCE;

        @Override
        public ByteSequence decorate(ByteSequence byteSequence) {
            return new BAMFile(byteSequence);
        }
    }

    public BAMFile(ByteSequence byteSequence) {
        this.byteSequence = byteSequence;
    }

    @Override
    public ByteSequenceMarshaler.Decorator getDecorator() {
        return BAMDecorator.INSTANCE;
    }

    @Override
    public URI getURI() {
        return byteSequence.getURI();
    }

    @Override
    public boolean isSelfContained() {
        return byteSequence.isSelfContained();
    }

    @Override
    public long getContentLength() throws IOException {
        return byteSequence.getContentLength();
    }

    @Override
    public String getContentType() throws IOException {
        return "application/bam";
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return byteSequence.newInputStream();
    }
}
