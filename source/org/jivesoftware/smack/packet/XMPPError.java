/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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

import java.util.*;

/**
 * Represents a XMPP error sub-packet. Typically, a server responds to a request that has
 * problems by sending the packet back and including an error packet. Each error has a code, type, 
 * error condition as well as as an optional text explanation. Typical errors are:<p>
 *
 * <table border=1>
 *      <hr><td><b>Code</b></td><td><b>XMPP Error</b></td><td><b>Type</b></td></hr>
 *      <tr><td>500</td><td>interna-server-error</td><td>WAIT</td></tr>
 *      <tr><td>403</td><td>forbidden</td><td>AUTH</td></tr>
 *      <tr><td>400</td<td>bad-request</td><td>MODIFY</td>></tr>
 *      <tr><td>404</td><td>item-not-found</td><td>CANCEL</td></tr>
 *      <tr><td>409</td><td>conflict</td><td>CANCEL</td></tr>
 *      <tr><td>501</td><td>feature-not-implemented</td><td>CANCEL</td></tr>
 *      <tr><td>302</td><td>gone</td><td>MODIFY</td></tr>
 *      <tr><td>400</td><td>jid-malformed</td><td>MODIFY</td></tr>
 *      <tr><td>406</td><td>no-acceptable</td><td> MODIFY</td></tr>
 *      <tr><td>405</td><td>not-allowed</td><td>CANCEL</td></tr>
 *      <tr><td>401</td><td>not-authorized</td><td>AUTH</td></tr>
 *      <tr><td>402</td><td>payment-required</td><td>AUTH</td></tr>
 *      <tr><td>404</td><td>recipient-unavailable</td><td>WAIT</td></tr>
 *      <tr><td>302</td><td>redirect</td><td>MODIFY</td></tr>
 *      <tr><td>407</td><td>registration-required</td><td>AUTH</td></tr>
 *      <tr><td>404</td><td>remote-server-not-found</td><td>CANCEL</td></tr>
 *      <tr><td>504</td><td>remote-server-timeout</td><td>WAIT</td></tr>
 *      <tr><td>502</td><td>remote-server-error</td><td>CANCEL</td></tr>
 *      <tr><td>500</td><td>resource-constraint</td><td>WAIT</td></tr>
 *      <tr><td>503</td><td>service-unavailable</td><td>CANCEL</td></tr>
 *      <tr><td>407</td><td>subscription-required</td><td>AUTH</td></tr>
 *      <tr><td>500</td><td>undefined-condition</td><td>WAIT</td></tr>
 *      <tr><td>400</td><td>unexpected-condition</td><td>WAIT</td></tr>
 *      <tr><td>408</td><td>request-timeout</td><td>CANCEL</td></tr>
 * </table>
 *
 * @author Matt Tucker
 */
public class XMPPError {

    private int code;
    private Type type;
    private String condition;
    private String message;
    private List<PacketExtension> applicationExtensions = null;


    /**
     * Creates a new error with the specified condition infering the type and code.
     * If the Condition is predefined, client code should be like:
     *     new XMPPError(XMPPError.Condition.remote_server_timeout);
     * If the Condition is not predefined, invocations should be like 
     *     new XMPPError(new XMPPError.Condition("my_own_error"));
     * 
     * @param condition the error condition.
     */
    public XMPPError(Condition condition) {
        this.init(condition);
        this.message = null;
    }

    /**
     * Creates a new error with the specified condition and message infering the type and code.
     * If the Condition is predefined, client code should be like:
     *     new XMPPError(XMPPError.Condition.remote_server_timeout, "Error Explanation");
     * If the Condition is not predefined, invocations should be like 
     *     new XMPPError(new XMPPError.Condition("my_own_error"), "Error Explanation");
     *
     * @param condition the error condition.
     * @param messageText a message describing the error.
     */
    public XMPPError(Condition condition, String messageText) {
        this.init(condition);
        this.message = messageText;
    }

    /**
     * Creates a new  error with the specified code and no message.
     *
     * @param code the error code.
     * @deprecated new errors should be created using the constructor XMPPError(condition)
     */
    public XMPPError(int code) {
        this.code = code;
        this.message = null;
    }

    /**
     * Creates a new error with the specified code and message.
     * deprecated
     *
     * @param code the error code.
     * @param message a message describing the error.
     * @deprecated new errors should be created using the constructor XMPPError(condition, message)
     */
    public XMPPError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Creates a new error with the specified code, type, condition and message.
     * This constructor is used when the condition is not recognized automatically by XMPPError
     * i.e. there is not a defined instance of ErrorCondition or it does not applies the default 
     * specification.
     * 
     * @param code the error code.
     * @param type the error type.
     * @param condition the error condition.
     * @param message a message describing the error.
     * @param extension list of packet extensions
     */
    public XMPPError(int code, Type type, String condition, String message,
            List<PacketExtension> extension) {
        this.code = code;
        this.type = type;
        this.condition = condition;
        this.message = message;
        this.applicationExtensions = extension;
    }

