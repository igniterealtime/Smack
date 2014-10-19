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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.compress.packet.Compress;
import org.jivesoftware.smack.packet.Bind;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.StartTls;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements.SASLFailure;
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

    public static Packet parseStanza(String stanza) throws Exception {
        return parseStanza(getParserFor(stanza));
    }

    public static Packet parseStanza(XmlPullParser parser) throws Exception {
        return parseStanza(parser, null);
    }

    /**
     * Tries to parse and return either a Message, IQ or Presence stanza.
     * 
     * connection is optional and is used to return feature-not-implemented errors for unknown IQ stanzas.
     *
     * @param parser
     * @param connection
     * @return a packet which is either a Message, IQ or Presence.
     * @throws Exception
     */
    public static Packet parseStanza(XmlPullParser parser, XMPPConnection connection) throws Exception {
        assert(parser.getEventType() == XmlPullParser.START_TAG);
        final String name = parser.getName();
        switch (name) {
        case Message.ELEMENT:
            return parseMessage(parser);
        case IQ.ELEMENT:
            return parse(parser, connection);
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
     * @throws IOException 
     * @throws XmlPullParserException 
     * @throws SmackException 
     */
    public static Message parseMessage(XmlPullParser parser)
                    throws XmlPullParserException, IOException, SmackException {
        ParserUtils.assertAtStartTag(parser);
        assert(parser.getName().equals(Message.ELEMENT));

        final int initialDepth = parser.getDepth();
        Message message = new Message();
        message.setPacketID(parser.getAttributeValue("", "id"));
        message.setTo(parser.getAttributeValue("", "to"));
        message.setFrom(parser.getAttributeValue("", "from"));
        String typeString = parser.getAttributeValue("", "type");
        if (typeString != null) {
            message.setType(Message.Type.fromString(typeString));
        }
        String language = getLanguageAttribute(parser);
        
        // determine message's default language
        String defaultLanguage = null;
        if (language != null && !"".equals(language.trim())) {
            message.setLanguage(language);
            defaultLanguage = language;
        } 
        else {
            defaultLanguage = Packet.getDefaultLanguage();
        }

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
                switch(elementName) {
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
                    PacketParserUtils.addPacketExtension(message, parser, elementName, namespace);
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
     * Returns the textual content of an element as String.
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
        assert(parser.getEventType() == XmlPullParser.START_TAG);
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
        while (true) {
            if (event == XmlPullParser.START_TAG) {
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
            }
            else if (event == XmlPullParser.END_TAG) {
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
                    break;
                }
            }
            else if (event == XmlPullParser.TEXT) {
                xml.append(parser.getText());
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
                sb.append(parser.getText());
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
     * @throws IOException 
     * @throws XmlPullParserException 
     * @throws SmackException 
     */
    public static Presence parsePresence(XmlPullParser parser)
                    throws XmlPullParserException, IOException, SmackException {
        ParserUtils.assertAtStartTag(parser);
        final int initialDepth = parser.getDepth();

        Presence.Type type = Presence.Type.available;
        String typeString = parser.getAttributeValue("", "type");
        if (typeString != null && !typeString.equals("")) {
            type = Presence.Type.fromString(typeString);
        }
        Presence presence = new Presence(type);
        presence.setTo(parser.getAttributeValue("", "to"));
        presence.setFrom(parser.getAttributeValue("", "from"));
        presence.setPacketID(parser.getAttributeValue("", "id"));

        String language = getLanguageAttribute(parser);
        if (language != null && !"".equals(language.trim())) {
        	presence.setLanguage(language);
        }

        // Parse sub-elements
        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                switch(elementName) {
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
                                        + presence.getPacketID()
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
                        PacketParserUtils.addPacketExtension(presence, parser, elementName, namespace);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to parse extension packet in Presence packet.", e);
                    }
                    break;
                }
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
     * @param connection the optional XMPPConnection used to send feature-not-implemented replies.
     * @return an IQ object.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    public static IQ parse(XmlPullParser parser, XMPPConnection connection) throws Exception {
        IQ iqPacket = null;
        XMPPError error = null;

        final String id = parser.getAttributeValue("", "id");
        final String to = parser.getAttributeValue("", "to");
        final String from = parser.getAttributeValue("", "from");
        final IQ.Type type = IQ.Type.fromString(parser.getAttributeValue("", "type"));

        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                if (elementName.equals("error")) {
                    error = PacketParserUtils.parseError(parser);
                }
                else if (elementName.equals(RosterPacket.ELEMENT) && namespace.equals(RosterPacket.NAMESPACE)) {
                    iqPacket = parseRoster(parser);
                }
                else if (elementName.equals(Bind.ELEMENT) && namespace.equals(Bind.NAMESPACE)) {
                    iqPacket = parseResourceBinding(parser);
                }
                // Otherwise, see if there is a registered provider for
                // this element name and namespace.
                else {
                    IQProvider<IQ> provider = ProviderManager.getIQProvider(elementName, namespace);
                    if (provider != null) {
                            iqPacket = provider.parse(parser);
                    } else {
                        Class<?> introspectionProvider = ProviderManager.getIQIntrospectionProvider(
                                        elementName, namespace);
                        if (introspectionProvider != null) {
                        iqPacket = (IQ) PacketParserUtils.parseWithIntrospection(
                                        elementName, introspectionProvider,
                                        parser);
                        }
                        // Only handle unknown IQs of type result. Types of 'get' and 'set' which are not understood
                        // have to be answered with an IQ error response. See the code a few lines below
                        else if (IQ.Type.result == type){
                            // No Provider found for the IQ stanza, parse it to an UnparsedIQ instance
                            // so that the content of the IQ can be examined later on
                            iqPacket = new UnparsedResultIQ(parseContent(parser));
                        }
                    }
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("iq")) {
                    done = true;
                }
            }
        }
        // Decide what to do when an IQ packet was not understood
        if (iqPacket == null) {
            if (connection != null && (IQ.Type.get == type || IQ.Type.set == type)) {
                // If the IQ stanza is of type "get" or "set" containing a child element qualified
                // by a namespace with no registered Smack provider, then answer an IQ of type
                // "error" with code 501 ("feature-not-implemented")
                iqPacket = new IQ() {
                    @Override
                    public String getChildElementXML() {
                        return null;
                    }
                };
                iqPacket.setPacketID(id);
                iqPacket.setTo(from);
                iqPacket.setFrom(to);
                iqPacket.setType(IQ.Type.error);
                iqPacket.setError(new XMPPError(XMPPError.Condition.feature_not_implemented));
                connection.sendPacket(iqPacket);
                return null;
            }
            else {
                // If an IQ packet wasn't created above, create an empty IQ packet.
                iqPacket = new IQ() {
                    @Override
                    public String getChildElementXML() {
                        return null;
                    }
                };
            }
        }

        // Set basic values on the iq packet.
        iqPacket.setPacketID(id);
        iqPacket.setTo(to);
        iqPacket.setFrom(from);
        iqPacket.setType(type);
        iqPacket.setError(error);

        return iqPacket;
    }

    public static RosterPacket parseRoster(XmlPullParser parser) throws XmlPullParserException, IOException {
        RosterPacket roster = new RosterPacket();
        boolean done = false;
        RosterPacket.Item item = null;

        String version = parser.getAttributeValue("", "ver");
        roster.setVersion(version);

        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("item")) {
                    String jid = parser.getAttributeValue("", "jid");
                    String name = parser.getAttributeValue("", "name");
                    // Create packet.
                    item = new RosterPacket.Item(jid, name);
                    // Set status.
                    String ask = parser.getAttributeValue("", "ask");
                    RosterPacket.ItemStatus status = RosterPacket.ItemStatus.fromString(ask);
                    item.setItemStatus(status);
                    // Set type.
                    String subscription = parser.getAttributeValue("", "subscription");
                    RosterPacket.ItemType type = RosterPacket.ItemType.valueOf(subscription != null ? subscription : "none");
                    item.setItemType(type);
                }
                else if (parser.getName().equals("group") && item!= null) {
                    final String groupName = parser.nextText();
                    if (groupName != null && groupName.trim().length() > 0) {
                        item.addGroupName(groupName);
                    }
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("item")) {
                    roster.addRosterItem(item);
                }
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }
        }
        return roster;
    }

    public static Bind parseResourceBinding(XmlPullParser parser) throws IOException,
                    XmlPullParserException {
        assert (parser.getEventType() == XmlPullParser.START_TAG);
        int initalDepth = parser.getDepth();
        String name;
        Bind bind = null;
        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
                name = parser.getName();
                switch (name) {
                case "resource":
                    bind = Bind.newSet(parser.nextText());
                    break;
                case "jid":
                    bind = Bind.newResult(parser.nextText());
                    break;
                }
                break;
            case XmlPullParser.END_TAG:
                name = parser.getName();
                if (name.equals(Bind.ELEMENT) && parser.getDepth() == initalDepth) {
                    break outerloop;
                }
                break;
            }
        }
        assert (parser.getEventType() == XmlPullParser.END_TAG);
        return bind;
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
     * @throws XmlPullParserException if an exception occurs while parsing the stanza.
     */
    public static Compress.Feature parseCompressionFeature(XmlPullParser parser)
                    throws IOException, XmlPullParserException {
        assert (parser.getEventType() == XmlPullParser.START_TAG);
        String name;
        final int initialDepth = parser.getDepth();
        List<String> methods = new LinkedList<String>();
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

    /**
     * Parses SASL authentication error packets.
     * 
     * @param parser the XML parser.
     * @return a SASL Failure packet.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    public static SASLFailure parseSASLFailure(XmlPullParser parser) throws Exception {
        String condition = null;
        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                if (!parser.getName().equals("failure")) {
                    condition = parser.getName();
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("failure")) {
                    done = true;
                }
            }
        }
        return new SASLFailure(condition);
    }

    /**
     * Parses stream error packets.
     *
     * @param parser the XML parser.
     * @return an stream error packet.
     * @throws XmlPullParserException if an exception occurs while parsing the packet.
     */
    public static StreamError parseStreamError(XmlPullParser parser) throws IOException,
            XmlPullParserException {
    final int depth = parser.getDepth();
    boolean done = false;
    String code = null;
    String text = null;
    while (!done) {
        int eventType = parser.next();

        if (eventType == XmlPullParser.START_TAG) {
            String namespace = parser.getNamespace();
            if (StreamError.NAMESPACE.equals(namespace)) {
                String name = parser.getName();
                if (name.equals(Packet.TEXT) && !parser.isEmptyElementTag()) {
                    parser.next();
                    text = parser.getText();
                }
                else {
                    // If it's not a text element, that is qualified by the StreamError.NAMESPACE,
                    // then it has to be the stream error code
                    code = name;
                }
            }
        }
        else if (eventType == XmlPullParser.END_TAG && depth == parser.getDepth()) {
            done = true;
        }
    }
    return new StreamError(code, text);
}

    /**
     * Parses error sub-packets.
     *
     * @param parser the XML parser.
     * @return an error sub-packet.
     * @throws IOException 
     * @throws XmlPullParserException 
     * @throws SmackException 
     */
    public static XMPPError parseError(XmlPullParser parser)
                    throws XmlPullParserException, IOException, SmackException {
        String type = null;
        String message = null;
        String condition = null;
        List<PacketExtension> extensions = new ArrayList<PacketExtension>();

        // Parse the error header
        type = parser.getAttributeValue("", "type");
        // Parse the text and condition tags
        outerloop:
        while (true) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals(Packet.TEXT)) {
                    message = parser.nextText();
                }
                else {
                	// Condition tag, it can be xmpp error or an application defined error.
                    String elementName = parser.getName();
                    String namespace = parser.getNamespace();
                    if (namespace.equals(XMPPError.NAMESPACE)) {
                    	condition = elementName;
                    }
                     else {
                        PacketParserUtils.addPacketExtension(extensions, parser, elementName, namespace);
                    }
                }
            }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("error")) {
                        break outerloop;
                    }
                }
        }
        // Parse the error type.
        XMPPError.Type errorType = XMPPError.Type.CANCEL;
        try {
            if (type != null) {
                errorType = XMPPError.Type.valueOf(type.toUpperCase(Locale.US));
            }
        }
        catch (IllegalArgumentException iae) {
            LOGGER.log(Level.SEVERE, "Could not find error type for " + type.toUpperCase(Locale.US), iae);
        }
        return new XMPPError(errorType, condition, message, extensions);
    }

    /**
     * Parses a packet extension sub-packet.
     *
     * @param elementName the XML element name of the packet extension.
     * @param namespace the XML namespace of the packet extension.
     * @param parser the XML parser, positioned at the starting element of the extension.
     * @return a PacketExtension.
     */
    public static PacketExtension parsePacketExtension(String elementName, String namespace,
                    XmlPullParser parser) throws XmlPullParserException,
                    IOException, SmackException {
        ParserUtils.assertAtStartTag(parser);
        // See if a provider is registered to handle the extension.
        PacketExtensionProvider<PacketExtension> provider = ProviderManager.getExtensionProvider(elementName, namespace);
        if (provider != null) {
                return provider.parse(parser);
        }
        Class<?> introspectionProvider = ProviderManager.getExtensionIntrospectionProvider(elementName, namespace);
        if (introspectionProvider != null) {
            try {
                return (PacketExtension)parseWithIntrospection(elementName, introspectionProvider, parser);
            } catch (NoSuchMethodException | SecurityException
                            | InstantiationException | IllegalAccessException
                            | IllegalArgumentException
                            | InvocationTargetException
                            | ClassNotFoundException e) {
                throw new SmackException(e);
            }
        }

        final int initialDepth = parser.getDepth();
        // No providers registered, so use a default extension.
        DefaultPacketExtension extension = new DefaultPacketExtension(elementName, namespace);
        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
                String name = parser.getName();
                // If an empty element, set the value with the empty string.
                if (parser.isEmptyElementTag()) {
                    extension.setValue(name,"");
                }
                // Otherwise, get the the element text.
                else {
                    eventType = parser.next();
                    if (eventType == XmlPullParser.TEXT) {
                        String value = parser.getText();
                        extension.setValue(name, value);
                    }
                }
                break;
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
        }
        return extension;
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
        assert(parser.getEventType() == XmlPullParser.END_TAG);
        return new StartTls(required);
    }

    private static String getLanguageAttribute(XmlPullParser parser) {
    	for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            if ( "xml:lang".equals(attributeName) ||
                    ("lang".equals(attributeName) &&
                            "xml".equals(parser.getAttributePrefix(i)))) {
    			return parser.getAttributeValue(i);
    		}
    	}
    	return null;
    }

    public static Object parseWithIntrospection(String elementName, Class<?> objectClass,
                    XmlPullParser parser) throws NoSuchMethodException, SecurityException,
                    InstantiationException, IllegalAccessException, XmlPullParserException,
                    IOException, IllegalArgumentException, InvocationTargetException,
                    ClassNotFoundException {
        boolean done = false;
        Object object = objectClass.newInstance();
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();
                String stringValue = parser.nextText();
                Class<?> propertyType = object.getClass().getMethod(
                                "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1)).getReturnType();
                // Get the value of the property by converting it from a
                // String to the correct object type.
                Object value = decode(propertyType, stringValue);
                // Set the value of the bean.
                object.getClass().getMethod(
                                "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1),
                                propertyType).invoke(object, value);
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(elementName)) {
                    done = true;
                }
            }
        }
        return object;
    }

    public static void addPacketExtension(Packet packet, XmlPullParser parser) throws XmlPullParserException,
                    IOException, SmackException {
        ParserUtils.assertAtStartTag(parser);
        addPacketExtension(packet, parser, parser.getName(), parser.getNamespace());
    }

    public static void addPacketExtension(Packet packet, XmlPullParser parser, String elementName, String namespace)
                    throws XmlPullParserException, IOException, SmackException {
        PacketExtension packetExtension = parsePacketExtension(elementName, namespace, parser);
        packet.addExtension(packetExtension);
    }

    public static void addPacketExtension(Collection<PacketExtension> collection, XmlPullParser parser)
                    throws XmlPullParserException, IOException, SmackException {
        addPacketExtension(collection, parser, parser.getName(), parser.getNamespace());
    }

    public static void addPacketExtension(Collection<PacketExtension> collection, XmlPullParser parser,
                    String elementName, String namespace) throws XmlPullParserException, IOException, SmackException {
        PacketExtension packetExtension = parsePacketExtension(elementName, namespace, parser);
        collection.add(packetExtension);
    }

    /**
     * Decodes a String into an object of the specified type. If the object
     * type is not supported, null will be returned.
     *
     * @param type the type of the property.
     * @param value the encode String value to decode.
     * @return the String value decoded into the specified type.
     * @throws ClassNotFoundException 
     */
    private static Object decode(Class<?> type, String value) throws ClassNotFoundException {
        if (type.getName().equals("java.lang.String")) {
            return value;
        }
        if (type.getName().equals("boolean")) {
            return Boolean.valueOf(value);
        }
        if (type.getName().equals("int")) {
            return Integer.valueOf(value);
        }
        if (type.getName().equals("long")) {
            return Long.valueOf(value);
        }
        if (type.getName().equals("float")) {
            return Float.valueOf(value);
        }
        if (type.getName().equals("double")) {
            return Double.valueOf(value);
        }
        if (type.getName().equals("java.lang.Class")) {
            return Class.forName(value);
        }
        return null;
    }

    /**
     * This class represents and unparsed IQ of the type 'result'. Usually it's created when no IQProvider
     * was found for the IQ element.
     * 
     * The child elements can be examined with the getChildElementXML() method.
     *
     */
    public static class UnparsedResultIQ extends IQ {
        public UnparsedResultIQ(CharSequence content) {
            this.content = content;
        }

        private final CharSequence content;

        @Override
        public CharSequence getChildElementXML() {
            return this.content;
        }
    }
}
