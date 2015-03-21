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
package org.jivesoftware.smack.packet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Represents an XMPP error sub-packet. Typically, a server responds to a request that has
 * problems by sending the stanza(/packet) back and including an error packet. Each error has a type,
 * error condition as well as as an optional text explanation. Typical errors are:<p>
 *
 * <table border=1>
 *      <hr><td><b>XMPP Error Condition</b></td><td><b>Type</b></td><td><b>RFC 6120 Section</b></td></hr>
 *      <tr><td>bad-request</td><td>MODIFY</td><td>8.3.3.1</td></tr>
 *      <tr><td>conflict</td><td>CANCEL</td><td>8.3.3.2</td></tr>
 *      <tr><td>feature-not-implemented</td><td>CANCEL</td><td>8.3.3.3</td></tr>
 *      <tr><td>forbidden</td><td>AUTH</td><td>8.3.3.4</td></tr>
 *      <tr><td>gone</td><td>MODIFY</td><td>8.3.3.5</td></tr>
 *      <tr><td>internal-server-error</td><td>WAIT</td><td>8.3.3.6</td></tr>
 *      <tr><td>item-not-found</td><td>CANCEL</td><td>8.3.3.7</td></tr>
 *      <tr><td>jid-malformed</td><td>MODIFY</td><td>8.3.3.8</td></tr>
 *      <tr><td>not-acceptable</td><td> MODIFY</td><td>8.3.3.9</td></tr>
 *      <tr><td>not-allowed</td><td>CANCEL</td><td>8.3.3.10</td></tr>
 *      <tr><td>not-authorized</td><td>AUTH</td><td>8.3.3.11</td></tr>
 *      <tr><td>policy-violation</td><td>AUTH</td><td>8.3.3.12</td></tr>
 *      <tr><td>recipient-unavailable</td><td>WAIT</td><td>8.3.3.13</td></tr>
 *      <tr><td>redirect</td><td>MODIFY</td><td>8.3.3.14</td></tr>
 *      <tr><td>registration-required</td><td>AUTH</td><td>8.3.3.15</td></tr>
 *      <tr><td>remote-server-not-found</td><td>CANCEL</td><td>8.3.3.16</td></tr>
 *      <tr><td>remote-server-timeout</td><td>WAIT</td><td>8.3.3.17</td></tr>
 *      <tr><td>resource-constraint</td><td>WAIT</td><td>8.3.3.18</td></tr>
 *      <tr><td>service-unavailable</td><td>CANCEL</td><td>8.3.3.19</td></tr>
 *      <tr><td>subscription-required</td><td>AUTH</td><td>8.3.3.20</td></tr>
 *      <tr><td>undefined-condition</td><td>WAIT</td><td>8.3.3.21</td></tr>
 *      <tr><td>unexpected-request</td><td>WAIT</td><td>8.3.3.22</td></tr>
 * </table>
 *
 * @author Matt Tucker
 * @see <a href="http://xmpp.org/rfcs/rfc6120.html#stanzas-error-syntax">RFC 6120 - 8.3.2 Syntax: The Syntax of XMPP error stanzas</a>
 */
public class XMPPError extends AbstractError {

    public static final String NAMESPACE = "urn:ietf:params:xml:ns:xmpp-stanzas";
    public static final String ERROR = "error";

    private static final Logger LOGGER = Logger.getLogger(XMPPError.class.getName());
    private static final Map<Condition, Type> CONDITION_TO_TYPE = new HashMap<Condition, Type>();

    static {
        CONDITION_TO_TYPE.put(Condition.bad_request, Type.MODIFY);
        CONDITION_TO_TYPE.put(Condition.conflict, Type.CANCEL);
        CONDITION_TO_TYPE.put(Condition.feature_not_implemented, Type.CANCEL);
        CONDITION_TO_TYPE.put(Condition.forbidden, Type.AUTH);
        CONDITION_TO_TYPE.put(Condition.gone, Type.CANCEL);
        CONDITION_TO_TYPE.put(Condition.internal_server_error, Type.CANCEL);
        CONDITION_TO_TYPE.put(Condition.item_not_found, Type.CANCEL);
        CONDITION_TO_TYPE.put(Condition.jid_malformed, Type.MODIFY);
        CONDITION_TO_TYPE.put(Condition.not_acceptable, Type.MODIFY);
        CONDITION_TO_TYPE.put(Condition.not_allowed, Type.CANCEL);
        CONDITION_TO_TYPE.put(Condition.not_authorized, Type.AUTH);
        CONDITION_TO_TYPE.put(Condition.policy_violation, Type.MODIFY);
        CONDITION_TO_TYPE.put(Condition.recipient_unavailable, Type.WAIT);
        CONDITION_TO_TYPE.put(Condition.redirect, Type.MODIFY);
        CONDITION_TO_TYPE.put(Condition.registration_required, Type.AUTH);
        CONDITION_TO_TYPE.put(Condition.remote_server_not_found, Type.CANCEL);
        CONDITION_TO_TYPE.put(Condition.remote_server_timeout, Type.WAIT);
        CONDITION_TO_TYPE.put(Condition.resource_constraint, Type.WAIT);
        CONDITION_TO_TYPE.put(Condition.service_unavailable, Type.WAIT);
        CONDITION_TO_TYPE.put(Condition.subscription_required, Type.WAIT);
        CONDITION_TO_TYPE.put(Condition.unexpected_request, Type.MODIFY);
    }
    private final Condition condition;
    private final String conditionText;
    private final String errorGenerator;
    private final Type type;

