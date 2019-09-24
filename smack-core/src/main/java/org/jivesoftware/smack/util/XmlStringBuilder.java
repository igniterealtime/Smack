/**
 *
 * Copyright 2014-2019 Florian Schmaus
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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.FullyQualifiedElement;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;

import org.jxmpp.util.XmppDateTime;

public class XmlStringBuilder implements Appendable, CharSequence, Element {
    public static final String RIGHT_ANGLE_BRACKET = Character.toString('>');

    private final LazyStringBuilder sb;

    private final XmlEnvironment effectiveXmlEnvironment;

    public XmlStringBuilder() {
        sb = new LazyStringBuilder();
        effectiveXmlEnvironment = null;
    }

    public XmlStringBuilder(ExtensionElement pe) {
        this(pe, null);
    }

    public XmlStringBuilder(NamedElement e) {
        this();
        halfOpenElement(e.getElementName());
    }

    public XmlStringBuilder(FullyQualifiedElement element, XmlEnvironment enclosingXmlEnvironment) {
        sb = new LazyStringBuilder();
        halfOpenElement(element);

        String xmlNs = element.getNamespace();
        String xmlLang = element.getLanguage();
        if (enclosingXmlEnvironment == null) {
            xmlnsAttribute(xmlNs);
            xmllangAttribute(xmlLang);
        } else {
            if (!enclosingXmlEnvironment.effectiveNamespaceEquals(xmlNs)) {
                xmlnsAttribute(xmlNs);
            }
            if (!enclosingXmlEnvironment.effectiveLanguageEquals(xmlLang)) {
                xmllangAttribute(xmlLang);
            }
        }

        effectiveXmlEnvironment = XmlEnvironment.builder()
                .withNamespace(xmlNs)
                .withLanguage(xmlLang)
                .withNext(enclosingXmlEnvironment)
                .build();
    }

    public XmlStringBuilder escapedElement(String name, String escapedContent) {
        assert escapedContent != null;
        openElement(name);
        append(escapedContent);
        closeElement(name);
        return this;
    }

    /**
     * Add a new element to this builder.
     *
     * @param name TODO javadoc me please
     * @param content TODO javadoc me please
     * @return the XmlStringBuilder
     */
    public XmlStringBuilder element(String name, String content) {
        if (content.isEmpty()) {
            return emptyElement(name);
        }
        openElement(name);
        escape(content);
        closeElement(name);
        return this;
    }

    /**
     * Add a new element to this builder, with the {@link java.util.Date} instance as its content,
     * which will get formatted with {@link XmppDateTime#formatXEP0082Date(Date)}.
     *
     * @param name element name
     * @param content content of element
     * @return this XmlStringBuilder
     */
    public XmlStringBuilder element(String name, Date content) {
        assert content != null;
        return element(name, XmppDateTime.formatXEP0082Date(content));
    }

   /**
    * Add a new element to this builder.
    *
    * @param name TODO javadoc me please
    * @param content TODO javadoc me please
    * @return the XmlStringBuilder
    */
   public XmlStringBuilder element(String name, CharSequence content) {
       return element(name, content.toString());
   }

    public XmlStringBuilder element(String name, Enum<?> content) {
        assert content != null;
        element(name, content.toString());
        return this;
    }

    /**
     * Deprecated.
     *
     * @param element deprecated.
     * @return deprecated.
     * @deprecated use {@link #append(Element)} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
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

    /**
     * Add a new element to this builder, with the {@link java.util.Date} instance as its content,
     * which will get formatted with {@link XmppDateTime#formatXEP0082Date(Date)}
     * if {@link java.util.Date} instance is not <code>null</code>.
     *
     * @param name element name
     * @param content content of element
     * @return this XmlStringBuilder
     */
    public XmlStringBuilder optElement(String name, Date content) {
        if (content != null) {
            element(name, content);
        }
        return this;
    }

    public XmlStringBuilder optElement(String name, CharSequence content) {
        if (content != null) {
            element(name, content.toString());
        }
        return this;
    }

    public XmlStringBuilder optElement(Element element) {
        if (element != null) {
            append(element);
        }
        return this;
    }

    public XmlStringBuilder optElement(String name, Enum<?> content) {
        if (content != null) {
            element(name, content);
        }
        return this;
    }

    public XmlStringBuilder optElement(String name, Object object) {
        if (object != null) {
            element(name, object.toString());
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
        assert StringUtils.isNotEmpty(name);
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
     * Add a right angle bracket '&gt;'.
     *
     * @return a reference to this object.
     */
    public XmlStringBuilder rightAngleBracket() {
        sb.append(RIGHT_ANGLE_BRACKET);
        return this;
    }

    /**
     * Add a right angle bracket '&gt;'.
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
     * @param name TODO javadoc me please
     * @param value TODO javadoc me please
     * @return the XmlStringBuilder
     */
    public XmlStringBuilder attribute(String name, String value) {
        assert value != null;
        sb.append(' ').append(name).append("='");
        escapeAttributeValue(value);
        sb.append('\'');
        return this;
    }

    public XmlStringBuilder attribute(String name, boolean bool) {
        return attribute(name, Boolean.toString(bool));
    }

    /**
     * Add a new attribute to this builder, with the {@link java.util.Date} instance as its value,
     * which will get formatted with {@link XmppDateTime#formatXEP0082Date(Date)}.
     *
     * @param name name of attribute
     * @param value value of attribute
     * @return this XmlStringBuilder
     */
    public XmlStringBuilder attribute(String name, Date value) {
        assert value != null;
        return attribute(name, XmppDateTime.formatXEP0082Date(value));
    }

    public XmlStringBuilder attribute(String name, CharSequence value) {
        return attribute(name, value.toString());
    }

    public XmlStringBuilder attribute(String name, Enum<?> value) {
        assert value != null;
        // TODO: Should use toString() instead of name().
        attribute(name, value.name());
        return this;
    }

    public <E extends Enum<?>> XmlStringBuilder attribute(String name, E value, E implicitDefault) {
        if (value == null || value == implicitDefault) {
            return this;
        }

        attribute(name, value.toString());
        return this;
    }

    public XmlStringBuilder attribute(String name, int value) {
        assert name != null;
        return attribute(name, String.valueOf(value));
    }

    public XmlStringBuilder attribute(String name, long value) {
        assert name != null;
        return attribute(name, String.valueOf(value));
    }

    public XmlStringBuilder optAttribute(String name, String value) {
        if (value != null) {
            attribute(name, value);
        }
        return this;
    }

    public XmlStringBuilder optAttribute(String name, Long value) {
        if (value != null) {
            attribute(name, value);
        }
        return this;
    }

    /**
     * Add a new attribute to this builder, with the {@link java.util.Date} instance as its value,
     * which will get formatted with {@link XmppDateTime#formatXEP0082Date(Date)}
     * if {@link java.util.Date} instance is not <code>null</code>.
     *
     * @param name attribute name
     * @param value value of this attribute
     * @return this XmlStringBuilder
     */
    public XmlStringBuilder optAttribute(String name, Date value) {
        if (value != null) {
            attribute(name, value);
        }
        return this;
    }

    public XmlStringBuilder optAttribute(String name, CharSequence value) {
        if (value != null) {
            attribute(name, value.toString());
        }
        return this;
    }

    public XmlStringBuilder optAttribute(String name, Enum<?> value) {
        if (value != null) {
            attribute(name, value.toString());
        }
        return this;
    }

    public XmlStringBuilder optAttribute(String name, Number number) {
        if (number != null) {
            attribute(name, number.toString());
        }
        return this;
    }

    /**
     * Add the given attribute if {@code value => 0}.
     *
     * @param name TODO javadoc me please
     * @param value TODO javadoc me please
     * @return a reference to this object
     */
    public XmlStringBuilder optIntAttribute(String name, int value) {
        if (value >= 0) {
            attribute(name, Integer.toString(value));
        }
        return this;
    }

    /**
     * Add the given attribute if value not null and {@code value => 0}.
     *
     * @param name TODO javadoc me please
     * @param value TODO javadoc me please
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

    public XmlStringBuilder optBooleanAttributeDefaultTrue(String name, boolean bool) {
        if (!bool) {
            sb.append(' ').append(name).append("='false'");
        }
        return this;
    }

    private static final class XmlNsAttribute implements CharSequence {
        private final String value;
        private final String xmlFragment;

        private XmlNsAttribute(String value) {
            this.value = StringUtils.requireNotNullNorEmpty(value, "Value must not be null");
            this.xmlFragment = " xmlns='" + value + '\'';
        }

        @Override
        public String toString() {
            return xmlFragment;
        }

        @Override
        public int length() {
            return xmlFragment.length();
        }

        @Override
        public char charAt(int index) {
            return xmlFragment.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return xmlFragment.subSequence(start, end);
        }
    }

    public XmlStringBuilder xmlnsAttribute(String value) {
        if (value == null || (effectiveXmlEnvironment != null
                        && effectiveXmlEnvironment.effectiveNamespaceEquals(value))) {
            return this;
        }
        XmlNsAttribute xmlNsAttribute = new XmlNsAttribute(value);
        append(xmlNsAttribute);
        return this;
    }

    public XmlStringBuilder xmllangAttribute(String value) {
        // TODO: This should probably be attribute(), not optAttribute().
        optAttribute("xml:lang", value);
        return this;
    }

    public XmlStringBuilder optXmlLangAttribute(String lang) {
        if (!StringUtils.isNullOrEmpty(lang)) {
            xmllangAttribute(lang);
        }
        return this;
    }

    public XmlStringBuilder escape(String text) {
        assert text != null;
        sb.append(StringUtils.escapeForXml(text));
        return this;
    }

    public XmlStringBuilder escapeAttributeValue(String value) {
        assert value != null;
        sb.append(StringUtils.escapeForXmlAttributeApos(value));
        return this;
    }

    public XmlStringBuilder optEscape(CharSequence text) {
        if (text == null) {
            return this;
        }
        return escape(text);
    }

    public XmlStringBuilder escape(CharSequence text) {
        return escape(text.toString());
    }

    protected XmlStringBuilder prelude(FullyQualifiedElement pe) {
        return prelude(pe.getElementName(), pe.getNamespace());
    }

    protected XmlStringBuilder prelude(String elementName, String namespace) {
        halfOpenElement(elementName);
        xmlnsAttribute(namespace);
        return this;
    }

    public XmlStringBuilder optAppend(Element element) {
        if (element != null) {
            append(element.toXML(effectiveXmlEnvironment));
        }
        return this;
    }

    public XmlStringBuilder optTextChild(CharSequence sqc, NamedElement parentElement) {
        if (sqc == null) {
            return closeEmptyElement();
        }
        rightAngleBracket();
        escape(sqc);
        closeElement(parentElement);
        return this;
    }

    public XmlStringBuilder append(XmlStringBuilder xsb) {
        assert xsb != null;
        sb.append(xsb.sb);
        return this;
    }

    public XmlStringBuilder append(Element element) {
        return append(element.toXML(effectiveXmlEnvironment));
    }

    public XmlStringBuilder append(Collection<? extends Element> elements) {
        for (Element element : elements) {
            append(element);
        }
        return this;
    }

    public XmlStringBuilder emptyElement(Enum<?> element) {
        // Use Enum.toString() instead Enum.name() here, since some enums override toString() in order to replace
        // underscores ('_') with dash ('-') for example (name() is declared final in Enum).
        return emptyElement(element.toString());
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

    private static final class WrappedIoException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private final IOException wrappedIoException;

        private WrappedIoException(IOException wrappedIoException) {
            this.wrappedIoException = wrappedIoException;
        }
    }

    /**
     * Write the contents of this <code>XmlStringBuilder</code> to a {@link Writer}. This will write
     * the single parts one-by-one, avoiding allocation of a big continuous memory block holding the
     * XmlStringBuilder contents.
     *
     * @param writer TODO javadoc me please
     * @param enclosingXmlEnvironment the enclosing XML environment.
     * @throws IOException if an I/O error occured.
     */
    public void write(Writer writer, XmlEnvironment enclosingXmlEnvironment) throws IOException {
        try {
            appendXmlTo(csq -> {
                try {
                    writer.append(csq);
                } catch (IOException e) {
                    throw new WrappedIoException(e);
                }
            }, enclosingXmlEnvironment);
        } catch (WrappedIoException e) {
            throw e.wrappedIoException;
        }
    }

    public List<CharSequence> toList(XmlEnvironment enclosingXmlEnvironment) {
        List<CharSequence> res = new ArrayList<>(sb.getAsList().size());

        appendXmlTo(csq -> res.add(csq), enclosingXmlEnvironment);

        return res;
    }

    @Override
    public StringBuilder toXML(XmlEnvironment enclosingXmlEnvironment) {
        // This is only the potential length, since the actual length depends on the given XmlEnvironment.
        int potentialLength = length();
        StringBuilder res = new StringBuilder(potentialLength);

        appendXmlTo(csq -> res.append(csq), enclosingXmlEnvironment);

        return res;
    }

    private void appendXmlTo(Consumer<CharSequence> charSequenceSink, XmlEnvironment enclosingXmlEnvironment) {
        for (CharSequence csq : sb.getAsList()) {
            if (csq instanceof XmlStringBuilder) {
                ((XmlStringBuilder) csq).appendXmlTo(charSequenceSink, enclosingXmlEnvironment);
            }
            else if (csq instanceof XmlNsAttribute) {
                XmlNsAttribute xmlNsAttribute = (XmlNsAttribute) csq;
                if (!xmlNsAttribute.value.equals(enclosingXmlEnvironment.getEffectiveNamespace())) {
                    charSequenceSink.accept(xmlNsAttribute);
                    enclosingXmlEnvironment = new XmlEnvironment(xmlNsAttribute.value);
                }
            }
            else {
                charSequenceSink.accept(csq);
            }
        }
    }
}
