/**
 *
 * Copyright 2003-2007 Jive Software.
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

import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.packet.XMPPError;

/**
 * A generic exception that is thrown when an error occurs performing an
 * XMPP operation. XMPP servers can respond to error conditions with an error code
 * and textual description of the problem, which are encapsulated in the XMPPError
 * class. When appropriate, an XMPPError instance is attached instances of this exception.<p>
 *
 * When a stream error occurred, the server will send a stream error to the client before
 * closing the connection. Stream errors are unrecoverable errors. When a stream error
 * is sent to the client an XMPPException will be thrown containing the StreamError sent
 * by the server.
 *
 * @see XMPPError
 * @author Matt Tucker
 */
public abstract class XMPPException extends Exception {
    private static final long serialVersionUID = 6881651633890968625L;
    

    /**
     * Creates a new XMPPException.
     */
    protected XMPPException() {
        super();
    }

    /**
     * Creates a new XMPPException with a description of the exception.
     *
     * @param message description of the exception.
     */
    protected XMPPException(String message) {
        super(message);
    }


    /**
     * Creates a new XMPPException with a description of the exception and the
     * Throwable that was the root cause of the exception.
     *
     * @param message a description of the exception.
     * @param wrappedThrowable the root cause of the exception.
     */
    protected XMPPException(String message, Throwable wrappedThrowable) {
        super(message, wrappedThrowable);
    }

    public static class XMPPErrorException extends XMPPException {
        /**
         * 
         */
        private static final long serialVersionUID = 212790389529249604L;
        private final XMPPError error;

        /**
         * Creates a new XMPPException with the XMPPError that was the root case of the exception.
         * 
         * @param error the root cause of the exception.
         */
        public XMPPErrorException(XMPPError error) {
            super();
            this.error = error;
        }

        /**
         * Creates a new XMPPException with a description of the exception, an XMPPError, and the
         * Throwable that was the root cause of the exception.
         * 
         * @param message a description of the exception.
         * @param error the root cause of the exception.
         * @param wrappedThrowable the root cause of the exception.
         */
        public XMPPErrorException(String message, XMPPError error, Throwable wrappedThrowable) {
            super(message, wrappedThrowable);
            this.error = error;
        }

        /**
         * Creates a new XMPPException with a description of the exception and the XMPPException
         * that was the root cause of the exception.
         * 
         * @param message a description of the exception.
         * @param error the root cause of the exception.
         */
        public XMPPErrorException(String message, XMPPError error) {
            super(message);
            this.error = error;
        }

        /**
         * Returns the XMPPError associated with this exception, or <tt>null</tt> if there isn't
         * one.
         * 
         * @return the XMPPError associated with this exception.
         */
        public XMPPError getXMPPError() {
            return error;
        }

        @Override
        public String getMessage() {
            String superMessage = super.getMessage();
            if (superMessage != null) {
                return superMessage;
            }
            else {
                return error.toString();
            }
        }

        @Override
        public String toString() {
            return getMessage();
        }
    }

    public static class StreamErrorException extends XMPPException {
        /**
         * 
         */
        private static final long serialVersionUID = 3400556867134848886L;
        private final StreamError streamError;

        /**
         * Creates a new XMPPException with the stream error that was the root case of the
         * exception. When a stream error is received from the server then the underlying connection
         * will be closed by the server.
         * 
         * @param streamError the root cause of the exception.
         */
        public StreamErrorException(StreamError streamError) {
            super();
            this.streamError = streamError;
        }

        /**
         * Returns the StreamError associated with this exception. The underlying TCP connection is
         * closed by the server after sending the stream error to the client.
         * 
         * @return the StreamError associated with this exception.
         */
        public StreamError getStreamError() {
            return streamError;
        }

        @Override
        public String getMessage() {
            return streamError.toString();
        }

        @Override
        public String toString() {
            return getMessage();
        }
    }
}
