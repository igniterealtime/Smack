/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2004 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

import org.jivesoftware.smack.packet.XMPPError;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * A generic exception that is thrown when an error occurs performing an
 * XMPP operation. XMPP servers can respond to error conditions with an error code
 * and textual description of the problem, which are encapsulated in the XMPPError
 * class. When appropriate, an XMPPError instance is attached instances of this exception.
 *
 * @see XMPPError
 * @author Matt Tucker
 */
public class XMPPException extends Exception {

    private XMPPError error = null;
    private Throwable wrappedThrowable = null;

    /**
     * Creates a new XMPPException.
     */
    public XMPPException() {
        super();
    }

    /**
     * Creates a new XMPPException with a description of the exception.
     *
     * @param message description of the exception.
     */
    public XMPPException(String message) {
        super(message);
    }

    /**
     * Creates a new XMPPException with the Throwable that was the root cause of the
     * exception.
     *
     * @param wrappedThrowable the root cause of the exception.
     */
    public XMPPException(Throwable wrappedThrowable) {
        super();
        this.wrappedThrowable = wrappedThrowable;
    }

    /**
     * Cretaes a new XMPPException with the XMPPError that was the root case of the
     * exception.
     *
     * @param error the root cause of the exception.
     */
    public XMPPException(XMPPError error) {
        super();
        this.error = error;
    }

    /**
     * Creates a new XMPPException with a description of the exception and the
     * Throwable that was the root cause of the exception.
     *
     * @param message a description of the exception.
     * @param wrappedThrowable the root cause of the exception.
     */
    public XMPPException(String message, Throwable wrappedThrowable) {
        super(message);
        this.wrappedThrowable = wrappedThrowable;
    }

    /**
     * Creates a new XMPPException with a description of the exception, an XMPPError,
     * and the Throwable that was the root cause of the exception.
     *
     * @param message a description of the exception.
     * @param error the root cause of the exception.
     * @param wrappedThrowable the root cause of the exception.
     */
    public XMPPException(String message, XMPPError error, Throwable wrappedThrowable) {
        super(message);
        this.error = error;
        this.wrappedThrowable = wrappedThrowable;
    }

    /**
     * Creates a new XMPPException with a description of the exception and the
     * XMPPException that was the root cause of the exception.
     *
     * @param message a description of the exception.
     * @param error the root cause of the exception.
     */
    public XMPPException(String message, XMPPError error) {
        super(message);
        this.error = error;
    }

    /**
     * Returns the XMPPError asscociated with this exception, or <tt>null</tt> if there
     * isn't one.
     *
     * @return the XMPPError asscociated with this exception.
     */
    public XMPPError getXMPPError() {
        return error;
    }

    /**
     * Returns the Throwable asscociated with this exception, or <tt>null</tt> if there
     * isn't one.
     *
     * @return the Throwable asscociated with this exception.
     */
    public Throwable getWrappedThrowable() {
        return wrappedThrowable;
    }

    public void printStackTrace() {
        printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream out) {
        super.printStackTrace(out);
        if (wrappedThrowable != null) {
            out.println("Nested Exception: ");
            wrappedThrowable.printStackTrace(out);
        }
    }

    public void printStackTrace(PrintWriter out) {
        super.printStackTrace(out);
        if (wrappedThrowable != null) {
            out.println("Nested Exception: ");
            wrappedThrowable.printStackTrace(out);
        }
    }

    public String getMessage() {
        String msg = super.getMessage();
        // If the message was not set, but there is an XMPPError, return the
        // XMPPError as the message.
        if (msg == null && error != null) {
            return error.toString();
        }
        return msg;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        String message = super.getMessage();
        if (message != null) {
            buf.append(message).append(": ");
        }
        if (error != null) {
            buf.append(error);
        }
        if (wrappedThrowable != null) {
            buf.append("\n  -- caused by: ").append(wrappedThrowable);
        }

        return buf.toString();
    }
}