/**
 *
 * Copyright 2018 Paul Schaub
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
package org.jivesoftware.smackx.reference.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.reference.ReferenceManager;

public class ReferenceElement implements ExtensionElement {

    public static final String ELEMENT = "reference";
    public static final String ATTR_BEGIN = "begin";
    public static final String ATTR_END = "end";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_ANCHOR = "anchor";
    public static final String ATTR_URI = "uri";

    public enum Type {
        mention,
        data
    }

    private final Integer begin;
    private final Integer end;
    private final Type type;
    private final String anchor;
    private final String uri;

    // Non-XEP-compliant, but needed for SIMS
    private final ExtensionElement child;

    /**
     * XEP-incompliant (v0.2) constructor. This is needed for SIMS.
     *
     * @param begin
     * @param end
     * @param type
     * @param anchor
     * @param uri
     * @param child
     */
    public ReferenceElement(Integer begin, Integer end, Type type, String anchor, String uri, ExtensionElement child) {
        if (begin != null && begin < 0) {
            throw new IllegalArgumentException("Attribute 'begin' MUST NOT be smaller than 0.");
        }
        if (end != null && end < 0) {
            throw new IllegalArgumentException("Attribute 'end' MUST NOT be smaller than 0.");
        }
        if (begin != null && end != null && begin >= end) {
            throw new IllegalArgumentException("Attribute 'begin' MUST be smaller than attribute 'end'.");
        }
        if (type == null) {
            throw new NullPointerException("Attribute 'type' MUST NOT be null.");
        }
        // TODO: The uri attribute is not mandatory according to SIMS, but it is according to references.
        /*if (uri == null) {
            throw new NullPointerException("Attribute 'uri' MUST NOT be null.");
        }*/
        this.begin = begin;
        this.end = end;
        this.type = type;
        this.anchor = anchor;
        this.uri = uri;
        this.child = child;
    }

    /**
     * XEP-Compliant constructor.
     *
     * @param begin
     * @param end
     * @param type
     * @param anchor
     * @param uri
     */
    public ReferenceElement(Integer begin, Integer end, Type type, String anchor, String uri) {
        this(begin, end, type, anchor, uri, null);
    }

    public Integer getBegin() {
        return begin;
    }

    public Integer getEnd() {
        return end;
    }

    public Type getType() {
        return type;
    }

    public String getAnchor() {
        return anchor;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public String getNamespace() {
        return ReferenceManager.NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this)
                .optIntAttribute(ATTR_BEGIN, begin != null ? begin : -1)
                .optIntAttribute(ATTR_END, end != null ? end : -1)
                .attribute(ATTR_TYPE, type.toString())
                .optAttribute(ATTR_ANCHOR, anchor)
                .optAttribute(ATTR_URI, uri);

        if (child == null) {
            return xml.closeEmptyElement();
        } else {
            return xml.rightAngleBracket()
                    .append(child.toXML())
                    .closeElement(this);
        }
    }
}
