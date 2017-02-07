/**
 *
 * Copyright 2017 Fernando Ramirez
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.avatar.MetadataInfo;
import org.jivesoftware.smackx.avatar.MetadataPointer;
import org.jivesoftware.smackx.avatar.UserAvatarManager;

/**
 * Metadata extension element class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0084.html">XEP-0084: User
 *      Avatar</a>
 */
public class MetadataExtension implements ExtensionElement {

    public static final String ELEMENT = "metadata";
    public static final String NAMESPACE = UserAvatarManager.METADATA_NAMESPACE;

    private List<MetadataInfo> infos;
    private List<MetadataPointer> pointers;

    /**
     * Metadata Extension constructor.
     * 
     * @param infos
     */
    public MetadataExtension(List<MetadataInfo> infos) {
        this.infos = infos;
    }

    /**
     * Metadata Extension constructor.
     * 
     * @param infos
     * @param pointers
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
    public List<MetadataInfo> getInfos() {
        return infos;
    }

    /**
     * Get the pointer elements list.
     * 
     * @return the pointer elements list
     */
    public List<MetadataPointer> getPointers() {
        return pointers;
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
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        parseInfos(xml);
        parsePointers(xml);
        closeElement(xml);
        return xml;
    }

    private void parseInfos(XmlStringBuilder xml) {
        if (infos != null) {
            xml.rightAngleBracket();

            for (MetadataInfo info : infos) {
                xml.halfOpenElement("info");
                xml.attribute("id", info.getId());
                xml.attribute("bytes", String.valueOf(info.getBytes()));
                xml.attribute("type", info.getType());
                xml.optAttribute("url", info.getUrl());

                if (info.getHeight() > 0) {
                    xml.attribute("height", info.getHeight());
                }

                if (info.getWidth() > 0) {
                    xml.attribute("width", info.getWidth());
                }

                xml.closeEmptyElement();
            }
        }
    }

    private void parsePointers(XmlStringBuilder xml) {
        if (pointers != null) {

            for (MetadataPointer pointer : pointers) {
                xml.openElement("pointer");
                xml.halfOpenElement("x");

                String namespace = pointer.getNamespace();
                if (namespace != null) {
                    xml.xmlnsAttribute(namespace);
                }

                xml.rightAngleBracket();

                HashMap<String, Object> fields = pointer.getFields();
                if (fields != null) {
                    Iterator<Map.Entry<String, Object>> it = fields.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, Object> pair = it.next();
                        xml.escapedElement(pair.getKey(), String.valueOf(pair.getValue()));
                    }
                }

                xml.closeElement("x");
                xml.closeElement("pointer");
            }

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
