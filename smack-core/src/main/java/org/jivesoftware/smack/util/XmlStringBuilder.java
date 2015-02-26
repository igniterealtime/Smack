/**
 *
 * Copyright 2014 Florian Schmaus
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
package org.jivesoftware.smack.util;

import java.util.Collection;

import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.ExtensionElement;

public class XmlStringBuilder implements Appendable, CharSequence {
    public static final String RIGHT_ANGLE_BRACKET = Character.toString('>');

    private final LazyStringBuilder sb;

    public XmlStringBuilder() {
        sb = new LazyStringBuilder();
    }

    public XmlStringBuilder(ExtensionElement pe) {
        this();
        prelude(pe);
    }

    public XmlStringBuilder(NamedElement e) {
        this();
        halfOpenElement(e.getElementName());
    }

    public XmlStringBuilder escapedElement(String name, String escapedContent) {
        assert escapedContent != null;
        openElement(name);
        append(escapedContent);
        closeElement(name);
        return this;
    }

    /**
     *
     * @param name
     * @param content
     * @return the XmlStringBuilder
     */
    public XmlStringBuilder element(String name, String content) {
        assert content != null;
        openElement(name);
        escape(content);
        closeElement(name);
        return this;
    }

    public XmlStringBuilder element(String name, Enum<?> content) {
        assert content != null;
        element(name, content.name());
        return this;
    }

    public XmlStringBuilder element(Element element) {
        assert element != null;
        return append(element.toXML());
    }

    public XmlStringBuilder optElement(String name, String content) {
        if (content != null) {
            element(name, content);
        }
        return this;
    }

    public XmlStringBuilder optElement(Element element) {
        if (element != null) {
            append(element.toXML());
        }
        return this;
    }

    public XmlStringBuilder optElement(String name, Enum<?> content) {
        if (content != null) {
            element(name, content);
        }
        return this;
    }

    public XmlStringBuilder optIntElement(String name, int value) {
        if (value >= 0) {
            element(name, String.valueOf(value));
        }
        return this;
    }

    public XmlStringBuilder halfOpenElement(String name) {
        assert(StringUtils.isNotEmpty(name));
        sb.append('<').append(name);
        return this;
    }

    public XmlStringBuilder halfOpenElement(NamedElement namedElement) {
        return halfOpenElement(namedElement.getElementName());
    }

    public XmlStringBuilder openElement(String name) {
        halfOpenElement(name).rightAngleBracket();
        return this;
    }

    public XmlStringBuilder closeElement(String name) {
        sb.append("</").append(name);
        rightAngleBracket();
        return this;
    }

    public XmlStringBuilder closeElement(NamedElement e) {
        closeElement(e.getElementName());
        return this;
    }

    public XmlStringBuilder closeEmptyElement() {
        sb.append("/>");
        return this;
    }

    /**
     * Add a right angle bracket '>'
     * 
     * @return a reference to this object.
     */
    public XmlStringBuilder rightAngleBracket() {
        sb.append(RIGHT_ANGLE_BRACKET);
        return this;
    }

    /**
     * 
     * @return a reference to this object
     * @deprecated use {@link #rightAngleBracket()} instead
     */
    @Deprecated
    public XmlStringBuilder rightAngelBracket() {
        return rightAngleBracket();
    }

    /**
     * Does nothing if value is null.
     *
     * @param name
     * @param value
     * @return the XmlStringBuilder
     */
    public XmlStringBuilder attribute(String name, String value) {
        assert value != null;
        sb.append(' ').append(name).append("='");
        escape(value);
        sb.append('\'');
        return this;
    }

    public XmlStringBuilder attribute(String name, Enum<?> value) {
        assert value != null;
        attribute(name, value.name());
        return this;
    }

    public XmlStringBuilder attribute(String name, int value) {
        assert name != null;
        return attribute(name, String.valueOf(value));
    }

    public XmlStringBuilder optAttribute(String name, String value) {
        if (value != null) {
            attribute(name, value);
        }
        return this;
    }

    public XmlStringBuilder optAttribute(String name, Enum<?> value) {
        if (value != null) {
            attribute(name, value.toString());
        }
        return this;
    }

    /**
     * Add the given attribute if value => 0
     *
     * @param name
     * @param value
     * @return a reference to this object
     */
    public XmlStringBuilder optIntAttribute(String name, int value) {
        if (value >= 0) {
            attribute(name, Integer.toString(value));
        }
        return this;
    }

    /**
     * Add the given attribute if value not null and value => 0.
     *
     * @param name
     * @param value
     * @return a reference to this object
     */
    public XmlStringBuilder optLongAttribute(String name, Long value) {
        if (value != null && value >= 0) {
            attribute(name, Long.toString(value));
        }
        return this;
    }

    public XmlStringBuilder optBooleanAttribute(String name, boolean bool) {
        if (bool) {
            sb.append(' ').append(name).append("='true'");
        }
        return this;
    }

    public XmlStringBuilder xmlnsAttribute(String value) {
        optAttribute("xmlns", value);
        return this;
    }

    public XmlStringBuilder xmllangAttribute(String value) {
        optAttribute("xml:lang", value);
        return this;
    }
 
    public XmlStringBuilder escape(String text) {
        assert text != null;
        sb.append(StringUtils.escapeForXML(text));
        return this;
    }

    public XmlStringBuilder prelude(ExtensionElement pe) {
        return prelude(pe.getElementName(), pe.getNamespace());
    }

    public XmlStringBuilder prelude(String elementName, String namespace) {
        halfOpenElement(elementName);
        xmlnsAttribute(namespace);
        return this;
    }

    public XmlStringBuilder optAppend(CharSequence csq) {
        if (csq != null) {
            append(csq);
        }
        return this;
    }

    public XmlStringBuilder optAppend(Element element) {
        if (element != null) {
            append(element.toXML());
        }
        return this;
    }

    public XmlStringBuilder append(XmlStringBuilder xsb) {
        assert xsb != null;
        sb.append(xsb.sb);
        return this;
    }

    public XmlStringBuilder append(Collection<? extends Element> elements) {
        for (Element element : elements) {
            append(element.toXML());
        }
        return this;
    }

    public XmlStringBuilder emptyElement(Enum<?> element) {
        return emptyElement(element.name());
    }

    public XmlStringBuilder emptyElement(String element) {
        halfOpenElement(element);
        return closeEmptyElement();
    }

    public XmlStringBuilder condEmptyElement(boolean condition, String element) {
        if (condition) {
            emptyElement(element);
        }
        return this;
    }

    public XmlStringBuilder condAttribute(boolean condition, String name, String value) {
        if (condition) {
            attribute(name, value);
        }
        return this;
    }

    @Override
    public XmlStringBuilder append(CharSequence csq) {
        assert csq != null;
        sb.append(csq);
        return this;
    }

    @Override
    public XmlStringBuilder append(CharSequence csq, int start, int end) {
        assert csq != null;
        sb.append(csq, start, end);
        return this;
    }

    @Override
    public XmlStringBuilder append(char c) {
        sb.append(c);
        return this;
    }

    @Override
    public int length() {
        return sb.length();
    }

    @Override
    public char charAt(int index) {
        return sb.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return sb.subSequence(start, end);
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CharSequence)) {
            return false;
        }
        CharSequence otherCharSequenceBuilder = (CharSequence) other;
        return toString().equals(otherCharSequenceBuilder.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
