/**
 *
 * Copyright 2003-2007 Jive Software, 2019 Florian Schmaus.
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.compress.packet.Compress;
import org.jivesoftware.smack.packet.EmptyResultIQ;
import org.jivesoftware.smack.packet.ErrorIQ;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.PresenceBuilder;
import org.jivesoftware.smack.packet.Session;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.StartTls;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.packet.UnparsedIQ;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.parsing.StandardExtensionElementProvider;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.xml.SmackXmlParser;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.jid.Jid;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Utility class that helps to parse packets. Any parsing packets method that must be shared
 * between many clients must be placed in this utility class.
 *
 * @author Gaston Dombiak
 */
public class PacketParserUtils {
    private static final Logger LOGGER = Logger.getLogger(PacketParserUtils.class.getName());

    // TODO: Rename argument name from 'stanza' to 'element'.
    public static XmlPullParser getParserFor(String stanza) throws XmlPullParserException, IOException {
        return getParserFor(new StringReader(stanza));
    }

    public static XmlPullParser getParserFor(InputStream inputStream) throws XmlPullParserException {
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        return SmackXmlParser.newXmlParser(inputStreamReader);
    }

    public static XmlPullParser getParserFor(Reader reader) throws XmlPullParserException, IOException {
        XmlPullParser parser = SmackXmlParser.newXmlParser(reader);
        ParserUtils.forwardToStartElement(parser);
        return parser;
    }

    @SuppressWarnings("unchecked")
    public static <S extends Stanza> S parseStanza(String stanza) throws XmlPullParserException, SmackParsingException, IOException {
        return (S) parseStanza(getParserFor(stanza), XmlEnvironment.EMPTY);
    }

    /**
     * Tries to parse and return either a Message, IQ or Presence stanza.
     *
     * connection is optional and is used to return feature-not-implemented errors for unknown IQ stanzas.
     *
     * @param parser TODO javadoc me please
     * @param outerXmlEnvironment the outer XML environment (optional).
     * @return a stanza which is either a Message, IQ or Presence.
     * @throws XmlPullParserException if an error in the XML parser occurred.
     * @throws SmackParsingException if the Smack parser (provider) encountered invalid input.
     * @throws IOException if an I/O error occurred.
     */
    public static Stanza parseStanza(XmlPullParser parser, XmlEnvironment outerXmlEnvironment) throws XmlPullParserException, SmackParsingException, IOException {
        ParserUtils.assertAtStartTag(parser);
        final String name = parser.getName();
        switch (name) {
        case Message.ELEMENT:
            return parseMessage(parser, outerXmlEnvironment);
        case IQ.IQ_ELEMENT:
            return parseIQ(parser, outerXmlEnvironment);
        case Presence.ELEMENT:
            return parsePresence(parser, outerXmlEnvironment);
        default:
            throw new IllegalArgumentException("Can only parse message, iq or presence, not " + name);
        }
    }

    private interface StanzaBuilderSupplier<SB extends StanzaBuilder<?>> {
        SB get(String stanzaId);
    }

    private static <SB extends StanzaBuilder<?>> SB parseCommonStanzaAttributes(StanzaBuilderSupplier<SB> stanzaBuilderSupplier, XmlPullParser parser, XmlEnvironment xmlEnvironment) throws XmppStringprepException {
        String id = parser.getAttributeValue("id");

        SB stanzaBuilder = stanzaBuilderSupplier.get(id);

        Jid to = ParserUtils.getJidAttribute(parser, "to");
        stanzaBuilder.to(to);

        Jid from = ParserUtils.getJidAttribute(parser, "from");
        stanzaBuilder.from(from);

        String language = ParserUtils.getXmlLang(parser, xmlEnvironment);
        stanzaBuilder.setLanguage(language);

        return stanzaBuilder;
    }

    public static Message parseMessage(XmlPullParser parser) throws XmlPullParserException, IOException, SmackParsingException {
        return parseMessage(parser, XmlEnvironment.EMPTY);
    }

