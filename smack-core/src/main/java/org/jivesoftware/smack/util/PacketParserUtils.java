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
package org.jivesoftware.smack.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.compress.packet.Compress;
import org.jivesoftware.smack.packet.EmptyResultIQ;
import org.jivesoftware.smack.packet.ErrorIQ;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Session;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.StartTls;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.packet.UnparsedIQ;
import org.jivesoftware.smack.parsing.StandardExtensionElementProvider;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements.SASLFailure;

import org.jxmpp.jid.Jid;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * Utility class that helps to parse packets. Any parsing packets method that must be shared
 * between many clients must be placed in this utility class.
 *
 * @author Gaston Dombiak
 */
public class PacketParserUtils {
    private static final Logger LOGGER = Logger.getLogger(PacketParserUtils.class.getName());

    public static final String FEATURE_XML_ROUNDTRIP = "http://xmlpull.org/v1/doc/features.html#xml-roundtrip";

    private static final XmlPullParserFactory XML_PULL_PARSER_FACTORY;

    /**
     * True if the XmlPullParser supports the XML_ROUNDTRIP feature.
     */
    public static final boolean XML_PULL_PARSER_SUPPORTS_ROUNDTRIP;

    static {
        // Ensure that Smack is initialized.
        SmackConfiguration.getVersion();

        XmlPullParser xmlPullParser;
        boolean roundtrip = false;
        try {
            XML_PULL_PARSER_FACTORY = XmlPullParserFactory.newInstance();
            xmlPullParser = XML_PULL_PARSER_FACTORY.newPullParser();
            try {
                xmlPullParser.setFeature(FEATURE_XML_ROUNDTRIP, true);
                // We could successfully set the feature
                roundtrip = true;
            } catch (XmlPullParserException e) {
                // Doesn't matter if FEATURE_XML_ROUNDTRIP isn't available
                LOGGER.log(Level.FINEST, "XmlPullParser does not support XML_ROUNDTRIP", e);
            }
        }
        catch (XmlPullParserException e) {
            // Something really bad happened
            throw new AssertionError(e);
        }
        XML_PULL_PARSER_SUPPORTS_ROUNDTRIP = roundtrip;
    }

    public static XmlPullParser getParserFor(String stanza) throws XmlPullParserException, IOException {
        return getParserFor(new StringReader(stanza));
    }

    public static XmlPullParser getParserFor(Reader reader) throws XmlPullParserException, IOException {
        XmlPullParser parser = newXmppParser(reader);
        // Wind the parser forward to the first start tag
        int event = parser.getEventType();
        while (event != XmlPullParser.START_TAG) {
            if (event == XmlPullParser.END_DOCUMENT) {
                throw new IllegalArgumentException("Document contains no start tag");
            }
            event = parser.next();
        }
        return parser;
    }

    public static XmlPullParser getParserFor(String stanza, String startTag)
                    throws XmlPullParserException, IOException {
        XmlPullParser parser = getParserFor(stanza);

        while (true) {
            int event = parser.getEventType();
            String name = parser.getName();
            if (event == XmlPullParser.START_TAG && name.equals(startTag)) {
                break;
            }
            else if (event == XmlPullParser.END_DOCUMENT) {
                throw new IllegalArgumentException("Could not find start tag '" + startTag
                                + "' in stanza: " + stanza);
            }
            parser.next();
        }

        return parser;
    }

    @SuppressWarnings("unchecked")
    public static <S extends Stanza> S parseStanza(String stanza) throws Exception {
        return (S) parseStanza(getParserFor(stanza));
    }

    /**
     * Tries to parse and return either a Message, IQ or Presence stanza.
     * 
     * connection is optional and is used to return feature-not-implemented errors for unknown IQ stanzas.
     *
     * @param parser
     * @return a stanza which is either a Message, IQ or Presence.
     * @throws Exception 
     */
    public static Stanza parseStanza(XmlPullParser parser) throws Exception {
        ParserUtils.assertAtStartTag(parser);
        final String name = parser.getName();
        switch (name) {
        case Message.ELEMENT:
            return parseMessage(parser);
        case IQ.IQ_ELEMENT:
            return parseIQ(parser);
        case Presence.ELEMENT:
            return parsePresence(parser);
        default:
            throw new IllegalArgumentException("Can only parse message, iq or presence, not " + name);
        }
    }

