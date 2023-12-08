/**
 *
 * Copyright 2023 Paul Schaub
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
package org.jivesoftware.smackx.thumbnails.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class ThumbnailElement implements ExtensionElement {

    public static final String ELEMENT = "thumbnail";
    public static final String NAMESPACE = "urn:xmpp:thumbs:1";
    public static final String ELEM_URI = "uri";
    public static final String ELEM_MEDIA_TYPE = "media-type";
    public static final String ELEM_WIDTH = "width";
    public static final String ELEM_HEIGHT = "height";

    private final String uri;
    private final String mediaType;
    private final Integer width;
    private final Integer height;

    public ThumbnailElement(String uri) {
        this(uri, null, null, null);
    }

    public ThumbnailElement(String uri, String mediaType, Integer width, Integer height) {
        this.uri = Objects.requireNonNull(uri);
        this.mediaType = mediaType;

        if (width != null && width < 0) {
            throw new IllegalArgumentException("Width cannot be negative.");
        }
        this.width = width;

        if (height != null && height < 0) {
            throw new IllegalArgumentException("Height cannot be negative.");
        }
        this.height = height;
    }

    public String getUri() {
        return uri;
    }

    public String getMediaType() {
        return mediaType;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    @Override
    public CharSequence toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder sb = new XmlStringBuilder(this, xmlEnvironment);
        return sb.attribute(ELEM_URI, uri)
                .optAttribute(ELEM_MEDIA_TYPE, mediaType)
                .optAttribute(ELEM_WIDTH, width)
                .optAttribute(ELEM_HEIGHT, height)
                .closeEmptyElement();
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }
}
