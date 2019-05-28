/**
 *
 * Copyright 2019 Aditya Borikar
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
package org.jivesoftware.smackx.dataformmedia;

import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

public final class MediaElement implements ExtensionElement {

    private static final String NAMESPACE = "urn:xmpp:media-element";
    private static final String ELEMENT = "media";
    private final List<URINode> uriNodeList;
    private static final String ATTRIBUTE_HEIGHT = "height";
    private static final String ATTRIBUTE_WIDTH = "width";
    private final Integer height;
    private final Integer width;

    public MediaElement(List<URINode> uriNodeList) {
        if (uriNodeListSizeCheck(uriNodeList)) {
               this.uriNodeList = uriNodeList;
        }
        else {
            this.uriNodeList = null;
        }
        this.height = null;
        this.width = null;
    }

    public MediaElement(List<URINode> uriNodeList, int height, int width) {
        if (uriNodeListSizeCheck(uriNodeList)) {
            this.uriNodeList = uriNodeList;
        }
        else {
            this.uriNodeList = null;
        }
        this.height = height;
        this.width = width;
    }

    private boolean uriNodeListSizeCheck(List<URINode> uriNodeList) {
        if (uriNodeList.size() < 1) {
            throw new IllegalArgumentException("Atleast one URI element is needed");
        }
        return true;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        if (height != null && width != null) {
            xml.attribute(ATTRIBUTE_HEIGHT, height);
            xml.attribute(ATTRIBUTE_WIDTH, width);
        }
        xml.rightAngleBracket();
        xml.append(getMediaXML());
        return xml.closeElement(getElementName());
    }

    private XmlStringBuilder getMediaXML() {
        XmlStringBuilder mediaXML = new XmlStringBuilder();
        for (URINode node : uriNodeList) {
            mediaXML.element(node);
        }
        return mediaXML;
    }
}
