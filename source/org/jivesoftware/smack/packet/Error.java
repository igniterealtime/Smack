/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */

package org.jivesoftware.smack.packet;

/**
 * Represents a XMPP error subpacket. Typically, a server responds to a request that has
 * problems by sending the packet back and including an error packet. Each error has a code
 * as well as as an optional text explanation. Typical error codes are as follows:
 *
 * <table border=1>
 *      <tr><td><b>Code</b></td><td><b>Description</b></td></tr>
 *      <tr><td> 302 </td><td> Redirect </td></tr>
 *      <tr><td> 400 </td><td> Bad Request </td></tr>
 *      <tr><td> 401 </td><td> Unauthorized </td></tr>
 *      <tr><td> 402 </td><td> Payment Required </td></tr>
 *      <tr><td> 403 </td><td> Forbidden </td></tr>
 *      <tr><td> 404 </td><td> Not Found </td></tr>
 *      <tr><td> 405 </td><td> Not Allowed </td></tr>
 *      <tr><td> 406 </td><td> Not Acceptable </td></tr>
 *      <tr><td> 407 </td><td> Registration Required </td></tr>
 *      <tr><td> 408 </td><td> Request Timeout </td></tr>
 *      <tr><td> 409 </td><td> Conflict </td></tr>
 *      <tr><td> 500 </td><td> Internal Server Error </td></tr>
 *      <tr><td> 501 </td><td> Not Implemented </td></tr>
 *      <tr><td> 502 </td><td> Remote Server Error </td></tr>
 *      <tr><td> 503 </td><td> Service Unavailable </td></tr>
 *      <tr><td> 504 </td><td> Remote Server Timeout </td></tr>
 * </table>
 *
 * @author Matt Tucker
 */
public class Error {

    private int code;
    private String message;

    /**
     * Creates a new  error with the specified code and no message..
     *
     * @param code the error code.
     */
    public Error (int code) {
        this.code = code;
        this.message = null;
    }

    /**
     * Creates a new error with the specified code and message.
     *
     * @param code the error code.
     * @param message a message describing the error.
     */
    public Error(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Returns the error code.
     *
     * @return the error code.
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the message describing the error, or null if there is no message.
     *
     * @return the message describing the error, or null if there is no message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the error as XML.
     *
     * @return the error as XML.
     */
    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<error>");
        buf.append("<code>").append(code).append("</code>");
        if (message != null) {
            buf.append("<message>").append(message).append("</message>");
        }
        buf.append("</error>");
        return buf.toString();
    }
}
