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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.reference.ReferenceManager;

import org.jxmpp.jid.BareJid;

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
    private final URI uri;

    // Non-XEP-compliant, but needed for SIMS
    private final ExtensionElement child;

    /**
     * XEP-incompliant (v0.2) constructor. This is needed for SIMS.
     *
     * @param begin TODO javadoc me please
     * @param end TODO javadoc me please
     * @param type TODO javadoc me please
     * @param anchor TODO javadoc me please
     * @param uri TODO javadoc me please
     * @param child TODO javadoc me please
     */
    public ReferenceElement(Integer begin, Integer end, Type type, String anchor, URI uri, ExtensionElement child) {
        if (begin != null && begin < 0) {
            throw new IllegalArgumentException("Attribute 'begin' MUST NOT be smaller than 0.");
        }
        if (end != null && end < 0) {
            throw new IllegalArgumentException("Attribute 'end' MUST NOT be smaller than 0.");
        }
        if (begin != null && end != null && begin >= end) {
            throw new IllegalArgumentException("Attribute 'begin' MUST be smaller than attribute 'end'.");
        }
        Objects.requireNonNull(type);
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
     * @param begin TODO javadoc me please
     * @param end TODO javadoc me please
     * @param type TODO javadoc me please
     * @param anchor TODO javadoc me please
     * @param uri TODO javadoc me please
     */
    public ReferenceElement(Integer begin, Integer end, Type type, String anchor, URI uri) {
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

    public URI getUri() {
        return uri;
    }

    /**
     * Add a reference to another users bare jid to a stanza.
     *
     * @param stanza stanza.
     * @param begin start index of the mention in the messages body.
     * @param end end index of the mention in the messages body.
     * @param jid referenced jid.
     */
    public static void addMention(Stanza stanza, int begin, int end, BareJid jid) {
        URI uri;
        try {
            uri = new URI("xmpp:" + jid.toString());
        } catch (URISyntaxException e) {
            throw new AssertionError("Cannot create URI from bareJid.");
        }
        ReferenceElement reference = new ReferenceElement(begin, end, ReferenceElement.Type.mention, null, uri);
        stanza.addExtension(reference);
    }

    /**
     * Return a list of all reference extensions contained in a stanza.
     * If there are no reference elements, return an empty list.
     *
     * @param stanza stanza
     * @return list of all references contained in the stanza
     */
    public static List<ReferenceElement> getReferencesFromStanza(Stanza stanza) {
        List<ReferenceElement> references = new ArrayList<>();
        List<ExtensionElement> extensions = stanza.getExtensions(ReferenceElement.ELEMENT, ReferenceManager.NAMESPACE);
        for (ExtensionElement e : extensions) {
            references.add((ReferenceElement) e);
        }
        return references;
    }

    /**
     * Return true, if the stanza contains at least one reference extension.
     *
     * @param stanza stanza
     * @return true if stanza contains references
     */
    public static boolean containsReferences(Stanza stanza) {
        return getReferencesFromStanza(stanza).size() > 0;
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
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this)
                .optIntAttribute(ATTR_BEGIN, begin != null ? begin : -1)
                .optIntAttribute(ATTR_END, end != null ? end : -1)
                .attribute(ATTR_TYPE, type.toString())
                .optAttribute(ATTR_ANCHOR, anchor)
                .optAttribute(ATTR_URI, uri != null ? uri.toString() : null);

        if (child == null) {
            return xml.closeEmptyElement();
        } else {
            return xml.rightAngleBracket()
                    .append(child.toXML())
                    .closeElement(this);
        }
    }
}
