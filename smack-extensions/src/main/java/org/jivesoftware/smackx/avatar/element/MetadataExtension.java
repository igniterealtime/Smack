/*
 *
 * Copyright 2017 Fernando Ramirez, 2019 Paul Schaub
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
package org.jivesoftware.smackx.avatar.element;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.avatar.MetadataInfo;
import org.jivesoftware.smackx.avatar.MetadataPointer;
import org.jivesoftware.smackx.avatar.UserAvatarManager;

/**
 * Metadata extension element class.
 * This class contains metadata about published avatars.
 *
 * @author Fernando Ramirez
 * @author Paul Schaub
 * @see <a href="https://xmpp.org/extensions/xep-0084.html">XEP-0084: User Avatar</a>
 */
public class MetadataExtension implements ExtensionElement {

    public static final String ELEMENT = "metadata";
    public static final String NAMESPACE = UserAvatarManager.METADATA_NAMESPACE;

    private final List<MetadataInfo> infos;
    private final List<MetadataPointer> pointers;

    /**
     * Empty constructor.
     */
    public MetadataExtension() {
        this(null);
    }

    /**
     * Metadata Extension constructor.
     *
     * @param infos list of {@link MetadataInfo} elements.
     */
    public MetadataExtension(List<MetadataInfo> infos) {
        this(infos, null);
    }

    /**
     * Metadata Extension constructor.
     *
     * @param infos list of {@link MetadataInfo} elements
     * @param pointers optional list of {@link MetadataPointer} elements
     */
    public MetadataExtension(List<MetadataInfo> infos, List<MetadataPointer> pointers) {
        this.infos = infos;
        this.pointers = pointers;
    }

    /**
     * Get the info elements list.
     *
     * @return the info elements list
     */
    public List<MetadataInfo> getInfoElements() {
        return Collections.unmodifiableList(infos);
    }

    /**
     * Get the pointer elements list.
     *
     * @return the pointer elements list
     */
    public List<MetadataPointer> getPointerElements() {
        return (pointers == null) ? null : Collections.unmodifiableList(pointers);
    }

    /**
     * Return true, if this {@link MetadataExtension} is to be interpreted as Avatar unpublishing.
     *
     * @return true if unpublishing, false otherwise
     */
    public boolean isDisablingPublishing() {
        return getInfoElements().isEmpty();
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
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
        appendInfoElements(xml);
        appendPointerElements(xml);
        closeElement(xml);
        return xml;
    }

    private void appendInfoElements(XmlStringBuilder xml) {
        if (infos == null) {
            return;
        }

        xml.rightAngleBracket();

        for (MetadataInfo info : infos) {
            xml.halfOpenElement("info");
            xml.attribute("id", info.getId());
            xml.attribute("bytes", info.getBytes().longValue());
            xml.attribute("type", info.getType());
            xml.optAttribute("url", info.getUrl());

            if (info.getHeight().nativeRepresentation() > 0) {
                xml.attribute("height", info.getHeight().nativeRepresentation());
            }

            if (info.getWidth().nativeRepresentation() > 0) {
                xml.attribute("width", info.getWidth().nativeRepresentation());
            }

            xml.closeEmptyElement();
        }
    }

    private void appendPointerElements(XmlStringBuilder xml) {
        if (pointers == null) {
            return;
        }

        for (MetadataPointer pointer : pointers) {
            xml.openElement("pointer");
            xml.halfOpenElement("x");

            String namespace = pointer.getNamespace();
            if (namespace != null) {
                xml.xmlnsAttribute(namespace);
            }

            xml.rightAngleBracket();

            Map<String, Object> fields = pointer.getFields();
            if (fields != null) {
                for (Map.Entry<String, Object> pair : fields.entrySet()) {
                    xml.escapedElement(pair.getKey(), String.valueOf(pair.getValue()));
                }
            }

            xml.closeElement("x");
            xml.closeElement("pointer");
        }

    }

    private void closeElement(XmlStringBuilder xml) {
        if (infos != null || pointers != null) {
            xml.closeElement(this);
        } else {
            xml.closeEmptyElement();
        }
    }
}
