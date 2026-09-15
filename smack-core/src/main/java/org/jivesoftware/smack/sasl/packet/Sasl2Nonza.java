/*
 *
 * Copyright 2026 Florian Schmaus
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
package org.jivesoftware.smack.sasl.packet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.AbstractError;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.sasl.SASLError;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.XmppElementUtil;

public interface Sasl2Nonza extends Nonza {
    String NAMESPACE = "urn:xmpp:sasl:2";

    @Override
    default String getNamespace() {
        return NAMESPACE;
    }

    class UserAgent implements ExtensionElement {
        public static final String ELEMENT = "user-agent";
        public static final String NAMESPACE = Sasl2Nonza.NAMESPACE;
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        public static final String DEFAULT_SOFTWARE = "Smack";

        /**
         * A stable identifier for this connection's "client installation", per XEP-0388 § 2.3: "The contents of the
         * 'id' attribute MUST be a UUID v4."
         */
        private final UUID id;
        private final String software;
        private final String device;

        public UserAgent(UUID id, String software, String device) {
            this.id = id;
            this.software = software;
            this.device = device;
        }

        public UserAgent(String software, String device) {
            this(java.util.UUID.randomUUID(), software, device);
        }

        public UserAgent(String software) {
            this(software, null);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        public UUID getId() {
            return id;
        }

        public String getSoftware() {
            return software;
        }

        public String getDevice() {
            return device;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            if (id != null) {
                xml.attribute("id", id.toString());
            }
            if (software == null && device == null) {
                xml.closeEmptyElement();
                return xml;
            }
            xml.rightAngleBracket();
            xml.optElement("software", software);
            xml.optElement("device", device);
            xml.closeElement(this);
            return xml;
        }
    }

    class Authenticate implements Sasl2Nonza {
        public static final String ELEMENT = "authenticate";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String mechanism;
        private final String initialResponse;
        private final UserAgent userAgent;
        private final List<XmlElement> extensionElements;

        public Authenticate(String mechanism, String initialResponse, UserAgent userAgent) {
            this(mechanism, initialResponse, userAgent, null);
        }

        public Authenticate(String mechanism, String initialResponse, UserAgent userAgent, List<? extends XmlElement> extensionElements) {
            this.mechanism = Objects.requireNonNull(mechanism, "SASL mechanism must not be null");
            this.initialResponse = initialResponse;
            this.userAgent = userAgent;
            this.extensionElements = extensionElements == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(extensionElements));
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        public String getMechanism() {
            return mechanism;
        }

        public String getInitialResponse() {
            return initialResponse;
        }

        public UserAgent getUserAgent() {
            return userAgent;
        }

        public List<XmlElement> getExtensionElements() {
            return extensionElements;
        }

        public boolean hasExtension(Class<? extends ExtensionElement> extensionElementClass) {
            return getExtension(extensionElementClass) != null;
        }

        public boolean hasExtension(QName qname) {
            return getExtension(qname) != null;
        }

        public <E extends ExtensionElement> E getExtension(Class<E> extensionElementClass) {
            return XmppElementUtil.from(extensionElements, extensionElementClass);
        }

        public XmlElement getExtension(QName qname) {
            for (XmlElement element : extensionElements) {
                if (qname.equals(element.getQName())) {
                    return element;
                }
            }
            return null;
        }

        public <E extends ExtensionElement> List<E> getExtensions(Class<E> extensionElementClass) {
            return XmppElementUtil.getElementsFrom(extensionElements, extensionElementClass);
        }

        public List<XmlElement> getExtensions(QName qname) {
            List<XmlElement> res = new ArrayList<>();
            for (XmlElement element : extensionElements) {
                if (qname.equals(element.getQName())) {
                    res.add(element);
                }
            }
            return Collections.unmodifiableList(res);
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.attribute("mechanism", mechanism);
            if (initialResponse == null && userAgent == null && extensionElements.isEmpty()) {
                xml.closeEmptyElement();
                return xml;
            }
            xml.rightAngleBracket();
            xml.optElement("initial-response", initialResponse);
            xml.optAppend(userAgent);
            xml.append(extensionElements);
            xml.closeElement(this);
            return xml;
        }
    }

    class Challenge implements Sasl2Nonza {
        public static final String ELEMENT = "challenge";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String data;

        public Challenge(String data) {
            this.data = StringUtils.returnIfNotEmptyTrimmed(data);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        public String getData() {
            return data;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.optTextChild(data, this);
            return xml;
        }
    }

    class Response implements Sasl2Nonza {
        public static final String ELEMENT = "response";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String data;

        public Response() {
            this(null);
        }

        public Response(String data) {
            this.data = StringUtils.returnIfNotEmptyTrimmed(data);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        public String getData() {
            return data;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.optTextChild(data, this);
            return xml;
        }
    }

    class Success implements Sasl2Nonza {
        public static final String ELEMENT = "success";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String additionalData;
        private final CharSequence authorizationIdentifier;
        private final List<XmlElement> extensionElements;

        public Success(String additionalData, CharSequence authorizationIdentifier, List<? extends XmlElement> extensionElements) {
            this.additionalData = additionalData;
            this.authorizationIdentifier = authorizationIdentifier;
            this.extensionElements = extensionElements == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(extensionElements));
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        public String getAdditionalData() {
            return additionalData;
        }

        public CharSequence getAuthorizationIdentifier() {
            return authorizationIdentifier;
        }

        public List<XmlElement> getExtensionElements() {
            return extensionElements;
        }

        public boolean hasExtension(Class<? extends ExtensionElement> extensionElementClass) {
            return getExtension(extensionElementClass) != null;
        }

        public boolean hasExtension(String elementName, String namespace) {
            return hasExtension(new QName(namespace, elementName));
        }

        public boolean hasExtension(QName qname) {
            return getExtension(qname) != null;
        }

        public XmlElement getExtension(String elementName, String namespace) {
            return getExtension(new QName(namespace, elementName));
        }

        public <E extends ExtensionElement> E getExtension(Class<E> extensionElementClass) {
            if (extensionElementClass.isInterface()) {
                for (XmlElement element : extensionElements) {
                    if (extensionElementClass.isInstance(element)) {
                        return extensionElementClass.cast(element);
                    }
                }
                return null;
            }
            return XmppElementUtil.from(extensionElements, extensionElementClass);
        }

        public XmlElement getExtension(QName qname) {
            for (XmlElement element : extensionElements) {
                if (qname.equals(element.getQName())) {
                    return element;
                }
            }
            return null;
        }

        public <E extends ExtensionElement> List<E> getExtensions(Class<E> extensionElementClass) {
            if (extensionElementClass.isInterface()) {
                List<E> res = new ArrayList<>();
                for (XmlElement element : extensionElements) {
                    if (extensionElementClass.isInstance(element)) {
                        res.add(extensionElementClass.cast(element));
                    }
                }
                return res;
            }
            return XmppElementUtil.getElementsFrom(extensionElements, extensionElementClass);
        }

        public List<XmlElement> getExtensions(QName qname) {
            List<XmlElement> res = new ArrayList<>();
            for (XmlElement element : extensionElements) {
                if (qname.equals(element.getQName())) {
                    res.add(element);
                }
            }
            return Collections.unmodifiableList(res);
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            if (additionalData == null && authorizationIdentifier == null && extensionElements.isEmpty()) {
                xml.closeEmptyElement();
                return xml;
            }
            xml.rightAngleBracket();
            xml.optElement("additional-data", additionalData);
            xml.optElement("authorization-identifier", authorizationIdentifier);
            xml.append(extensionElements);
            xml.closeElement(this);
            return xml;
        }
    }

    class Failure extends AbstractError implements Sasl2Nonza {
        public static final String ELEMENT = "failure";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final SASLError saslError;
        private final String saslErrorString;

        public Failure(String saslErrorString) {
            this(saslErrorString, null, null);
        }

        public Failure(SASLError saslError, Map<String, String> descriptiveTexts, List<? extends XmlElement> extensionElements) {
            super(descriptiveTexts, null, extensionElements == null ? Collections.emptyList() : new ArrayList<>(extensionElements));
            this.saslError = Objects.requireNonNull(saslError, "SASLError must not be null");
            this.saslErrorString = saslError.toString();
        }

        public Failure(String saslErrorString, Map<String, String> descriptiveTexts, List<? extends XmlElement> extensionElements) {
            super(descriptiveTexts, null, extensionElements == null ? Collections.emptyList() : new ArrayList<>(extensionElements));
            this.saslErrorString = saslErrorString;
            this.saslError = saslErrorString != null ? SASLError.fromString(saslErrorString) : null;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        public SASLError getSASLError() {
            return saslError;
        }

        public String getSASLErrorString() {
            return saslErrorString;
        }

        public List<XmlElement> getExtensionElements() {
            return extensions;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            if (saslErrorString == null && descriptiveTexts.isEmpty() && extensions.isEmpty()) {
                xml.closeEmptyElement();
                return xml;
            }
            xml.rightAngleBracket();
            if (saslErrorString != null) {
                xml.halfOpenElement(saslErrorString).xmlnsAttribute(SaslNonza.NAMESPACE).closeEmptyElement();
            }
            addDescriptiveTextsAndExtensions(xml);
            xml.closeElement(this);
            return xml;
        }
    }

    class Continue implements Sasl2Nonza {
        public static final String ELEMENT = "continue";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String additionalData;
        private final List<String> tasks;
        private final String text;

        public Continue(String additionalData, List<String> tasks, String text) {
            this.additionalData = additionalData;
            this.tasks = tasks == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(tasks));
            this.text = text;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        public String getAdditionalData() {
            return additionalData;
        }

        public List<String> getTasks() {
            return tasks;
        }

        public String getText() {
            return text;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            if (additionalData == null && tasks.isEmpty() && text == null) {
                xml.closeEmptyElement();
                return xml;
            }
            xml.rightAngleBracket();
            xml.optElement("additional-data", additionalData);
            if (!tasks.isEmpty()) {
                xml.openElement("tasks");
                for (String task : tasks) {
                    xml.element("task", task);
                }
                xml.closeElement("tasks");
            }
            xml.optElement("text", text);
            xml.closeElement(this);
            return xml;
        }
    }

    class Next implements Sasl2Nonza {
        public static final String ELEMENT = "next";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String task;
        private final List<XmlElement> extensionElements;

        public Next(String task, List<? extends XmlElement> extensionElements) {
            this.task = Objects.requireNonNull(task, "Task must not be null");
            this.extensionElements = extensionElements == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(extensionElements));
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        public String getTask() {
            return task;
        }

        public List<XmlElement> getExtensionElements() {
            return extensionElements;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.attribute("task", task);
            if (extensionElements.isEmpty()) {
                xml.closeEmptyElement();
                return xml;
            }
            xml.rightAngleBracket();
            xml.append(extensionElements);
            xml.closeElement(this);
            return xml;
        }
    }

    class TaskData implements Sasl2Nonza {
        public static final String ELEMENT = "task-data";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final List<XmlElement> extensionElements;

        public TaskData(List<? extends XmlElement> extensionElements) {
            this.extensionElements = extensionElements == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(extensionElements));
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        public List<XmlElement> getExtensionElements() {
            return extensionElements;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            if (extensionElements.isEmpty()) {
                xml.closeEmptyElement();
                return xml;
            }
            xml.rightAngleBracket();
            xml.append(extensionElements);
            xml.closeElement(this);
            return xml;
        }
    }

    class Abort implements Sasl2Nonza {
        public static final String ELEMENT = "abort";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String text;
        private final List<XmlElement> extensionElements;

        public Abort() {
            this(null, null);
        }

        public Abort(String text) {
            this(text, null);
        }

        public Abort(String text, List<? extends XmlElement> extensionElements) {
            this.text = text;
            this.extensionElements = extensionElements == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(extensionElements));
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        public String getText() {
            return text;
        }

        public List<XmlElement> getExtensionElements() {
            return extensionElements;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            if (text == null && extensionElements.isEmpty()) {
                xml.closeEmptyElement();
                return xml;
            }
            xml.rightAngleBracket();
            xml.optElement("text", text);
            xml.append(extensionElements);
            xml.closeElement(this);
            return xml;
        }
    }
}