    /**
     * Creates a new XmlPullParser suitable for parsing XMPP. This means in particular that
     * FEATURE_PROCESS_NAMESPACES is enabled.
     * <p>
     * Note that not all XmlPullParser implementations will return a String on
     * <code>getText()</code> if the parser is on START_TAG or END_TAG. So you must not rely on this
     * behavior when using the parser.
     * </p>
     * 
     * @return A suitable XmlPullParser for XMPP parsing
     * @throws XmlPullParserException
     */
    public static XmlPullParser newXmppParser() throws XmlPullParserException {
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        if (XML_PULL_PARSER_SUPPORTS_ROUNDTRIP) {
            try {
                parser.setFeature(FEATURE_XML_ROUNDTRIP, true);
            }
            catch (XmlPullParserException e) {
                LOGGER.log(Level.SEVERE,
                                "XmlPullParser does not support XML_ROUNDTRIP, although it was first determined to be supported",
                                e);
            }
        }
        return parser;
    }

    /**
     * Creates a new XmlPullParser suitable for parsing XMPP. This means in particular that
     * FEATURE_PROCESS_NAMESPACES is enabled.
     * <p>
     * Note that not all XmlPullParser implementations will return a String on
     * <code>getText()</code> if the parser is on START_TAG or END_TAG. So you must not rely on this
     * behavior when using the parser.
     * </p>
     * 
     * @param reader
     * @return A suitable XmlPullParser for XMPP parsing
     * @throws XmlPullParserException
     */
    public static XmlPullParser newXmppParser(Reader reader) throws XmlPullParserException {
        XmlPullParser parser = newXmppParser();
        parser.setInput(reader);
        return parser;
    }