    /**
     * Parses a message packet.
     *
     * @param parser the XML parser, positioned at the start of a message packet.
     * @param outerXmlEnvironment the outer XML environment (optional).
     * @return a Message packet.
     * @throws XmlPullParserException if an error in the XML parser occurred.
     * @throws IOException if an I/O error occurred.
     * @throws SmackParsingException if the Smack parser (provider) encountered invalid input.
     */
    public static Message parseMessage(XmlPullParser parser, XmlEnvironment outerXmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        ParserUtils.assertAtStartTag(parser);
        assert parser.getName().equals(Message.ELEMENT);

        XmlEnvironment messageXmlEnvironment = XmlEnvironment.from(parser, outerXmlEnvironment);
        final int initialDepth = parser.getDepth();

        MessageBuilder message = parseCommonStanzaAttributes(id -> {
            return StanzaBuilder.buildMessage(id);
        }, parser, outerXmlEnvironment);

        String typeString = parser.getAttributeValue("", "type");
        if (typeString != null) {
            message.ofType(Message.Type.fromString(typeString));
        }

        // Parse sub-elements. We include extra logic to make sure the values
        // are only read once. This is because it's possible for the names to appear
        // in arbitrary sub-elements.
        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                switch (elementName) {
                case "error":
                    message.setError(parseError(parser, messageXmlEnvironment));
                    break;
                 default:
                    ExtensionElement extensionElement = parseExtensionElement(elementName, namespace, parser, messageXmlEnvironment);
                    message.addExtension(extensionElement);
                    break;
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default: // fall out
            }
        }

        // TODO check for duplicate body elements. This means we need to check for duplicate xml:lang pairs and for
        // situations where we have a body element with an explicit xml lang set and once where the value is inherited
        // and both values are equal.

