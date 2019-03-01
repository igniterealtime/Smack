/**
 *
 * Copyright 2018 Florian Schmaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smack.compression.zlib;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XmppInputOutputFilter;
import org.jivesoftware.smack.compression.XMPPInputOutputStream;
import org.jivesoftware.smack.compression.XMPPInputOutputStream.FlushMethod;
import org.jivesoftware.smack.compression.XmppCompressionFactory;

public final class ZlibXmppCompressionFactory extends XmppCompressionFactory {

    public static final ZlibXmppCompressionFactory INSTANCE = new ZlibXmppCompressionFactory();

    private ZlibXmppCompressionFactory() {
        super("zlib", 100);
    }

    @Override
    public XmppInputOutputFilter fabricate(ConnectionConfiguration configuration) {
        return new ZlibXmppInputOutputFilter();
    }

    private static final class ZlibXmppInputOutputFilter implements XmppInputOutputFilter {

        private static final int MINIMUM_OUTPUT_BUFFER_INITIAL_SIZE = 4;
        private static final int MINIMUM_OUTPUT_BUFFER_INCREASE = 480;

        private final Deflater compressor;
        private final Inflater decompressor = new Inflater();

        private long compressorInBytes;
        private long compressorOutBytes;

        private long decompressorInBytes;
        private long decompressorOutBytes;

        private int maxOutputOutput = -1;
        private int maxInputOutput = -1;

        private int maxBytesWrittenAfterFullFlush = -1;

        private ZlibXmppInputOutputFilter() {
            this(Deflater.DEFAULT_COMPRESSION);
        }

        private ZlibXmppInputOutputFilter(int compressionLevel) {
            compressor = new Deflater(compressionLevel);
        }

        private ByteBuffer outputBuffer;

        @Override
        public OutputResult output(ByteBuffer outputData, boolean isFinalDataOfElement, boolean destinationAddressChanged,
                        boolean moreDataAvailable) throws IOException {
            if (destinationAddressChanged && XMPPInputOutputStream.getFlushMethod() == FlushMethod.FULL_FLUSH) {
                outputBuffer = ByteBuffer.allocate(256);

                int bytesWritten = deflate(Deflater.FULL_FLUSH);

                maxBytesWrittenAfterFullFlush = Math.max(bytesWritten, maxBytesWrittenAfterFullFlush);
                compressorOutBytes += bytesWritten;
            }

            if (outputData == null && outputBuffer == null) {
                return OutputResult.NO_OUTPUT;
            }

            int bytesRemaining = outputData.remaining();
            if (outputBuffer == null) {
                final int outputBufferSize = bytesRemaining < MINIMUM_OUTPUT_BUFFER_INITIAL_SIZE ? MINIMUM_OUTPUT_BUFFER_INITIAL_SIZE : bytesRemaining;
                // We assume that the compressed data will not take more space as the uncompressed. Even if this is not
                // always true, the automatic buffer resize mechanism of deflate() will take care.
                outputBuffer = ByteBuffer.allocate(outputBufferSize);
            }

            // There is an invariant of Deflater/Inflater that input should only be set if needsInput() return true.
            assert (compressor.needsInput());

            final byte[] compressorInputBuffer;
            final int compressorInputBufferOffset, compressorInputBufferLength;
            if (outputData.hasArray()) {
                compressorInputBuffer = outputData.array();
                compressorInputBufferOffset = outputData.arrayOffset();
                compressorInputBufferLength = outputData.remaining();
            } else {
                compressorInputBuffer = new byte[outputData.remaining()];
                compressorInputBufferOffset = 0;
                compressorInputBufferLength = compressorInputBuffer.length;
                outputData.get(compressorInputBuffer);
            }

            compressorInBytes += compressorInputBufferLength;

            compressor.setInput(compressorInputBuffer, compressorInputBufferOffset, compressorInputBufferLength);

            int flushMode;
            if (moreDataAvailable) {
                flushMode = Deflater.NO_FLUSH;
            } else {
                flushMode = Deflater.SYNC_FLUSH;
            }

            int bytesWritten = deflate(flushMode);

            maxOutputOutput = Math.max(outputBuffer.position(), maxOutputOutput);
            compressorOutBytes += bytesWritten;

            OutputResult outputResult = new OutputResult(outputBuffer);
            outputBuffer = null;
            return outputResult;
        }

        private int deflate(int flushMode) {
//            compressor.finish();

            int totalBytesWritten = 0;
            while (true) {
                int initialOutputBufferPosition = outputBuffer.position();
                byte[] buffer = outputBuffer.array();
                int length = outputBuffer.limit() - initialOutputBufferPosition;

                int bytesWritten = compressor.deflate(buffer, initialOutputBufferPosition, length, flushMode);

                int newOutputBufferPosition = initialOutputBufferPosition + bytesWritten;
                outputBuffer.position(newOutputBufferPosition);

                totalBytesWritten += bytesWritten;

                if (compressor.needsInput() && outputBuffer.hasRemaining()) {
                    break;
                }

                int increasedBufferSize = outputBuffer.capacity() * 2;
                if (increasedBufferSize < MINIMUM_OUTPUT_BUFFER_INCREASE) {
                    increasedBufferSize = MINIMUM_OUTPUT_BUFFER_INCREASE;
                }
                ByteBuffer newCurrentOutputBuffer = ByteBuffer.allocate(increasedBufferSize);
                outputBuffer.flip();
                newCurrentOutputBuffer.put(outputBuffer);
                outputBuffer = newCurrentOutputBuffer;
            }

            return totalBytesWritten;
        }

        @Override
        public ByteBuffer input(ByteBuffer inputData) throws IOException {
            int bytesRemaining = inputData.remaining();

            final byte[] inputBytes;
            final int offset, length;
            if (inputData.hasArray()) {
                inputBytes = inputData.array();
                offset = inputData.arrayOffset();
                length = inputData.remaining();
            } else {
                // Copy since we are dealing with a buffer whose array is not accessible (possibly a direct buffer).
                inputBytes = new byte[bytesRemaining];
                inputData.get(inputBytes);
                offset = 0;
                length = inputBytes.length;
            }

            decompressorInBytes += length;

            decompressor.setInput(inputBytes, offset, length);

            int bytesInflated;
            // Assume that the inflated/decompressed result will be roughly at most twice the size of the compressed
            // variant. It appears to hold most of the times, if not, then the buffer resize mechanism will take care of
            // it.
            ByteBuffer outputBuffer = ByteBuffer.allocate(2 * length);
            while (true) {
                byte[] inflateOutputBuffer = outputBuffer.array();
                int inflateOutputBufferOffset = outputBuffer.position();
                int inflateOutputBufferLength = outputBuffer.limit() - inflateOutputBufferOffset;
                try {
                    bytesInflated = decompressor.inflate(inflateOutputBuffer, inflateOutputBufferOffset, inflateOutputBufferLength);
                }
                catch (DataFormatException e) {
                    throw new IOException(e);
                }

                outputBuffer.position(inflateOutputBufferOffset + bytesInflated);

                decompressorOutBytes += bytesInflated;

                if (decompressor.needsInput()) {
                    break;
                }

                int increasedBufferSize = outputBuffer.capacity() * 2;
                ByteBuffer increasedOutputBuffer = ByteBuffer.allocate(increasedBufferSize);
                outputBuffer.flip();
                increasedOutputBuffer.put(outputBuffer);
                outputBuffer = increasedOutputBuffer;
            }

            if (bytesInflated == 0) {
                return null;
            }

            maxInputOutput = Math.max(outputBuffer.position(), maxInputOutput);

            return outputBuffer;
        }

        @Override
        public Stats getStats() {
            return new Stats(this);
        }
    }

    public static final class Stats {
        public final long compressorInBytes;
        public final long compressorOutBytes;
        public final double compressionRatio;

        public final long decompressorInBytes;
        public final long decompressorOutBytes;
        public final double decompressionRatio;

        public final int maxOutputOutput;
        public final int maxInputOutput;

        public final int maxBytesWrittenAfterFullFlush;

        private Stats(ZlibXmppInputOutputFilter filter) {
            // Note that we read the out bytes before the in bytes to not over approximate the compression ratio.
            compressorOutBytes = filter.compressorOutBytes;
            compressorInBytes = filter.compressorInBytes;
            compressionRatio = (double) compressorOutBytes / compressorInBytes;

            decompressorOutBytes = filter.decompressorOutBytes;
            decompressorInBytes = filter.decompressorInBytes;
            decompressionRatio = (double) decompressorInBytes / decompressorOutBytes;

            maxOutputOutput = filter.maxOutputOutput;
            maxInputOutput = filter.maxInputOutput;
            maxBytesWrittenAfterFullFlush = filter.maxBytesWrittenAfterFullFlush;
        }

        private transient String toStringCache;

        @Override
        public String toString() {
            if (toStringCache != null) {
                return toStringCache;
            }

            toStringCache =
                "compressor-in-bytes: "  + compressorInBytes + '\n'
              + "compressor-out-bytes: " + compressorOutBytes + '\n'
              + "compression-ratio: " + compressionRatio + '\n'
              + "decompressor-in-bytes: " + decompressorInBytes + '\n'
              + "decompressor-out-bytes: " + decompressorOutBytes + '\n'
              + "decompression-ratio: " + decompressionRatio + '\n'
              + "max-output-output: " + maxOutputOutput + '\n'
              + "max-input-output: " + maxInputOutput + '\n'
              + "max-bytes-written-after-full-flush: " + maxBytesWrittenAfterFullFlush + '\n'
              ;

            return toStringCache;
        }
    }
}
