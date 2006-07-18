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

package org.jivesoftware.smack.packet;

/**
 * Represents a XMPP error sub-packet. Typically, a server responds to a request that has
 * problems by sending the packet back and including an error packet. Each error has a code
 * as well as as an optional text explanation. Typical error codes are as follows:<p>
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
 *      <tr><td> 500 </td><td> Internal Server XMPPError </td></tr>
 *      <tr><td> 501 </td><td> Not Implemented </td></tr>
 *      <tr><td> 502 </td><td> Remote Server Error </td></tr>
 *      <tr><td> 503 </td><td> Service Unavailable </td></tr>
 *      <tr><td> 504 </td><td> Remote Server Timeout </td></tr>
 * </table>
 *
 * @author Matt Tucker
 */
public class XMPPError {

    private int code;
    private String message;

    /**
     * Creates a new  error with the specified code and no message..
     *
     * @param code the error code.
     */
    public XMPPError(int code) {
        this.code = code;
        this.message = null;
    }

    /**
     * Creates a new error with the specified code and message.
     *
     * @param code the error code.
     * @param message a message describing the error.
     */
    public XMPPError(int code, String message) {
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
        StringBuilder buf = new StringBuilder();
        buf.append("<error code=\"").append(code).append("\">");
        if (message != null) {
            buf.append(message);
        }
        buf.append("</error>");
        return buf.toString();
    }

    public String toString() {
        StringBuilder txt = new StringBuilder();
        txt.append("(").append(code).append(")");
        if (message != null) {
            txt.append(" ").append(message);
        }
        return txt.toString();
    }
}