    /**
     * Initialize the error infering the type and code for the received condition.
     * 
     * @param condition the error condition.
     */
    private void init(Condition condition) {
        // Look for the condition and its default code and type
        ErrorSpecification defaultErrorSpecification = ErrorSpecification.specFor(condition);
        this.condition = condition.value;
        if (defaultErrorSpecification != null) {
            // If there is a default error specification for the received condition,
            // it get configured with the infered type and code.
            this.type = defaultErrorSpecification.getType();
            this.code = defaultErrorSpecification.getCode();
        }
    }
    /**
     * Returns the error condition.
     *
     * @return the error condition.
     */
    public String getCondition() {
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
        buf.append("<error code=\"").append(code).append("\"");
        if (type != null) {
            buf.append(" type=\"");
            buf.append(type.name());
            buf.append("\"");
        }
        buf.append(">");
        if (condition != null) {
            buf.append("<").append(condition);
            buf.append(" xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>");
        }
        if (message != null) {
            buf.append("<text xml:lang=\"en\" xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">");
            buf.append(message);
            buf.append("</text>");
        }
        for (PacketExtension element : this.getExtensions()) {
            buf.append(element.toXML());
        }
        buf.append("</error>");
        return buf.toString();
    }

    public String toString() {
        StringBuilder txt = new StringBuilder();
        if (condition != null) {
            txt.append(condition);
        }
        txt.append("(").append(code).append(")");
        if (message != null) {
            txt.append(" ").append(message);
        }
        return txt.toString();
    }

    /**
     * Returns an Iterator for the error extensions attached to the xmppError.
     * An application MAY provide application-specific error information by including a 
     * properly-namespaced child in the error element.
     *
     * @return an Iterator for the error extensions.
     */
    public synchronized List<PacketExtension> getExtensions() {
        if (applicationExtensions == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(applicationExtensions);
    }

    /**
     * Returns the first patcket extension that matches the specified element name and
     * namespace, or <tt>null</tt> if it doesn't exist. 
     *
     * @param elementName the XML element name of the packet extension.
     * @param namespace the XML element namespace of the packet extension.
     * @return the extension, or <tt>null</tt> if it doesn't exist.
     */
    public synchronized PacketExtension getExtension(String elementName, String namespace) {
        if (applicationExtensions == null || elementName == null || namespace == null) {
            return null;
        }
        for (PacketExtension ext : applicationExtensions) {
            if (elementName.equals(ext.getElementName()) && namespace.equals(ext.getNamespace())) {
                return ext;
            }
        }
        return null;
    }

    /**
     * Adds a packet extension to the error.
     *
     * @param extension a packet extension.
     */
    public synchronized void addExtension(PacketExtension extension) {
        if (applicationExtensions == null) {
            applicationExtensions = new ArrayList<PacketExtension>();
        }
        applicationExtensions.add(extension);
    }

    /**
     * Set the packet extension to the error.
     *
     * @param extension a packet extension.
     */
    public synchronized void setExtension(List<PacketExtension> extension) {
        applicationExtensions = extension;
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
        CONTINUE
    }

    /**
     * A class to represent predefined error conditions.
     */
    public static class Condition {

        public static final Condition interna_server_error = new Condition("internal-server-error");
        public static final Condition forbidden = new Condition("forbidden");
        public static final Condition bad_request = new Condition("bad-request");
        public static final Condition conflict = new Condition("conflict");
        public static final Condition feature_not_implemented = new Condition("feature-not-implemented");
        public static final Condition gone = new Condition("gone");
        public static final Condition item_not_found = new Condition("item-not-found");
        public static final Condition jid_malformed = new Condition("jid-malformed");
        public static final Condition no_acceptable = new Condition("not-acceptable");
        public static final Condition not_allowed = new Condition("not-allowed");
        public static final Condition not_authorized = new Condition("not-authorized");
        public static final Condition payment_required = new Condition("payment-required");
        public static final Condition recipient_unavailable = new Condition("recipient-unavailable");
        public static final Condition redirect = new Condition("redirect");
        public static final Condition registration_required = new Condition("registration-required");
        public static final Condition remote_server_error = new Condition("remote-server-error");
        public static final Condition remote_server_not_found = new Condition("remote-server-not-found");
        public static final Condition remote_server_timeout = new Condition("remote-server-timeout");
        public static final Condition resource_constraint = new Condition("resource-constraint");
        public static final Condition service_unavailable = new Condition("service-unavailable");
        public static final Condition subscription_required = new Condition("subscription-required");
        public static final Condition undefined_condition = new Condition("undefined-condition");
        public static final Condition unexpected_request = new Condition("unexpected-request");
        public static final Condition request_timeout = new Condition("request-timeout");

        private String value;

        public Condition(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }


    /**
     * A class to represent the error specification used to infer common usage.
     */
    private static class ErrorSpecification {
        private int code;
        private Type type;
        private Condition condition;
        private static Map<Condition, ErrorSpecification> instances = errorSpecifications();

        private ErrorSpecification(Condition condition, Type type, int code) {
            this.code = code;
            this.type = type;
            this.condition = condition;
        }

        private static Map<Condition, ErrorSpecification> errorSpecifications() {
            Map<Condition, ErrorSpecification> instances = new HashMap<Condition, ErrorSpecification>(22);
            instances.put(Condition.interna_server_error, new ErrorSpecification(
                    Condition.interna_server_error, Type.WAIT, 500));
            instances.put(Condition.forbidden, new ErrorSpecification(Condition.forbidden,
                    Type.AUTH, 403));
            instances.put(Condition.bad_request, new XMPPError.ErrorSpecification(
                    Condition.bad_request, Type.MODIFY, 400));
            instances.put(Condition.item_not_found, new XMPPError.ErrorSpecification(
                    Condition.item_not_found, Type.CANCEL, 404));
            instances.put(Condition.conflict, new XMPPError.ErrorSpecification(
                    Condition.conflict, Type.CANCEL, 409));
            instances.put(Condition.feature_not_implemented, new XMPPError.ErrorSpecification(
                    Condition.feature_not_implemented, Type.CANCEL, 501));
            instances.put(Condition.gone, new XMPPError.ErrorSpecification(
                    Condition.gone, Type.MODIFY, 302));
            instances.put(Condition.jid_malformed, new XMPPError.ErrorSpecification(
                    Condition.jid_malformed, Type.MODIFY, 400));
            instances.put(Condition.no_acceptable, new XMPPError.ErrorSpecification(
                    Condition.no_acceptable, Type.MODIFY, 406));
            instances.put(Condition.not_allowed, new XMPPError.ErrorSpecification(
                    Condition.not_allowed, Type.CANCEL, 405));
            instances.put(Condition.not_authorized, new XMPPError.ErrorSpecification(
                    Condition.not_authorized, Type.AUTH, 401));
            instances.put(Condition.payment_required, new XMPPError.ErrorSpecification(
                    Condition.payment_required, Type.AUTH, 402));
            instances.put(Condition.recipient_unavailable, new XMPPError.ErrorSpecification(
                    Condition.recipient_unavailable, Type.WAIT, 404));
            instances.put(Condition.redirect, new XMPPError.ErrorSpecification(
                    Condition.redirect, Type.MODIFY, 302));
            instances.put(Condition.registration_required, new XMPPError.ErrorSpecification(
                    Condition.registration_required, Type.AUTH, 407));
            instances.put(Condition.remote_server_not_found, new XMPPError.ErrorSpecification(
                    Condition.remote_server_not_found, Type.CANCEL, 404));
            instances.put(Condition.remote_server_timeout, new XMPPError.ErrorSpecification(
                    Condition.remote_server_timeout, Type.WAIT, 504));
            instances.put(Condition.remote_server_error, new XMPPError.ErrorSpecification(
                    Condition.remote_server_error, Type.CANCEL, 502));
            instances.put(Condition.resource_constraint, new XMPPError.ErrorSpecification(
                    Condition.resource_constraint, Type.WAIT, 500));
            instances.put(Condition.service_unavailable, new XMPPError.ErrorSpecification(
                    Condition.service_unavailable, Type.CANCEL, 503));
            instances.put(Condition.subscription_required, new XMPPError.ErrorSpecification(
                    Condition.subscription_required, Type.AUTH, 407));
            instances.put(Condition.undefined_condition, new XMPPError.ErrorSpecification(
                    Condition.undefined_condition, Type.WAIT, 500));
            instances.put(Condition.unexpected_request, new XMPPError.ErrorSpecification(
                    Condition.unexpected_request, Type.WAIT, 400));
            instances.put(Condition.request_timeout, new XMPPError.ErrorSpecification(
                    Condition.request_timeout, Type.CANCEL, 408));

            return instances;
        }

        protected static ErrorSpecification specFor(Condition condition) {
            return instances.get(condition);
        }

        /**
         * Returns the error condition.
         *
         * @return the error condition.
         */
        protected Condition getCondition() {
            return condition;
        }

        /**
         * Returns the error type.
         *
         * @return the error type.
         */
        protected Type getType() {
            return type;
        }

        /**
         * Returns the error code.
         *
         * @return the error code.
         */
        protected int getCode() {
            return code;
        }
    }
}
