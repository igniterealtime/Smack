/**
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

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.jingle_rtp.element.RtpDescription;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * Implements <code>ExtensionElement</code> for XEP-0353: Jingle Message Initiation 0.4.0 (2021-11-27).
 * @see <a href="https://xmpp.org/extensions/xep-0353.html">XEP-0353: Jingle Message Initiation</a>
 *
 * @author Eng Chong Meng
 */
public class JingleMessage implements ExtensionElement {
    public static String ELEMENT = "propose";
    public static final String NAMESPACE = "urn:xmpp:jingle-message:0";

    public static final String ACTION_PROPOSE = "propose";
    public static final String ACTION_RETRACT = "retract";
    public static final String ACTION_ACCEPT = "accept";
    public static final String ACTION_PROCEED = "proceed";
    public static final String ACTION_REJECT = "reject";

    public static final String ATTR_ID = "id";

    public static QName QNAME = new QName(NAMESPACE, ELEMENT);

    private List<RtpDescription> rtpDescriptions = null;

    private String action;
    private final String id;
    private final List<String> media = new ArrayList<>();

    /**
     * Creates a new instance of jingleMessage.
     *
     * @param action message type element name
     * @param id Jingle message id.
     */
    public JingleMessage(String action, String id) {
        this.action = action;
        this.id = id;
        ELEMENT = action;
        QNAME = new QName(NAMESPACE, ELEMENT);
    }

    public JingleMessage(StandardExtensionElement extElement) {
        this(extElement.getElementName(), extElement.getAttributeValue(ATTR_ID));
        if (ACTION_PROPOSE.equals(action)) {
            List<StandardExtensionElement> elements
                    = extElement.getElements(RtpDescription.ELEMENT, RtpDescription.NAMESPACE);
            media.clear();
            if (elements != null) {
                for (StandardExtensionElement element : elements) {
                    media.add(element.getAttributeValue(RtpDescription.ATTR_MEDIA));
                }
            }
        }
    }

    /**
     * Returns the action specified in the jingle message.
     *
     * @return the action specified in the jingle message.
     */
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Returns the jingle message Id.
     *
     * @return the jingle message id.
     */
    public String getId() {
        return id;
    }

    public List<String> getMedia() {
        return media;
    }

    /**
     * Returns the jingle message RtpDescription.
     *
     * @return the jingle message RtpDescription.
     */
    public List<RtpDescription> getDescriptionExt() {
        return rtpDescriptions;
    }

    public void addDescriptionExtension(RtpDescription extElement) {
        if (rtpDescriptions == null) {
            rtpDescriptions = new ArrayList<>();
        }
        rtpDescriptions.add(extElement);
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
        xml.attribute(ATTR_ID, id);
        if (rtpDescriptions == null) {
            xml.closeEmptyElement();
        } else {
            xml.rightAngleBracket();
            for (RtpDescription extension : rtpDescriptions) {
                xml.append(extension);
            }
            xml.closeElement(ELEMENT);
        }
        return xml;
    }
}
