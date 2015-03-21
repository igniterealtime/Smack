/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smack.packet;

import java.util.Locale;

import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * The base IQ (Info/Query) packet. IQ packets are used to get and set information
 * on the server, including authentication, roster operations, and creating
 * accounts. Each IQ stanza(/packet) has a specific type that indicates what type of action
 * is being taken: "get", "set", "result", or "error".<p>
 *
 * IQ packets can contain a single child element that exists in a specific XML
 * namespace. The combination of the element name and namespace determines what
 * type of IQ stanza(/packet) it is. Some example IQ subpacket snippets:<ul>
 *
 *  <li>&lt;query xmlns="jabber:iq:auth"&gt; -- an authentication IQ.
 *  <li>&lt;query xmlns="jabber:iq:private"&gt; -- a private storage IQ.
 *  <li>&lt;pubsub xmlns="http://jabber.org/protocol/pubsub"&gt; -- a pubsub IQ.
 * </ul>
 *
 * @author Matt Tucker
 */
public abstract class IQ extends Stanza {

    // Don't name this field 'ELEMENT'. When it comes to IQ, ELEMENT is the child element!
    public static final String IQ_ELEMENT = "iq";
    public static final String QUERY_ELEMENT = "query";

    private final String childElementName;
    private final String childElementNamespace;

    private Type type = Type.get;

    public IQ(IQ iq) {
        super(iq);
        type = iq.getType();
        this.childElementName = iq.childElementName;
        this.childElementNamespace = iq.childElementNamespace;
    }

    protected IQ(String childElementName) {
        this(childElementName, null);
    }

    protected IQ(String childElementName, String childElementNamespace) {
        this.childElementName = childElementName;
        this.childElementNamespace = childElementNamespace;
    }

    /**
     * Returns the type of the IQ packet.
     *
     * @return the type of the IQ packet.
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets the type of the IQ packet.
     * <p>
     * Since the type of an IQ must present, an IllegalArgmentException will be thrown when type is
     * <code>null</code>.
     * </p>
     *
     * @param type the type of the IQ packet.
     */
    public void setType(Type type) {
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    /**
     * Return true if this IQ is a request IQ, i.e. an IQ of type {@link Type#get} or {@link Type#set}.
     *
     * @return true if IQ type is 'get' or 'set', false otherwise.
     * @since 4.1
     */
    public boolean isRequestIQ() {
        switch (type) {
        case get:
        case set:
            return true;
        default:
            return false;
        }
    }

    public final String getChildElementName() {
        return childElementName;
    }

    public final String getChildElementNamespace() {
        return childElementNamespace;
    }

    @Override
    public final XmlStringBuilder toXML() {
        XmlStringBuilder buf = new XmlStringBuilder();
        buf.halfOpenElement(IQ_ELEMENT);
        addCommonAttributes(buf);
        if (type == null) {
            buf.attribute("type", "get");
        }
        else {
            buf.attribute("type", type.toString());
        }
        buf.rightAngleBracket();
        buf.append(getChildElementXML());
        buf.closeElement(IQ_ELEMENT);
        return buf;
    }

    /**
     * Returns the sub-element XML section of the IQ packet, or the empty String if there
     * isn't one.
     *
     * @return the child element section of the IQ XML.
     */
    public final XmlStringBuilder getChildElementXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        if (type == Type.error) {
            // Add the error sub-packet, if there is one.
            appendErrorIfExists(xml);
        }
        else if (childElementName != null) {
            // Add the query section if there is one.
            IQChildElementXmlStringBuilder iqChildElement = getIQChildElementBuilder(new IQChildElementXmlStringBuilder(this));
            if (iqChildElement != null) {
                xml.append(iqChildElement);
                XmlStringBuilder extensionsXml = getExtensionsXML();
                if (iqChildElement.isEmptyElement) {
                    if (extensionsXml.length() == 0) {
                         xml.closeEmptyElement();
                         return xml;
                    } else {
                        xml.rightAngleBracket();
                    }
                }
                xml.append(extensionsXml);
                xml.closeElement(iqChildElement.element);
            }
        }
        return xml;
    }

