package com.svbio.cloudkeeper.samples;

import cloudkeeper.types.ByteSequence;

/**
 * Result of a {@link FileManipulationFuture}.
 */
public final class FileManipulationResult {
    private final ByteSequence byteSequence;
    private final int numberOfLines;
    private final long sizeOfPrependedFile;

    public FileManipulationResult(ByteSequence byteSequence, int numberOfLines, long sizeOfPrependedFile) {
        this.byteSequence = byteSequence;
        this.numberOfLines = numberOfLines;
        this.sizeOfPrependedFile = sizeOfPrependedFile;
    }

    public ByteSequence getByteSequence() {
        return byteSequence;
    }

    public int getNumberOfLines() {
        return numberOfLines;
    }

    public long getSizeOfPrependedFile() {
        return sizeOfPrependedFile;
    }
}
