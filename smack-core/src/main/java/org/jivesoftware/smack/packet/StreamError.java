/**
 *
 * Copyright 2003-2005 Jive Software.
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

package org.jivesoftware.smack.packet;

import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

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
 *      <tr><td> not-well-formed </td><td> the initiating entity has sent XML that is not
 *          well-formed. </td></tr>
 * </table>
 * <p>
 * Stream error syntax:
 * <pre>
 * {@code
 * <stream:error>
 *   <defined-condition xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>
 *   [<text xmlns='urn:ietf:params:xml:ns:xmpp-streams'
 *      xml:lang='langcode'>
 *   OPTIONAL descriptive text
 *   </text>]
 *   [OPTIONAL application-specific condition element]
 * </stream:error>
 * }
 * </pre>
 *
 * @author Gaston Dombiak
 */
public class StreamError extends AbstractError implements PlainStreamElement {

    public static final String ELEMENT = "stream:error";
    public static final String NAMESPACE = "urn:ietf:params:xml:ns:xmpp-streams";

    private final Condition condition;
    private final String conditionText;

    public StreamError(Condition condition, String conditionText, Map<String, String> descriptiveTexts, List<ExtensionElement> extensions) {
        super(descriptiveTexts, extensions);
        // Some implementations may send the condition as non-empty element containing the empty string, that is
        // <condition xmlns='foo'></condition>, in this case the parser may calls this constructor with the empty string
        // as conditionText, therefore reset it to null if it's the empty string
        if (StringUtils.isNullOrEmpty(conditionText)) {
            conditionText = null;
        }
        if (conditionText != null) {
            switch (condition) {
            case see_other_host:
                break;
            default:
                throw new IllegalArgumentException("The given condition '" + condition
                                + "' can not contain a conditionText");
            }
        }
        this.condition = condition;
        this.conditionText = conditionText;
    }

    public Condition getCondition() {
        return condition;
    }

    public String getConditionText() {
        return conditionText;
    }

    @Override
    public String toString() {
        return toXML().toString();
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.openElement(ELEMENT);
        xml.halfOpenElement(condition.toString()).xmlnsAttribute(NAMESPACE).closeEmptyElement();
        addDescriptiveTextsAndExtensions(xml);
        xml.closeElement(ELEMENT);
        return xml;
    }

    /**
     * The defined stream error conditions, see RFC 6120 § 4.9.3
     *
     */
    public enum Condition {
        bad_format,
        bad_namespace_prefix,
        conflict,
        connection_timeout,
        host_gone,
        host_unknown,
        improper_addressing,
        internal_server_error,
        invalid_from,
        invalid_namespace,
        invalid_xml,
        not_authorized,
        not_well_formed,
        policy_violation,
        remote_connection_failed,
        reset,
        resource_constraint,
        restricted_xml,
        see_other_host,
        system_shutdown,
        undeficed_condition,
        unsupported_encoding,
        unsupported_feature,
        unsupported_stanza_type,
        unsupported_version;

        @Override
        public String toString() {
            return this.name().replace('_', '-');
        }

        public static Condition fromString(String string) {
            string = string.replace('-', '_');
            Condition condition = null;
            try {
                condition = Condition.valueOf(string);
            } catch (Exception e) {
                throw new IllegalStateException("Could not transform string '" + string
                                + "' to XMPPErrorCondition", e);
            }
            return condition;
        }
    }
}