        return message.build();
    }

    /**
     * Returns the textual content of an element as String. After this method returns the parser
     * position will be END_ELEMENT, following the established pull parser calling convention.
     * <p>
     * The parser must be positioned on a START_ELEMENT of an element which MUST NOT contain Mixed
     * Content (as defined in XML 3.2.2), or else an XmlPullParserException will be thrown.
     * </p>
     * This method is used for the parts where the XMPP specification requires elements that contain
     * only text or are the empty element.
     *
     * @param parser TODO javadoc me please
     * @return the textual content of the element as String
     * @throws XmlPullParserException if an error in the XML parser occurred.
     * @throws IOException if an I/O error occurred.
     */
    public static String parseElementText(XmlPullParser parser) throws XmlPullParserException, IOException {
        assert parser.getEventType() == XmlPullParser.Event.START_ELEMENT;
        String res;
        // Advance to the text of the Element
        XmlPullParser.Event event = parser.next();
        if (event != XmlPullParser.Event.TEXT_CHARACTERS) {
            if (event == XmlPullParser.Event.END_ELEMENT) {
                // Assume this is the end tag of the start tag at the
                // beginning of this method. Typical examples where this
                // happens are body elements containing the empty string,
                // ie. <body></body>, which appears to be valid XMPP, or a
                // least it's not explicitly forbidden by RFC 6121 5.2.3
                return "";
            } else {
                throw new XmlPullParserException(
                                "Non-empty element tag not followed by text, while Mixed Content (XML 3.2.2) is disallowed");
            }
        }
        res = parser.getText();
        event = parser.next();
        if (event != XmlPullParser.Event.END_ELEMENT) {
            throw new XmlPullParserException(
                            "Non-empty element tag contains child-elements, while Mixed Content (XML 3.2.2) is disallowed");
        }
        return res;
    }

    /**
     * Returns the current element as string.
     * <p>
     * The parser must be positioned on START_ELEMENT.
     * </p>
     * Note that only the outermost namespace attributes ("xmlns") will be returned, not nested ones.
     *
     * @param parser the XML pull parser
     * @return the element as string
     * @throws XmlPullParserException if an error in the XML parser occurred.
     * @throws IOException if an I/O error occurred.
     */
    public static CharSequence parseElement(XmlPullParser parser) throws XmlPullParserException, IOException {
        return parseElement(parser, false);
    }

    public static CharSequence parseElement(XmlPullParser parser,
                    boolean fullNamespaces) throws XmlPullParserException,
                    IOException {
        assert parser.getEventType() == XmlPullParser.Event.START_ELEMENT;
        return parseContentDepth(parser, parser.getDepth(), fullNamespaces);
    }

    public static CharSequence parseContentDepth(XmlPullParser parser, int depth)
                    throws XmlPullParserException, IOException {
        return parseContentDepth(parser, depth, false);
    }

    /**
     * Returns the content from the current position of the parser up to the closing tag of the
     * given depth. Note that only the outermost namespace attributes ("xmlns") will be returned,
     * not nested ones, if <code>fullNamespaces</code> is false. If it is true, then namespaces of
     * parent elements will be added to child elements that don't define a different namespace.
     * <p>
     * This method is able to parse the content with MX- and KXmlParser. KXmlParser does not support
     * xml-roundtrip. i.e. return a String on getText() on START_ELEMENT and END_ELEMENT. We check for the
     * XML_ROUNDTRIP feature. If it's not found we are required to work around this limitation, which
     * results in only partial support for XML namespaces ("xmlns"): Only the outermost namespace of
     * elements will be included in the resulting String, if <code>fullNamespaces</code> is set to false.
     * </p>
     * <p>
     * In particular Android's XmlPullParser does not support XML_ROUNDTRIP.
     * </p>
     *
     * @param parser TODO javadoc me please
     * @param depth TODO javadoc me please
     * @param fullNamespaces TODO javadoc me please
     * @return the content of the current depth
     * @throws XmlPullParserException if an error in the XML parser occurred.
     * @throws IOException if an I/O error occurred.
     */
    public static CharSequence parseContentDepth(XmlPullParser parser, int depth, boolean fullNamespaces) throws XmlPullParserException, IOException {
        if (parser.supportsRoundtrip()) {
            return parseContentDepthWithRoundtrip(parser, depth, fullNamespaces);
        } else {
            return parseContentDepthWithoutRoundtrip(parser, depth, fullNamespaces);
        }
    }

    private static CharSequence parseContentDepthWithoutRoundtrip(XmlPullParser parser, int depth,
                    boolean fullNamespaces) throws XmlPullParserException, IOException {
        XmlStringBuilder xml = new XmlStringBuilder();
        XmlPullParser.Event event = parser.getEventType();
        // XmlPullParser reports namespaces in nested elements even if *only* the outer ones defines
        // it. This 'flag' ensures that when a namespace is set for an element, it won't be set again
        // in a nested element. It's an ugly workaround that has the potential to break things.
        String namespaceElement = null;
        boolean startElementJustSeen = false;
        outerloop: while (true) {
            switch (event) {
            case START_ELEMENT:
                if (startElementJustSeen) {
                    xml.rightAngleBracket();
                }
                else {
                    startElementJustSeen = true;
                }
                xml.halfOpenElement(parser.getName());
                if (namespaceElement == null || fullNamespaces) {
                    String namespace = parser.getNamespace();
                    if (StringUtils.isNotEmpty(namespace)) {
                        xml.attribute("xmlns", namespace);
                        namespaceElement = parser.getName();
                    }
                }
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    xml.attribute(parser.getAttributeName(i), parser.getAttributeValue(i));
                }
                break;
            case END_ELEMENT:
                if (startElementJustSeen) {
                    xml.closeEmptyElement();
                    startElementJustSeen = false;
                }
                else {
                    xml.closeElement(parser.getName());
                }
                if (namespaceElement != null && namespaceElement.equals(parser.getName())) {
                    // We are on the closing tag, which defined the namespace as starting tag, reset the 'flag'
                    namespaceElement = null;
                }
                if (parser.getDepth() <= depth) {
                    // Abort parsing, we are done
                    break outerloop;
                }
                break;
            case TEXT_CHARACTERS:
                if (startElementJustSeen) {
                    startElementJustSeen = false;
                    xml.rightAngleBracket();
                }
                xml.escape(parser.getText());
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
            event = parser.next();
        }
        return xml;
    }

    @SuppressWarnings("UnusedVariable")
    private static CharSequence parseContentDepthWithRoundtrip(XmlPullParser parser, int depth, boolean fullNamespaces)
                    throws XmlPullParserException, IOException {
        XmlStringBuilder sb = new XmlStringBuilder();
        XmlPullParser.Event event = parser.getEventType();
        boolean startElementJustSeen = false;
        outerloop: while (true) {
            switch (event) {
            case START_ELEMENT:
                if (startElementJustSeen) {
                    sb.rightAngleBracket();
                }
                startElementJustSeen = true;
                break;
            case END_ELEMENT:
                boolean isEmptyElement = false;
                if (startElementJustSeen) {
                    isEmptyElement = true;
                    startElementJustSeen = false;
                }
                if (!isEmptyElement) {
                    String text = parser.getText();
                    sb.append(text);
                }
                if (parser.getDepth() <= depth) {
                    break outerloop;
                }
                break;
            default:
                startElementJustSeen = false;
                CharSequence text = parser.getText();
                if (event == XmlPullParser.Event.TEXT_CHARACTERS) {
                    text = StringUtils.escapeForXml(text);
                }
                sb.append(text);
                break;
            }
            event = parser.next();
        }
        return sb;
    }

    public static Presence parsePresence(XmlPullParser parser) throws XmlPullParserException, IOException, SmackParsingException {
        return parsePresence(parser, XmlEnvironment.EMPTY);
    }

    /**
     * Parses a presence packet.
     *
     * @param parser the XML parser, positioned at the start of a presence packet.
     * @param outerXmlEnvironment the outer XML environment (optional).
     * @return a Presence packet.
     * @throws IOException if an I/O error occurred.
     * @throws XmlPullParserException if an error in the XML parser occurred.
     * @throws SmackParsingException if the Smack parser (provider) encountered invalid input.
     */
    public static Presence parsePresence(XmlPullParser parser, XmlEnvironment outerXmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        ParserUtils.assertAtStartTag(parser);
        final int initialDepth = parser.getDepth();
        XmlEnvironment presenceXmlEnvironment = XmlEnvironment.from(parser, outerXmlEnvironment);

        PresenceBuilder presence = parseCommonStanzaAttributes(
                        stanzaId -> StanzaBuilder.buildPresence(stanzaId), parser, outerXmlEnvironment);

        Presence.Type type = Presence.Type.available;
        String typeString = parser.getAttributeValue("", "type");
        if (typeString != null && !typeString.equals("")) {
            type = Presence.Type.fromString(typeString);
        }

        presence.ofType(type);

        // Parse sub-elements
        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                switch (elementName) {
                case "status":
                    presence.setStatus(parser.nextText());
                    break;
                case "priority":
                    Byte priority = ParserUtils.getByteAttributeFromNextText(parser);
                    presence.setPriority(priority);
                    break;
                case "show":
                    String modeText = parser.nextText();
                    if (StringUtils.isNotEmpty(modeText)) {
                        presence.setMode(Presence.Mode.fromString(modeText));
                    } else {
                        // Some implementations send presence stanzas with a
                        // '<show />' element, which is a invalid XMPP presence
                        // stanza according to RFC 6121 4.7.2.1
                        LOGGER.warning("Empty or null mode text in presence show element form "
                                        + presence
                                        + "' which is invalid according to RFC6121 4.7.2.1");
                    }
                    break;
                case "error":
                    presence.setError(parseError(parser, presenceXmlEnvironment));
                    break;
                default:
                // Otherwise, it must be a packet extension.
                    // Be extra robust: Skip PacketExtensions that cause Exceptions, instead of
                    // failing completely here. See SMACK-390 for more information.
                    try {
                        ExtensionElement extensionElement = parseExtensionElement(elementName, namespace, parser, presenceXmlEnvironment);
                        presence.addExtension(extensionElement);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to parse extension element in Presence stanza: " + presence, e);
                    }
                    break;
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }

        return presence.build();
    }

    public static IQ parseIQ(XmlPullParser parser) throws Exception {
        return parseIQ(parser, null);
    }

    /**
     * Parses an IQ packet.
     *
     * @param parser the XML parser, positioned at the start of an IQ packet.
     * @param outerXmlEnvironment the outer XML environment (optional).
     * @return an IQ object.
     * @throws XmlPullParserException if an error in the XML parser occurred.
     * @throws XmppStringprepException if the provided string is invalid.
     * @throws IOException if an I/O error occurred.
     * @throws SmackParsingException if the Smack parser (provider) encountered invalid input.
     */
    public static IQ parseIQ(XmlPullParser parser, XmlEnvironment outerXmlEnvironment) throws XmlPullParserException, XmppStringprepException, IOException, SmackParsingException {
        ParserUtils.assertAtStartTag(parser);
        final int initialDepth = parser.getDepth();
        XmlEnvironment iqXmlEnvironment = XmlEnvironment.from(parser, outerXmlEnvironment);
        IQ iqPacket = null;
        StanzaError error = null;

        final String id = parser.getAttributeValue("", "id");
        IqData iqData = StanzaBuilder.buildIqData(id);

        final Jid to = ParserUtils.getJidAttribute(parser, "to");
        iqData.to(to);

        final Jid from = ParserUtils.getJidAttribute(parser, "from");
        iqData.from(from);

        final IQ.Type type = IQ.Type.fromString(parser.getAttributeValue("", "type"));
        iqData.ofType(type);

        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();

            switch (eventType) {
            case START_ELEMENT:
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                switch (elementName) {
                case "error":
                    error = PacketParserUtils.parseError(parser, iqXmlEnvironment);
                    break;
                // Otherwise, see if there is a registered provider for
                // this element name and namespace.
                default:
                    IqProvider<IQ> provider = ProviderManager.getIQProvider(elementName, namespace);
                    if (provider != null) {
                            iqPacket = provider.parse(parser, iqData, outerXmlEnvironment);
                    }
                    // Note that if we reach this code, it is guranteed that the result IQ contained a child element
                    // (RFC 6120 § 8.2.3 6) because otherwhise we would have reached the END_ELEMENT first.
                    else {
                        // No Provider found for the IQ stanza, parse it to an UnparsedIQ instance
                        // so that the content of the IQ can be examined later on
                        iqPacket = new UnparsedIQ(elementName, namespace, parseElement(parser));
                    }
                    break;
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }
        // Decide what to do when an IQ packet was not understood
        if (iqPacket == null) {
            switch (type) {
            case error:
                // If an IQ packet wasn't created above, create an empty error IQ packet.
                iqPacket = new ErrorIQ(error);
                break;
            case result:
                iqPacket = new EmptyResultIQ();
                break;
            default:
                break;
            }
        }

        // Set basic values on the iq packet.
        iqPacket.setStanzaId(id);
        iqPacket.setTo(to);
        iqPacket.setFrom(from);
        iqPacket.setType(type);
        iqPacket.setError(error);

        return iqPacket;
    }

    /**
     * Parse the available SASL mechanisms reported from the server.
     *
     * @param parser the XML parser, positioned at the start of the mechanisms stanza.
     * @return a collection of Stings with the mechanisms included in the mechanisms stanza.
     * @throws IOException if an I/O error occurred.
     * @throws XmlPullParserException if an error in the XML parser occurred.
     */
    public static Collection<String> parseMechanisms(XmlPullParser parser)
                    throws XmlPullParserException, IOException {
        List<String> mechanisms = new ArrayList<String>();
        boolean done = false;
        while (!done) {
            XmlPullParser.Event eventType = parser.next();

            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                String elementName = parser.getName();
                if (elementName.equals("mechanism")) {
                    mechanisms.add(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                if (parser.getName().equals("mechanisms")) {
                    done = true;
                }
            }
        }
        return mechanisms;
    }

    /**
     * Parse the Compression Feature reported from the server.
     *
     * @param parser the XML parser, positioned at the start of the compression stanza.
     * @return The CompressionFeature stream element
     * @throws IOException if an I/O error occurred.
     * @throws XmlPullParserException if an exception occurs while parsing the stanza.
     */
    public static Compress.Feature parseCompressionFeature(XmlPullParser parser)
                    throws IOException, XmlPullParserException {
        assert parser.getEventType() == XmlPullParser.Event.START_ELEMENT;
        String name;
        final int initialDepth = parser.getDepth();
        List<String> methods = new LinkedList<>();
        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
                name = parser.getName();
                switch (name) {
                case "method":
                    methods.add(parser.nextText());
                    break;
                }
                break;
            case END_ELEMENT:
                name = parser.getName();
                switch (name) {
                case Compress.Feature.ELEMENT:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }
        assert parser.getEventType() == XmlPullParser.Event.END_ELEMENT;
        assert parser.getDepth() == initialDepth;
        return new Compress.Feature(methods);
    }

    public static Map<String, String> parseDescriptiveTexts(XmlPullParser parser, Map<String, String> descriptiveTexts)
                    throws XmlPullParserException, IOException {
        if (descriptiveTexts == null) {
            descriptiveTexts = new HashMap<>();
        }
        String xmllang = ParserUtils.getXmlLang(parser);
        if (xmllang == null) {
            // XMPPError assumes the default locale, 'en', or the empty string.
            // Establish the invariant that there is never null as a key.
            xmllang = "";
        }

        String text = parser.nextText();
        String previousValue = descriptiveTexts.put(xmllang, text);
        assert previousValue == null;
        return descriptiveTexts;
    }

    public static StreamError parseStreamError(XmlPullParser parser) throws XmlPullParserException, IOException, SmackParsingException {
        return parseStreamError(parser, null);
    }

    /**
     * Parses stream error packets.
     *
     * @param parser the XML parser.
     * @param outerXmlEnvironment the outer XML environment (optional).
     * @return an stream error packet.
     * @throws IOException if an I/O error occurred.
     * @throws XmlPullParserException if an error in the XML parser occurred.
     * @throws SmackParsingException if the Smack parser (provider) encountered invalid input.
     */
    public static StreamError parseStreamError(XmlPullParser parser, XmlEnvironment outerXmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        final int initialDepth = parser.getDepth();
        List<ExtensionElement> extensions = new ArrayList<>();
        Map<String, String> descriptiveTexts = null;
        StreamError.Condition condition = null;
        String conditionText = null;
        XmlEnvironment streamErrorXmlEnvironment = XmlEnvironment.from(parser, outerXmlEnvironment);
        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
                String name = parser.getName();
                String namespace = parser.getNamespace();
                switch (namespace) {
                case StreamError.NAMESPACE:
                    switch (name) {
                    case "text":
                        descriptiveTexts = parseDescriptiveTexts(parser, descriptiveTexts);
                        break;
                    default:
                        // If it's not a text element, that is qualified by the StreamError.NAMESPACE,
                        // then it has to be the stream error code
                        condition = StreamError.Condition.fromString(name);
                        conditionText = parser.nextText();
                        if (conditionText.isEmpty()) {
                            conditionText = null;
                        }
                        break;
                    }
                    break;
                default:
                    PacketParserUtils.addExtensionElement(extensions, parser, name, namespace, streamErrorXmlEnvironment);
                    break;
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }
        return new StreamError(condition, conditionText, descriptiveTexts, extensions);
    }

    public static StanzaError parseError(XmlPullParser parser) throws XmlPullParserException, IOException, SmackParsingException {
        return parseError(parser, null);
    }

    /**
     * Parses error sub-packets.
     *
     * @param parser the XML parser.
     * @param outerXmlEnvironment the outer XML environment (optional).
     * @return an error sub-packet.
     * @throws IOException if an I/O error occurred.
     * @throws XmlPullParserException if an error in the XML parser occurred.
     * @throws SmackParsingException if the Smack parser (provider) encountered invalid input.
     */
    public static StanzaError parseError(XmlPullParser parser, XmlEnvironment outerXmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        final int initialDepth = parser.getDepth();
        Map<String, String> descriptiveTexts = null;
        XmlEnvironment stanzaErrorXmlEnvironment = XmlEnvironment.from(parser, outerXmlEnvironment);
        List<ExtensionElement> extensions = new ArrayList<>();
        StanzaError.Builder builder = StanzaError.getBuilder();

        // Parse the error header
        builder.setType(StanzaError.Type.fromString(parser.getAttributeValue("", "type")));
        builder.setErrorGenerator(parser.getAttributeValue("", "by"));

        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
                String name = parser.getName();
                String namespace = parser.getNamespace();
                switch (namespace) {
                case StanzaError.ERROR_CONDITION_AND_TEXT_NAMESPACE:
                    switch (name) {
                    case Stanza.TEXT:
                        descriptiveTexts = parseDescriptiveTexts(parser, descriptiveTexts);
                        break;
                    default:
                        builder.setCondition(StanzaError.Condition.fromString(name));
                        String conditionText = parser.nextText();
                        if (!conditionText.isEmpty()) {
                            builder.setConditionText(conditionText);
                        }
                        break;
                    }
                    break;
                default:
                    PacketParserUtils.addExtensionElement(extensions, parser, name, namespace, stanzaErrorXmlEnvironment);
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }
        builder.setExtensions(extensions).setDescriptiveTexts(descriptiveTexts);

        return builder.build();
    }

    /**
     * Parses an extension element.
     *
     * @param elementName the XML element name of the extension element.
     * @param namespace the XML namespace of the stanza extension.
     * @param parser the XML parser, positioned at the starting element of the extension.
     * @param outerXmlEnvironment the outer XML environment (optional).
     *
     * @return an extension element.
     * @throws XmlPullParserException if an error in the XML parser occurred.
     * @throws IOException if an I/O error occurred.
     * @throws SmackParsingException if the Smack parser (provider) encountered invalid input.
     */
    public static ExtensionElement parseExtensionElement(String elementName, String namespace,
                    XmlPullParser parser, XmlEnvironment outerXmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        ParserUtils.assertAtStartTag(parser);
        // See if a provider is registered to handle the extension.
        ExtensionElementProvider<ExtensionElement> provider = ProviderManager.getExtensionProvider(elementName, namespace);
        if (provider != null) {
                return provider.parse(parser, outerXmlEnvironment);
        }

        // No providers registered, so use a default extension.
        return StandardExtensionElementProvider.INSTANCE.parse(parser, outerXmlEnvironment);
    }

    public static StartTls parseStartTlsFeature(XmlPullParser parser)
                    throws XmlPullParserException, IOException {
        ParserUtils.assertAtStartTag(parser);
        assert parser.getNamespace().equals(StartTls.NAMESPACE);
        int initalDepth = parser.getDepth();
        boolean required = false;
        outerloop: while (true) {
            XmlPullParser.Event event = parser.next();
            switch (event) {
            case START_ELEMENT:
                String name = parser.getName();
                switch (name) {
                case "required":
                    required = true;
                    break;
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initalDepth) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }
        ParserUtils.assertAtEndTag(parser);
        return new StartTls(required);
    }

    public static Session.Feature parseSessionFeature(XmlPullParser parser) throws XmlPullParserException, IOException {
        ParserUtils.assertAtStartTag(parser);
        final int initialDepth = parser.getDepth();
        boolean optional = false;

        outerloop: while (true) {
            XmlPullParser.Event event = parser.next();
            switch (event) {
            case START_ELEMENT:
                String name = parser.getName();
                switch (name) {
                    case Session.Feature.OPTIONAL_ELEMENT:
                        optional = true;
                        break;
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }

        return new Session.Feature(optional);
    }

    public static void addExtensionElement(StanzaBuilder<?> stanzaBuilder, XmlPullParser parser, XmlEnvironment outerXmlEnvironment)
                    throws XmlPullParserException, IOException, SmackParsingException {
        ParserUtils.assertAtStartTag(parser);
        addExtensionElement(stanzaBuilder, parser, parser.getName(), parser.getNamespace(), outerXmlEnvironment);
    }

    public static void addExtensionElement(StanzaBuilder<?> stanzaBuilder, XmlPullParser parser, String elementName,
            String namespace, XmlEnvironment outerXmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        ExtensionElement extensionElement = parseExtensionElement(elementName, namespace, parser, outerXmlEnvironment);
        stanzaBuilder.addExtension(extensionElement);
    }

    public static void addExtensionElement(Stanza packet, XmlPullParser parser, XmlEnvironment outerXmlEnvironment)
                    throws XmlPullParserException, IOException, SmackParsingException {
        ParserUtils.assertAtStartTag(parser);
        addExtensionElement(packet, parser, parser.getName(), parser.getNamespace(), outerXmlEnvironment);
    }

    public static void addExtensionElement(Stanza packet, XmlPullParser parser, String elementName,
            String namespace, XmlEnvironment outerXmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        ExtensionElement packetExtension = parseExtensionElement(elementName, namespace, parser, outerXmlEnvironment);
        packet.addExtension(packetExtension);
    }

    public static void addExtensionElement(Collection<ExtensionElement> collection, XmlPullParser parser, XmlEnvironment outerXmlEnvironment)
                    throws XmlPullParserException, IOException, SmackParsingException {
        addExtensionElement(collection, parser, parser.getName(), parser.getNamespace(), outerXmlEnvironment);
    }

    public static void addExtensionElement(Collection<ExtensionElement> collection, XmlPullParser parser,
                    String elementName, String namespace, XmlEnvironment outerXmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        ExtensionElement packetExtension = parseExtensionElement(elementName, namespace, parser, outerXmlEnvironment);
        collection.add(packetExtension);
    }
}