    /**
     * This method must be overwritten by IQ subclasses to create their child content. It is important that the builder
     * <b>does not include the final end element</b>. This will be done automatically by IQChildelementXmlStringBuilder
     * after eventual existing stanza(/packet) extensions have been added.
     * <p>
     * For example to create an IQ with a extra attribute and an additional child element
     * </p>
     * <pre>
     * {@code
     * <iq to='foo@example.org' id='123'>
     *   <bar xmlns='example:bar' extraAttribute='blaz'>
     *      <extraElement>elementText</extraElement>
     *   </bar>
     * </iq>
     * }
     * </pre>
     * the body of the {@code getIQChildElementBuilder} looks like
     * <pre>
     * {@code
     * // The builder 'xml' will already have the child element and the 'xmlns' attribute added
     * // So the current builder state is "<bar xmlns='example:bar'"
     * xml.attribute("extraAttribute", "blaz");
     * xml.rightAngleBracket();
     * xml.element("extraElement", "elementText");
     * // Do not close the 'bar' attribute by calling xml.closeElement('bar')
     * }
     * </pre>
     * If your IQ only contains attributes and no child elements, i.e. it can be represented as empty element, then you
     * can mark it as such.
     * <pre>
     * xml.attribute(&quot;myAttribute&quot;, &quot;myAttributeValue&quot;);
     * xml.setEmptyElement();
     * </pre>
     * If your IQ does not contain any attributes or child elements (besides stanza(/packet) extensions), consider sub-classing
     * {@link SimpleIQ} instead.
     * 
     * @param xml a pre-created builder which already has the child element and the 'xmlns' attribute set.
     * @return the build to create the IQ child content.
     */
    protected abstract IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml);

    /**
     * Convenience method to create a new empty {@link Type#result IQ.Type.result}
     * IQ based on a {@link Type#get IQ.Type.get} or {@link Type#set IQ.Type.set}
     * IQ. The new stanza(/packet) will be initialized with:<ul>
     *      <li>The sender set to the recipient of the originating IQ.
     *      <li>The recipient set to the sender of the originating IQ.
     *      <li>The type set to {@link Type#result IQ.Type.result}.
     *      <li>The id set to the id of the originating IQ.
     *      <li>No child element of the IQ element.
     * </ul>
     *
     * @param request the {@link Type#get IQ.Type.get} or {@link Type#set IQ.Type.set} IQ packet.
     * @throws IllegalArgumentException if the IQ stanza(/packet) does not have a type of
     *      {@link Type#get IQ.Type.get} or {@link Type#set IQ.Type.set}.
     * @return a new {@link Type#result IQ.Type.result} IQ based on the originating IQ.
     */
    public static IQ createResultIQ(final IQ request) {
        return new EmptyResultIQ(request);
    }

    /**
     * Convenience method to create a new {@link Type#error IQ.Type.error} IQ
     * based on a {@link Type#get IQ.Type.get} or {@link Type#set IQ.Type.set}
     * IQ. The new stanza(/packet) will be initialized with:<ul>
     *      <li>The sender set to the recipient of the originating IQ.
     *      <li>The recipient set to the sender of the originating IQ.
     *      <li>The type set to {@link Type#error IQ.Type.error}.
     *      <li>The id set to the id of the originating IQ.
     *      <li>The child element contained in the associated originating IQ.
     *      <li>The provided {@link XMPPError XMPPError}.
     * </ul>
     *
     * @param request the {@link Type#get IQ.Type.get} or {@link Type#set IQ.Type.set} IQ packet.
     * @param error the error to associate with the created IQ packet.
     * @throws IllegalArgumentException if the IQ stanza(/packet) does not have a type of
     *      {@link Type#get IQ.Type.get} or {@link Type#set IQ.Type.set}.
     * @return a new {@link Type#error IQ.Type.error} IQ based on the originating IQ.
     */
    public static ErrorIQ createErrorResponse(final IQ request, final XMPPError error) {
        if (!(request.getType() == Type.get || request.getType() == Type.set)) {
            throw new IllegalArgumentException(
                    "IQ must be of type 'set' or 'get'. Original IQ: " + request.toXML());
        }
        final ErrorIQ result = new ErrorIQ(error);
        result.setStanzaId(request.getStanzaId());
        result.setFrom(request.getTo());
        result.setTo(request.getFrom());
        return result;
    }

    /**
     * A enum to represent the type of the IQ stanza.
     */
    public enum Type {

        /**
         * The IQ stanza requests information, inquires about what data is needed in order to complete further operations, etc.
         */
        get,

        /**
         * The IQ stanza provides data that is needed for an operation to be completed, sets new values, replaces existing values, etc.
         */
        set,

        /**
         * The IQ stanza is a response to a successful get or set request.
         */
        result,

        /**
         * The IQ stanza reports an error that has occurred regarding processing or delivery of a get or set request.
         */
        error,
        ;

        /**
         * Converts a String into the corresponding types. Valid String values
         * that can be converted to types are: "get", "set", "result", and "error".
         *
         * @param string the String value to covert.
         * @return the corresponding Type.
         * @throws IllegalArgumentException when not able to parse the string parameter
         * @throws NullPointerException if the string is null
         */
        public static Type fromString(String string) {
            return Type.valueOf(string.toLowerCase(Locale.US));
        }
    }

    public static class IQChildElementXmlStringBuilder extends XmlStringBuilder {
        private final String element;

        private boolean isEmptyElement;

        private IQChildElementXmlStringBuilder(IQ iq) {
            this(iq.getChildElementName(), iq.getChildElementNamespace());
        }

        public IQChildElementXmlStringBuilder(ExtensionElement pe) {
            this(pe.getElementName(), pe.getNamespace());
        }

        private IQChildElementXmlStringBuilder(String element, String namespace) {
            prelude(element, namespace);
            this.element = element;
        }

        public void setEmptyElement() {
            isEmptyElement = true;
        }
    }
}