    /**
     * Parses a message packet.
     *
     * @param parser the XML parser, positioned at the start of a message packet.
     * @return a Message packet.
     * @throws Exception 
     */
    public static Message parseMessage(XmlPullParser parser)
                    throws Exception {
        ParserUtils.assertAtStartTag(parser);
        assert (parser.getName().equals(Message.ELEMENT));

        final int initialDepth = parser.getDepth();
        Message message = new Message();
        message.setStanzaId(parser.getAttributeValue("", "id"));
        message.setTo(ParserUtils.getJidAttribute(parser, "to"));
        message.setFrom(ParserUtils.getJidAttribute(parser, "from"));
        String typeString = parser.getAttributeValue("", "type");
        if (typeString != null) {
            message.setType(Message.Type.fromString(typeString));
        }
        String language = getLanguageAttribute(parser);
        message.setLanguage(language);

        // Parse sub-elements. We include extra logic to make sure the values
        // are only read once. This is because it's possible for the names to appear
        // in arbitrary sub-elements.
        String thread = null;
        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                switch (elementName) {
                case "subject":
                    String xmlLangSubject = getLanguageAttribute(parser);
                    if (xmlLangSubject == null) {
                        xmlLangSubject = defaultLanguage;
                    }

                    String subject = parseElementText(parser);

                    if (message.getSubject(xmlLangSubject) == null) {
                        message.addSubject(xmlLangSubject, subject);
                    }
                    break;
                case Message.BODY:
                    String xmlLang = getLanguageAttribute(parser);
                    if (xmlLang == null) {
                        xmlLang = defaultLanguage;
                    }

                    String body = parseElementText(parser);

                    if (message.getBody(xmlLang) == null) {
                        message.addBody(xmlLang, body);
                    }
                    break;
                case "thread":
                    if (thread == null) {
                        thread = parser.nextText();
                    }
                    break;
                case "error":
                    message.setError(parseError(parser));
                    break;
                 default:
                    PacketParserUtils.addExtensionElement(message, parser, elementName, namespace);
                    break;
                }
                break;
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            }
        }

        message.setThread(thread);
        return message;
    }

    /**
     * Returns the textual content of an element as String. After this method returns the parser
     * position will be END_TAG, following the established pull parser calling convention.
     * <p>
     * The parser must be positioned on a START_TAG of an element which MUST NOT contain Mixed
     * Content (as defined in XML 3.2.2), or else an XmlPullParserException will be thrown.
     * </p>
     * This method is used for the parts where the XMPP specification requires elements that contain
     * only text or are the empty element.
     * 
     * @param parser
     * @return the textual content of the element as String
     * @throws XmlPullParserException
     * @throws IOException
     */
    public static String parseElementText(XmlPullParser parser) throws XmlPullParserException, IOException {
        assert (parser.getEventType() == XmlPullParser.START_TAG);
        String res;
        if (parser.isEmptyElementTag()) {
            res = "";
        }
        else {
            // Advance to the text of the Element
            int event = parser.next();
            if (event != XmlPullParser.TEXT) {
                if (event == XmlPullParser.END_TAG) {
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
            if (event != XmlPullParser.END_TAG) {
                throw new XmlPullParserException(
                                "Non-empty element tag contains child-elements, while Mixed Content (XML 3.2.2) is disallowed");
            }
        }
        return res;
    }

    /**
     * Returns the current element as string.
     * <p>
     * The parser must be positioned on START_TAG.
     * </p>
     * Note that only the outermost namespace attributes ("xmlns") will be returned, not nested ones.
     *
     * @param parser the XML pull parser
     * @return the element as string
     * @throws XmlPullParserException
     * @throws IOException
     */
    public static CharSequence parseElement(XmlPullParser parser) throws XmlPullParserException, IOException {
        return parseElement(parser, false);
    }

    public static CharSequence parseElement(XmlPullParser parser,
                    boolean fullNamespaces) throws XmlPullParserException,
                    IOException {
        assert (parser.getEventType() == XmlPullParser.START_TAG);
        return parseContentDepth(parser, parser.getDepth(), fullNamespaces);
    }

    /**
     * Returns the content of a element.
     * <p>
     * The parser must be positioned on the START_TAG of the element which content is going to get
     * returned. If the current element is the empty element, then the empty string is returned. If
     * it is a element which contains just text, then just the text is returned. If it contains
     * nested elements (and text), then everything from the current opening tag to the corresponding
     * closing tag of the same depth is returned as String.
     * </p>
     * Note that only the outermost namespace attributes ("xmlns") will be returned, not nested ones.
     * 
     * @param parser the XML pull parser
     * @return the content of a tag
     * @throws XmlPullParserException if parser encounters invalid XML
     * @throws IOException if an IO error occurs
     */
    public static CharSequence parseContent(XmlPullParser parser)
                    throws XmlPullParserException, IOException {
        assert (parser.getEventType() == XmlPullParser.START_TAG);
        if (parser.isEmptyElementTag()) {
            return "";
        }
        // Advance the parser, since we want to parse the content of the current element
        parser.next();
        return parseContentDepth(parser, parser.getDepth(), false);
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
     * xml-roundtrip. i.e. return a String on getText() on START_TAG and END_TAG. We check for the
     * XML_ROUNDTRIP feature. If it's not found we are required to work around this limitation, which
     * results in only partial support for XML namespaces ("xmlns"): Only the outermost namespace of
     * elements will be included in the resulting String, if <code>fullNamespaces</code> is set to false.
     * </p>
     * <p>
     * In particular Android's XmlPullParser does not support XML_ROUNDTRIP.
     * </p>
     * 
     * @param parser
     * @param depth
     * @param fullNamespaces
     * @return the content of the current depth
     * @throws XmlPullParserException
     * @throws IOException
     */
    public static CharSequence parseContentDepth(XmlPullParser parser, int depth, boolean fullNamespaces) throws XmlPullParserException, IOException {
        if (parser.getFeature(FEATURE_XML_ROUNDTRIP)) {
            return parseContentDepthWithRoundtrip(parser, depth, fullNamespaces);
        } else {
            return parseContentDepthWithoutRoundtrip(parser, depth, fullNamespaces);
        }
    }

    private static CharSequence parseContentDepthWithoutRoundtrip(XmlPullParser parser, int depth,
                    boolean fullNamespaces) throws XmlPullParserException, IOException {
        XmlStringBuilder xml = new XmlStringBuilder();
        int event = parser.getEventType();
        boolean isEmptyElement = false;
        // XmlPullParser reports namespaces in nested elements even if *only* the outer ones defines
        // it. This 'flag' ensures that when a namespace is set for an element, it won't be set again
        // in a nested element. It's an ugly workaround that has the potential to break things.
        String namespaceElement = null;
        outerloop: while (true) {
            switch (event) {
            case XmlPullParser.START_TAG:
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
                if (parser.isEmptyElementTag()) {
                    xml.closeEmptyElement();
                    isEmptyElement = true;
                }
                else {
                    xml.rightAngleBracket();
                }
                break;
            case XmlPullParser.END_TAG:
                if (isEmptyElement) {
                    // Do nothing as the element was already closed, just reset the flag
                    isEmptyElement = false;
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
            case XmlPullParser.TEXT:
                xml.escape(parser.getText());
                break;
            }
            event = parser.next();
        }
        return xml;
    }

    private static CharSequence parseContentDepthWithRoundtrip(XmlPullParser parser, int depth, boolean fullNamespaces)
                    throws XmlPullParserException, IOException {
        StringBuilder sb = new StringBuilder();
        int event = parser.getEventType();
        outerloop: while (true) {
            // Only append the text if the parser is not on on an empty element' start tag. Empty elements are reported
            // twice, so in order to prevent duplication we only add their text when we are on their end tag.
            if (!(event == XmlPullParser.START_TAG && parser.isEmptyElementTag())) {
                CharSequence text = parser.getText();
                if (event == XmlPullParser.TEXT) {
                    // TODO the toString() can be removed in Smack 4.2.
                    text = StringUtils.escapeForXmlText(text.toString());
                }
                sb.append(text);
            }
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= depth) {
                break outerloop;
            }
            event = parser.next();
        }
        return sb;
    }

    /**
     * Parses a presence packet.
     *
     * @param parser the XML parser, positioned at the start of a presence packet.
     * @return a Presence packet.
     * @throws Exception 
     */
    public static Presence parsePresence(XmlPullParser parser)
                    throws Exception {
        ParserUtils.assertAtStartTag(parser);
        final int initialDepth = parser.getDepth();

        Presence.Type type = Presence.Type.available;
        String typeString = parser.getAttributeValue("", "type");
        if (typeString != null && !typeString.equals("")) {
            type = Presence.Type.fromString(typeString);
        }
        Presence presence = new Presence(type);
        presence.setTo(ParserUtils.getJidAttribute(parser, "to"));
        presence.setFrom(ParserUtils.getJidAttribute(parser, "from"));
        presence.setStanzaId(parser.getAttributeValue("", "id"));

        String language = getLanguageAttribute(parser);
        if (language != null && !"".equals(language.trim())) {
        // CHECKSTYLE:OFF
        	presence.setLanguage(language);
        // CHECKSTYLE:ON
        }

        // Parse sub-elements
        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                switch (elementName) {
                case "status":
                    presence.setStatus(parser.nextText());
                    break;
                case "priority":
                    int priority = Integer.parseInt(parser.nextText());
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
                                        + presence.getFrom()
                                        + " with id '"
                                        + presence.getStanzaId()
                                        + "' which is invalid according to RFC6121 4.7.2.1");
                    }
                    break;
                case "error":
                    presence.setError(parseError(parser));
                    break;
                default:
                // Otherwise, it must be a packet extension.
                    // Be extra robust: Skip PacketExtensions that cause Exceptions, instead of
                    // failing completely here. See SMACK-390 for more information.
                    try {
                        PacketParserUtils.addExtensionElement(presence, parser, elementName, namespace);
                    } catch (Exception e) {
                        LOGGER.warning("Failed to parse extension element in Presence stanza: \"" + e + "\" from: '"
                                        + presence.getFrom() + " id: '" + presence.getStanzaId() + "'");
                    }
                    break;
                }
                break;
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            }
        }
        return presence;
    }

    /**
     * Parses an IQ packet.
     *
     * @param parser the XML parser, positioned at the start of an IQ packet.
     * @return an IQ object.
     * @throws Exception
     */
    public static IQ parseIQ(XmlPullParser parser) throws Exception {
        ParserUtils.assertAtStartTag(parser);
        final int initialDepth = parser.getDepth();
        IQ iqPacket = null;
        StanzaError.Builder error = null;

        final String id = parser.getAttributeValue("", "id");
        final Jid to = ParserUtils.getJidAttribute(parser, "to");
        final Jid from = ParserUtils.getJidAttribute(parser, "from");
        final IQ.Type type = IQ.Type.fromString(parser.getAttributeValue("", "type"));

        outerloop: while (true) {
            int eventType = parser.next();

            switch (eventType) {
            case XmlPullParser.START_TAG:
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                switch (elementName) {
                case "error":
                    error = PacketParserUtils.parseError(parser);
                    break;
                // Otherwise, see if there is a registered provider for
                // this element name and namespace.
                default:
                    IQProvider<IQ> provider = ProviderManager.getIQProvider(elementName, namespace);
                    if (provider != null) {
                            iqPacket = provider.parse(parser);
                    }
                    // Note that if we reach this code, it is guranteed that the result IQ contained a child element
                    // (RFC 6120 § 8.2.3 6) because otherwhise we would have reached the END_TAG first.
                    else {
                        // No Provider found for the IQ stanza, parse it to an UnparsedIQ instance
                        // so that the content of the IQ can be examined later on
                        iqPacket = new UnparsedIQ(elementName, namespace, parseElement(parser));
                    }
                    break;
                }
                break;
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
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
     * @throws IOException 
     * @throws XmlPullParserException 
     */
    public static Collection<String> parseMechanisms(XmlPullParser parser)
                    throws XmlPullParserException, IOException {
        List<String> mechanisms = new ArrayList<String>();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                String elementName = parser.getName();
                if (elementName.equals("mechanism")) {
                    mechanisms.add(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
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
     * @throws IOException
     * @throws XmlPullParserException if an exception occurs while parsing the stanza.
     */
    public static Compress.Feature parseCompressionFeature(XmlPullParser parser)
                    throws IOException, XmlPullParserException {
        assert (parser.getEventType() == XmlPullParser.START_TAG);
        String name;
        final int initialDepth = parser.getDepth();
        List<String> methods = new LinkedList<>();
        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
                name = parser.getName();
                switch (name) {
                case "method":
                    methods.add(parser.nextText());
                    break;
                }
                break;
            case XmlPullParser.END_TAG:
                name = parser.getName();
                switch (name) {
                case Compress.Feature.ELEMENT:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                }
            }
        }
        assert (parser.getEventType() == XmlPullParser.END_TAG);
        assert (parser.getDepth() == initialDepth);
        return new Compress.Feature(methods);
    }

    public static Map<String, String> parseDescriptiveTexts(XmlPullParser parser, Map<String, String> descriptiveTexts)
                    throws XmlPullParserException, IOException {
        if (descriptiveTexts == null) {
            descriptiveTexts = new HashMap<>();
        }
        String xmllang = getLanguageAttribute(parser);
        if (xmllang == null) {
            // XMPPError assumes the default locale, 'en', or the empty string.
            // Establish the invariant that there is never null as a key.
            xmllang = "";
        }

        String text = parser.nextText();
        String previousValue = descriptiveTexts.put(xmllang, text);
        assert (previousValue == null);
        return descriptiveTexts;
    }

    /**
     * Parses SASL authentication error packets.
     * 
     * @param parser the XML parser.
     * @return a SASL Failure packet.
     * @throws IOException 
     * @throws XmlPullParserException 
     */
    public static SASLFailure parseSASLFailure(XmlPullParser parser) throws XmlPullParserException, IOException {
        final int initialDepth = parser.getDepth();
        String condition = null;
        Map<String, String> descriptiveTexts = null;
        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
                String name = parser.getName();
                if (name.equals("text")) {
                    descriptiveTexts = parseDescriptiveTexts(parser, descriptiveTexts);
                }
                else {
                    assert (condition == null);
                    condition = parser.getName();
                }
                break;
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            }
        }
        return new SASLFailure(condition, descriptiveTexts);
    }

    /**
     * Parses stream error packets.
     *
     * @param parser the XML parser.
     * @return an stream error packet.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    public static StreamError parseStreamError(XmlPullParser parser) throws Exception {
        final int initialDepth = parser.getDepth();
        List<ExtensionElement> extensions = new ArrayList<>();
        Map<String, String> descriptiveTexts = null;
        StreamError.Condition condition = null;
        String conditionText = null;
        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
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
                        if (!parser.isEmptyElementTag()) {
                            conditionText = parser.nextText();
                        }
                        break;
                    }
                    break;
                default:
                    PacketParserUtils.addExtensionElement(extensions, parser, name, namespace);
                    break;
                }
                break;
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            }
        }
        return new StreamError(condition, conditionText, descriptiveTexts, extensions);
    }

    /**
     * Parses error sub-packets.
     *
     * @param parser the XML parser.
     * @return an error sub-packet.
     * @throws Exception 
     */
    public static StanzaError.Builder parseError(XmlPullParser parser)
                    throws Exception {
        final int initialDepth = parser.getDepth();
        Map<String, String> descriptiveTexts = null;
        List<ExtensionElement> extensions = new ArrayList<>();
        StanzaError.Builder builder = StanzaError.getBuilder();

        // Parse the error header
        builder.setType(StanzaError.Type.fromString(parser.getAttributeValue("", "type")));
        builder.setErrorGenerator(parser.getAttributeValue("", "by"));

        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
                String name = parser.getName();
                String namespace = parser.getNamespace();
                switch (namespace) {
                case StanzaError.NAMESPACE:
                    switch (name) {
                    case Stanza.TEXT:
                        descriptiveTexts = parseDescriptiveTexts(parser, descriptiveTexts);
                        break;
                    default:
                        builder.setCondition(StanzaError.Condition.fromString(name));
                        if (!parser.isEmptyElementTag()) {
                            builder.setConditionText(parser.nextText());
                        }
                        break;
                    }
                    break;
                default:
                    PacketParserUtils.addExtensionElement(extensions, parser, name, namespace);
                }
                break;
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
        }
        builder.setExtensions(extensions).setDescriptiveTexts(descriptiveTexts);
        return builder;
    }

    /**
     * Parses an extension element.
     *
     * @param elementName the XML element name of the extension element.
     * @param namespace the XML namespace of the stanza extension.
     * @param parser the XML parser, positioned at the starting element of the extension.
     *
     * @return an extension element.
     * @throws Exception when an error occurs during parsing.
     * @deprecated use {@link #parseExtensionElement(String, String, XmlPullParser)} instead.
     */
    @Deprecated
    public static ExtensionElement parsePacketExtension(String elementName, String namespace,
                    XmlPullParser parser) throws Exception {
        return parseExtensionElement(elementName, namespace, parser);
    }

    /**
     * Parses an extension element.
     *
     * @param elementName the XML element name of the extension element.
     * @param namespace the XML namespace of the stanza extension.
     * @param parser the XML parser, positioned at the starting element of the extension.
     *
     * @return an extension element.
     * @throws Exception when an error occurs during parsing.
     */
    public static ExtensionElement parseExtensionElement(String elementName, String namespace,
                    XmlPullParser parser) throws Exception {
        ParserUtils.assertAtStartTag(parser);
        // See if a provider is registered to handle the extension.
        ExtensionElementProvider<ExtensionElement> provider = ProviderManager.getExtensionProvider(elementName, namespace);
        if (provider != null) {
                return provider.parse(parser);
        }

        // No providers registered, so use a default extension.
        return StandardExtensionElementProvider.INSTANCE.parse(parser);
    }

    public static StartTls parseStartTlsFeature(XmlPullParser parser)
                    throws XmlPullParserException, IOException {
        assert (parser.getEventType() == XmlPullParser.START_TAG);
        assert (parser.getNamespace().equals(StartTls.NAMESPACE));
        int initalDepth = parser.getDepth();
        boolean required = false;
        outerloop: while (true) {
            int event = parser.next();
            switch (event) {
            case XmlPullParser.START_TAG:
                String name = parser.getName();
                switch (name) {
                case "required":
                    required = true;
                    break;
                }
                break;
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initalDepth) {
                    break outerloop;
                }
            }
        }
        assert (parser.getEventType() == XmlPullParser.END_TAG);
        return new StartTls(required);
    }

    public static Session.Feature parseSessionFeature(XmlPullParser parser) throws XmlPullParserException, IOException {
        ParserUtils.assertAtStartTag(parser);
        final int initialDepth = parser.getDepth();
        boolean optional = false;
        if (!parser.isEmptyElementTag()) {
        outerloop: while (true) {
            int event = parser.next();
            switch (event) {
            case XmlPullParser.START_TAG:
                String name = parser.getName();
                switch (name) {
                    case Session.Feature.OPTIONAL_ELEMENT:
                        optional = true;
                        break;
                }
                break;
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
        }
        }
        return new Session.Feature(optional);

    }

    // TODO Remove this static method and use ParserUtils.getXmlLang(XmlPullParser) instead.
    private static String getLanguageAttribute(XmlPullParser parser) {
    // CHECKSTYLE:OFF
    	for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            if ( "xml:lang".equals(attributeName) ||
    // CHECKSTYLE:ON
                    ("lang".equals(attributeName) &&
                            "xml".equals(parser.getAttributePrefix(i)))) {
    // CHECKSTYLE:OFF
    			return parser.getAttributeValue(i);
    		}
    	}
    	return null;
    // CHECKSTYLE:ON
    }

    @Deprecated
    public static void addPacketExtension(Stanza packet, XmlPullParser parser) throws Exception {
        addExtensionElement(packet, parser);
    }

    @Deprecated
    public static void addPacketExtension(Stanza packet, XmlPullParser parser, String elementName, String namespace)
                    throws Exception {
        addExtensionElement(packet, parser, elementName, namespace);
    }

    @Deprecated
    public static void addPacketExtension(Collection<ExtensionElement> collection, XmlPullParser parser)
                    throws Exception {
        addExtensionElement(collection, parser, parser.getName(), parser.getNamespace());
    }

    @Deprecated
    public static void addPacketExtension(Collection<ExtensionElement> collection, XmlPullParser parser,
                    String elementName, String namespace) throws Exception {
        addExtensionElement(collection, parser, elementName, namespace);
    }


    public static void addExtensionElement(Stanza packet, XmlPullParser parser)
                    throws Exception {
        ParserUtils.assertAtStartTag(parser);
        addExtensionElement(packet, parser, parser.getName(), parser.getNamespace());
    }

    public static void addExtensionElement(Stanza packet, XmlPullParser parser, String elementName,
                    String namespace) throws Exception {
        ExtensionElement packetExtension = parseExtensionElement(elementName, namespace, parser);
        packet.addExtension(packetExtension);
    }

    public static void addExtensionElement(Collection<ExtensionElement> collection,
                    XmlPullParser parser) throws Exception {
        addExtensionElement(collection, parser, parser.getName(), parser.getNamespace());
    }

    public static void addExtensionElement(Collection<ExtensionElement> collection,
                    XmlPullParser parser, String elementName, String namespace)
                    throws Exception {
        ExtensionElement packetExtension = parseExtensionElement(elementName, namespace, parser);
        collection.add(packetExtension);
    }
}
