/**
 *
 * Copyright 2022 Micha Kurvers
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
package org.jivesoftware.smackx.httpfileupload;

import java.io.IOException;
import java.net.URL;

import org.jivesoftware.smackx.httpfileupload.element.Slot;
/**
 * An exception class to provide additional information in case of exceptions during file uploading.
 *
 */
public abstract class AbstractHttpUploadException extends IOException {

    private static final long serialVersionUID = 1L;
    private final long fileSize;
    private final Slot slot;

    protected AbstractHttpUploadException(long fileSize, Slot slot, String message) {
        this(fileSize, slot, message, null);
    }

    protected AbstractHttpUploadException(long fileSize, Slot slot, String message, Throwable wrappedThrowable) {
        super(message, wrappedThrowable);
        this.fileSize = fileSize;
        this.slot = slot;
    }

    public long getFileSize() {
        return fileSize;
    }

    public URL getPutUrl() {
        return slot.getPutUrl();
    }

    public Slot getSlot() {
        return slot;
    }

    /**
     * Exception thrown when http response returned after upload is not 200.
     */
    public static class HttpUploadErrorException extends AbstractHttpUploadException {

        private static final long serialVersionUID = 8494356028399474995L;
        private final int httpStatus;
        private final String responseMsg;

        public HttpUploadErrorException(int httpStatus, String responseMsg, long fileSize, Slot slot) {
            super(fileSize, slot, "Error response " + httpStatus + " from server during file upload: "
                            + responseMsg + ", file size: " + fileSize + ", put URL: "
                            + slot.getPutUrl());
            this.httpStatus = httpStatus;
            this.responseMsg = responseMsg;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public String getResponseMsg() {
            return responseMsg;
        }

    }

    /**
     * Exception thrown when an unexpected exception occurred during the upload.
     */
    public static class HttpUploadIOException extends AbstractHttpUploadException {

        private static final long serialVersionUID = 5940866318073349451L;
        private final IOException wrappedIOException;

        public HttpUploadIOException(long fileSize, Slot slot, IOException cause) {
            super(fileSize, slot, "Unexpected error occurred during file upload, file size: " + fileSize
                            + ", put Url: " + slot.getPutUrl(), cause);
            this.wrappedIOException = cause;
        }

        public IOException getCausingIOException() {
            return this.wrappedIOException;
        }

    }
}
