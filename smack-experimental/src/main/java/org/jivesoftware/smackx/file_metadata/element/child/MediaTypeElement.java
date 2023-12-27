/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.file_metadata.element.child;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * The media type of the file content, which SHOULD be a valid MIME-TYPE as registered with the
 * Internet Assigned Numbers Authority (IANA).
 *
 * @see <a href="http://www.iana.org/assignments/media-types">Media Types</a>
 */
public class MediaTypeElement implements NamedElement {

    public static final String ELEMENT = "media-type";

    private final String mediaType;

    public MediaTypeElement(String mediaType) {
        this.mediaType = StringUtils.requireNotNullNorEmpty(mediaType, "Media-Type MUST NOT be null nor empty");
    }

    /**
     * Return the media type of the file.
     *
     * @return media type
     */
    public String getMediaType() {
        return mediaType;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this)
                .rightAngleBracket()
                .append(getMediaType())
                .closeElement(this);
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public int hashCode() {
        return HashCode.builder()
                .append(getElementName())
                .append(getMediaType())
                .build();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsUtil.equals(this, obj, (equalsBuilder, other) ->
                equalsBuilder.append(getElementName(), other.getElementName())
                        .append(getMediaType(), other.getMediaType()));
    }
}
