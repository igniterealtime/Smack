/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smackx.mediaelement.element;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.datatypes.UInt16;
import org.jivesoftware.smack.packet.FullyQualifiedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.FormFieldChildElement;

public class MediaElement implements FormFieldChildElement {

    public static final String ELEMENT = "media";

    public static final String NAMESPACE = "urn:xmpp:media-element";

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    private final UInt16 height;

    private final UInt16 width;

    private final List<Uri> uris;

    public MediaElement(Builder builder) {
        this.height = builder.height;
        this.width = builder.width;
        this.uris = Collections.unmodifiableList(builder.uris);
    }

    public UInt16 getHeight() {
        return height;
    }

    public UInt16 getWidth() {
        return width;
    }

    public List<Uri> getUris() {
        return uris;
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
    public QName getQName() {
        return QNAME;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
        xml.optAttribute("height", height)
            .optAttribute("width", width)
            .rightAngleBracket();

        xml.append(uris);

        xml.closeElement(this);
        return xml;
    }

    public MediaElement from(FormField formField) {
        return (MediaElement) formField.getFormFieldChildElement(QNAME);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UInt16 height, width;

        private List<Uri> uris = new ArrayList<>();

        public Builder setHeightAndWidth(int height, int width) {
            return setHeightAndWidth(UInt16.from(height), UInt16.from(width));
        }

        public Builder setHeightAndWidth(UInt16 height, UInt16 width) {
            this.height = height;
            this.width = width;
            return this;
        }

        public Builder addUri(URI uri, String type) {
            return addUri(new Uri(uri, type));
        }

        public Builder addUri(Uri uri) {
            uris.add(uri);
            return this;
        }

        public MediaElement build() {
            return new MediaElement(this);
        }
    }

    public static final class Uri implements FullyQualifiedElement {
        public static final String ELEMENT = "uri";

        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final URI uri;
        private final String type;

        public Uri(URI uri, String type) {
            this.uri = Objects.requireNonNull(uri);
            this.type = StringUtils.requireNotNullNorEmpty(type, "The 'type' argument must not be null or empty");
        }

        public URI getUri() {
            return uri;
        }

        public String getType() {
            return type;
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
        public QName getQName() {
            return QNAME;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.attribute("type", type)
               .rightAngleBracket();
            xml.escape(uri.toString());
            xml.closeElement(this);
            return xml;
        }

    }
}
