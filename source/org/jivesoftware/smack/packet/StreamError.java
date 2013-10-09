/**
 * $Revision: 2408 $
 * $Date: 2004-11-02 20:53:30 -0300 (Tue, 02 Nov 2004) $
 *
 * Copyright 2003-2005 Jive Software.
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
 * Represents a stream error packet. Stream errors are unrecoverable errors where the server
 * will close the unrelying TCP connection after the stream error was sent to the client.
 * These is the list of stream errors as defined in the XMPP spec:<p>
 *
 * <table border=1>
 *      <tr><td><b>Code</b></td><td><b>Description</b></td></tr>
 *      <tr><td> bad-format </td><td> the entity has sent XML that cannot be processed </td></tr>
 *      <tr><td> unsupported-encoding </td><td>  the entity has sent a namespace prefix that is
 *          unsupported </td></tr>
 *      <tr><td> bad-namespace-prefix </td><td> Remote Server Timeout </td></tr>
 *      <tr><td> conflict </td><td> the server is closing the active stream for this entity
 *          because a new stream has been initiated that conflicts with the existing
 *          stream. </td></tr>
 *      <tr><td> connection-timeout </td><td> the entity has not generated any traffic over
 *          the stream for some period of time. </td></tr>
 *      <tr><td> host-gone </td><td> the value of the 'to' attribute provided by the initiating
 *          entity in the stream header corresponds to a hostname that is no longer hosted by
 *          the server. </td></tr>
 *      <tr><td> host-unknown </td><td> the value of the 'to' attribute provided by the
 *          initiating entity in the stream header does not correspond to a hostname that is
 *          hosted by the server. </td></tr>
 *      <tr><td> improper-addressing </td><td> a stanza sent between two servers lacks a 'to'
 *          or 'from' attribute </td></tr>
 *      <tr><td> internal-server-error </td><td> the server has experienced a
 *          misconfiguration. </td></tr>
 *      <tr><td> invalid-from </td><td> the JID or hostname provided in a 'from' address does
 *          not match an authorized JID. </td></tr>
 *      <tr><td> invalid-id </td><td> the stream ID or dialback ID is invalid or does not match
 *          an ID previously provided. </td></tr>
 *      <tr><td> invalid-namespace </td><td> the streams namespace name is invalid. </td></tr>
 *      <tr><td> invalid-xml </td><td> the entity has sent invalid XML over the stream. </td></tr>
 *      <tr><td> not-authorized </td><td> the entity has attempted to send data before the
 *          stream has been authenticated </td></tr>
 *      <tr><td> policy-violation </td><td> the entity has violated some local service
 *          policy. </td></tr>
 *      <tr><td> remote-connection-failed </td><td> Rthe server is unable to properly connect
 *          to a remote entity. </td></tr>
 *      <tr><td> resource-constraint </td><td> Rthe server lacks the system resources necessary
 *          to service the stream. </td></tr>
 *      <tr><td> restricted-xml </td><td> the entity has attempted to send restricted XML
 *          features. </td></tr>
 *      <tr><td> see-other-host </td><td>  the server will not provide service to the initiating
 *          entity but is redirecting traffic to another host. </td></tr>
 *      <tr><td> system-shutdown </td><td> the server is being shut down and all active streams
 *          are being closed. </td></tr>
 *      <tr><td> undefined-condition </td><td> the error condition is not one of those defined
 *          by the other conditions in this list. </td></tr>
 *      <tr><td> unsupported-encoding </td><td> the initiating entity has encoded the stream in
 *          an encoding that is not supported. </td></tr>
 *      <tr><td> unsupported-stanza-type </td><td> the initiating entity has sent a first-level
 *          child of the stream that is not supported. </td></tr>
 *      <tr><td> unsupported-version </td><td> the value of the 'version' attribute provided by
 *          the initiating entity in the stream header specifies a version of XMPP that is not
 *          supported. </td></tr>
 *      <tr><td> xml-not-well-formed </td><td> the initiating entity has sent XML that is
 *          not well-formed. </td></tr>
 * </table>
 *
 * @author Gaston Dombiak
 */
public class StreamError {

    public static final String NAMESPACE = "urn:ietf:params:xml:ns:xmpp-streams";

    private String code;
    private String text;

    public StreamError(String code) {
        super();
        this.code = code;
    }

    public StreamError(String code, String text) {
        this(code);
        this.text = text;
    }

    /**
     * Returns the error code.
     *
     * @return the error code.
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the error text, which may be null.
     *
     * @return the error text.
     */
    public String getText() {
        return text;
    }

    public String toString() {
        StringBuilder txt = new StringBuilder();
        txt.append("stream:error (").append(code).append(")");
        if (text != null) txt.append(" text: ").append(text);
        return txt.toString();
    }
}
