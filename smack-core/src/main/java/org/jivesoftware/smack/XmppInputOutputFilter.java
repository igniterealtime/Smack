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
package org.jivesoftware.smack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;

import org.jivesoftware.smack.SmackException.NoResponseException;

public interface XmppInputOutputFilter {

    /**
     * The {@code outputData} argument may be a direct {@link ByteBuffer}. The filter has consume the data of the buffer
     * completely.
     *
     * This method must return a {@link OutputResult}. Use {@link OutputResult#NO_OUTPUT} if there is no output.
     *
     * @param outputData the data this method needs to process.
     * @param isFinalDataOfElement if this is the final data of the element.
     * @param destinationAddressChanged if the destination address has changed.
     * @param moreDataAvailable if more data is available.
     * @return a output result.
     * @throws IOException in case an I/O exception occurs.
     */
    OutputResult output(ByteBuffer outputData, boolean isFinalDataOfElement, boolean destinationAddressChanged,
                    boolean moreDataAvailable) throws IOException;

    class OutputResult {
        public static final OutputResult NO_OUTPUT = new OutputResult(false, null);

        public final boolean pendingFilterData;
        public final ByteBuffer filteredOutputData;

        public OutputResult(ByteBuffer filteredOutputData) {
            this(false, filteredOutputData);
        }

        public OutputResult(boolean pendingFilterData, ByteBuffer filteredOutputData) {
            this.pendingFilterData = pendingFilterData;
            this.filteredOutputData = filteredOutputData;
        }
    }

    /**
     * The returned {@link ByteBuffer} is going to get fliped by the caller. The callee must not flip the buffer.
     * @param inputData the data this methods needs to process.
     * @return a {@link ByteBuffer} or {@code null} if no data could be produced.
     * @throws IOException in case an I/O exception occurs.
     */
    ByteBuffer input(ByteBuffer inputData) throws IOException;

    default void closeInputOutput() {
    }

    default void waitUntilInputOutputClosed() throws IOException, NoResponseException, CertificateException, InterruptedException, SmackException {
    }

    default Object getStats() {
        return null;
    }
}
