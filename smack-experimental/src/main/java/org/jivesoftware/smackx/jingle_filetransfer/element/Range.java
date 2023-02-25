/**
 *
 * Copyright © 2017 Paul Schaub, 2019 Florian Schmaus
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
package org.jivesoftware.smackx.jingle_filetransfer.element;

import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.hashes.element.HashElement;

/**
 * RangeElement which specifies, which range of a file shall be transferred.
 */
public class Range implements XmlElement {

    public static final String ELEMENT = "range";
    public static final String NAMESPACE = JingleFileTransferChild.NAMESPACE;

    public static final String ATTR_OFFSET = "offset";
    public static final String ATTR_LENGTH = "length";

    private final Integer offset, length;
    private final HashElement hash;

    /**
     * Create a Range element with default values.
     */
    public Range() {
        this(null, null, null);
    }

    /**
     * Create a Range element with specified length.
     * @param length length of the transmitted data in bytes.
     */
    public Range(int length) {
        this(null, length, null);
    }

    /**
     * Create a Range element with specified offset and length.
     * @param offset offset in bytes from the beginning of the transmitted data.
     * @param length number of bytes that shall be transferred.
     */
    public Range(int offset, int length) {
        this(offset, length, null);
    }

    /**
     * Create a Range element with specified offset, length and hash.
     * @param offset offset in bytes from the beginning of the transmitted data.
     * @param length number of bytes that shall be transferred.
     * @param hash hash of the bytes in the specified range.
     */
    public Range(Integer offset, Integer length, HashElement hash) {
        this.offset = offset;
        this.length = length;
        this.hash = hash;
    }

    /**
     * Return the index of the offset.
     * This marks the begin of the specified range.
     * @return offset TODO javadoc me please
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Return the length of the range.
     * @return length TODO javadoc me please
     */
    public int getLength() {
        return length;
    }

    /**
     * Return the hash element that contains a checksum of the bytes specified in the range.
     * @return hash element
     */
    public HashElement getHash() {
        return hash;
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
    public CharSequence toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder sb =  new XmlStringBuilder(this);

        sb.optAttribute(ATTR_OFFSET, offset);
        sb.optAttribute(ATTR_LENGTH, length);

        if (hash != null) {
            sb.rightAngleBracket();
            sb.append(hash);
            sb.closeElement(this);
        } else {
            sb.closeEmptyElement();
        }
        return sb;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof Range)) {
            return false;
        }

        Range otherRange = (Range) other;
        return this.getOffset() == otherRange.getOffset() &&
            this.getLength() == otherRange.getLength() &&
            this.getHash().equals(otherRange.getHash());
    }

    @Override
    public int hashCode() {
        return toXML().toString().hashCode();
    }
}
