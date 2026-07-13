/*
 *
 * Copyright 2017-2022 Eng Chong Meng
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
package org.jivesoftware.smackx.jinglemessage.element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jinglemessage.JingleMessageState;

/**
 * Implements <code>XmlElement</code> for XEP-0353: Jingle Message Initiation 0.8.0 (2026-02-19).
 *
 * @author Eng Chong Meng
 * @see <a href="https://xmpp.org/extensions/xep-0353.html">XEP-0353: Jingle Message Initiation</a>
 */
public class JingleMessage implements XmlElement {
    public static final String NAMESPACE = "urn:xmpp:jingle-message:0";
    public final String ELEMENT;
    public final QName QNAME;

    public static final String ACTION_FINISH = "finish";
    public static final String ACTION_PROCEED = "proceed";
    public static final String ACTION_PROPOSE = "propose";
    public static final String ACTION_REJECT = "reject";
    public static final String ACTION_RETRACT = "retract";
    public static final String ACTION_RINGING = "ringing";

    public static final String ATTR_ID = "id";

    private final String mAction;
    private final String mUuid;
    /**
     * The <code>reason</code> extension in a <code>jingleMessage</code> provides machine
     * and possibly human-readable information about the reason for the action.
     */
    private final JingleReason mReason;

    // List of the NamedElement or XmlElement to be included in JingleMessage before sending.
    private List<NamedElement> namedElements = null;

    /**
     * Creates a new instance of jingleMessage.
     *
     * @param action message type element name
     * @param id Jingle message id.
     * @param reason Jingle reason.
     * @param namedElement child Named element to be included.
     */
    @SuppressWarnings("this-escape")
    public JingleMessage(String action, String id, JingleReason reason, NamedElement namedElement) {
        mAction = action;
        assert id != null;
        mUuid = id;
        mReason = reason;
        addElement(namedElement);

        ELEMENT = action;
        QNAME = new QName(NAMESPACE, ELEMENT);
    }

    @SuppressWarnings("this-escape")
    public JingleMessage(String action, String id) {
        this(action, id, null, null);
    }

    /**
     * Returns the action specified in the jingle message.
     *
     * @return the action specified in the jingle message.
     */
    public String getAction() {
        return mAction;
    }

    /**
     * Returns the jingle message Id.
     *
     * @return the jingle message id.
     */
    public String getId() {
        return mUuid;
    }

    public JingleMessageState getJmState() {
        return JingleMessageState.fromString(mAction);
    }

    public JingleReason getReason() {
        return mReason;
    }

    public List<NamedElement> getElements(String elementName) {
        List<NamedElement> elements = new ArrayList<>();
        if (namedElements != null) {
            for (NamedElement element : namedElements) {
                if (element.getElementName().equals(elementName)) {
                    elements.add(element);
                }
            }
        }
        return elements;
    }

    /**
     * Returns the jingle message extensions.
     *
     * @return the jingle message extensions.
     */
    public List<NamedElement> getElements() {
        return namedElements;
    }

    public void addElement(NamedElement namedElement) {
        if (namedElement == null) return;
        if (namedElements == null) {
            namedElements = new ArrayList<>();
        }
        namedElements.add(namedElement);
    }

    public void addElements(Collection<? extends NamedElement> elements) {
        if (elements == null) return;
        for (NamedElement namedElement : elements) {
            addElement(namedElement);
        }
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this, enclosingNamespace);
        xml.attribute(ATTR_ID, mUuid);

        if (mReason == null && namedElements == null) {
            xml.closeEmptyElement();
        }
        else {
            xml.rightAngleBracket();
            xml.optElement(mReason);

            xml.optAppend(namedElements);
            xml.closeElement(ELEMENT);
        }
        return xml;
    }
}