    public XMPPError(Condition condition) {
        this(condition, null, null, null, null, null);
    }

    public XMPPError(Condition condition, ExtensionElement applicationSpecificCondition) {
        this(condition, null, null, null, null, Arrays.asList(applicationSpecificCondition));
    }

    /**
     * Creates a new error with the specified type, condition and message.
     * This constructor is used when the condition is not recognized automatically by XMPPError
     * i.e. there is not a defined instance of ErrorCondition or it does not apply the default 
     * specification.
     * 
     * @param type the error type.
     * @param condition the error condition.
     * @param descriptiveTexts 
     * @param extensions list of stanza(/packet) extensions
     */
    public XMPPError(Condition condition, String conditionText, String errorGenerator, Type type, Map<String, String> descriptiveTexts,
            List<ExtensionElement> extensions) {
        super(descriptiveTexts, NAMESPACE, extensions);
        this.condition = condition;
        // Some implementations may send the condition as non-empty element containing the empty string, that is
        // <condition xmlns='foo'></condition>, in this case the parser may calls this constructor with the empty string
        // as conditionText, therefore reset it to null if it's the empty string
        if (StringUtils.isNullOrEmpty(conditionText)) {
            conditionText = null;
        }
        if (conditionText != null) {
            switch (condition) {
            case gone:
            case redirect:
                break;
            default:
                throw new IllegalArgumentException(
                                "Condition text can only be set with condtion types 'gone' and 'redirect', not "
                                                + condition);
            }
        }
        this.conditionText = conditionText;
        this.errorGenerator = errorGenerator;
        if (type == null) {
            Type determinedType = CONDITION_TO_TYPE.get(condition);
            if (determinedType == null) {
                LOGGER.warning("Could not determine type for condition: " + condition);
                determinedType = Type.CANCEL;
            }
            this.type = determinedType;
        } else {
            this.type = type;
        }
    }

    /**
     * Returns the error condition.
     *
     * @return the error condition.
     */
    public Condition getCondition() {
        return condition;
    }

    /**
     * Returns the error type.
     *
     * @return the error type.
     */
    public Type getType() {
        return type;
    }

    public String getErrorGenerator() {
        return errorGenerator;
    }

    public String getConditionText() {
        return conditionText;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("XMPPError: ");
        sb.append(condition.toString()).append(" - ").append(type.toString());
        if (errorGenerator != null) {
            sb.append(". Generated by ").append(errorGenerator);
        }
        return sb.toString();
    }

    /**
     * Returns the error as XML.
     *
     * @return the error as XML.
     */
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.halfOpenElement(ERROR);
        xml.attribute("type", type.toString());
        xml.optAttribute("by", errorGenerator);
        xml.rightAngleBracket();

        xml.halfOpenElement(condition.toString());
        xml.xmlnsAttribute(NAMESPACE);
        if (conditionText != null) {
            xml.rightAngleBracket();
            xml.escape(conditionText);
            xml.closeElement(condition.toString());
        }
        else {
            xml.closeEmptyElement();
        }

        addDescriptiveTextsAndExtensions(xml);

        xml.closeElement(ERROR);
        return xml;
    }

    public static XMPPError from(Condition condition, String descriptiveText) {
        Map<String, String> descriptiveTexts = new HashMap<String, String>();
        descriptiveTexts.put("en", descriptiveText);
        return new XMPPError(condition, null, null, null, descriptiveTexts, null);
    }

    /**
     * A class to represent the type of the Error. The types are:
     *
     * <ul>
     *      <li>XMPPError.Type.WAIT - retry after waiting (the error is temporary)
     *      <li>XMPPError.Type.CANCEL - do not retry (the error is unrecoverable)
     *      <li>XMPPError.Type.MODIFY - retry after changing the data sent
     *      <li>XMPPError.Type.AUTH - retry after providing credentials
     *      <li>XMPPError.Type.CONTINUE - proceed (the condition was only a warning)
     * </ul>
     */
    public static enum Type {
        WAIT,
        CANCEL,
        MODIFY,
        AUTH,
        CONTINUE;

        @Override
        public String toString() {
            // Locale.US not required, since Type consists only of ASCII chars
            return name().toLowerCase();
        }

        public static Type fromString(String string) {
            // Locale.US not required, since Type consists only of ASCII chars
            string = string.toUpperCase();
            return Type.valueOf(string);
        }
    }

    public enum Condition {
        bad_request,
        conflict,
        feature_not_implemented,
        forbidden,
        gone,
        internal_server_error,
        item_not_found,
        jid_malformed,
        not_acceptable,
        not_allowed,
        not_authorized,
        policy_violation,
        recipient_unavailable,
        redirect,
        registration_required,
        remote_server_not_found,
        remote_server_timeout,
        resource_constraint,
        service_unavailable,
        subscription_required,
        undefined_condition,
        unexpected_request;

        @Override
        public String toString() {
            return this.name().replace('_', '-');
        }

        public static Condition fromString(String string) {
            // Backwards compatibility for older implementations still using RFC 3920. RFC 6120
            // changed 'xml-not-well-formed' to 'not-well-formed'.
            if ("xml-not-well-formed".equals(string)) {
                string = "not-well-formed";
            }
            string = string.replace('-', '_');
            Condition condition = null;
            try {
                condition = Condition.valueOf(string);
            } catch (Exception e) {
                throw new IllegalStateException("Could not transform string '" + string + "' to XMPPErrorCondition", e);
            }
            return condition;
        }
    }

}
