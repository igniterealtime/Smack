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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Bind;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.sasl.SASLMechanism.SASLFailure;
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
        return parser;
    }

    /**
     * Parses a message packet.
     *
     * @param parser the XML parser, positioned at the start of a message packet.
     * @return a Message packet.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    public static Message parseMessage(XmlPullParser parser) throws Exception {
        Message message = new Message();
        String id = parser.getAttributeValue("", "id");
        message.setPacketID(id == null ? Packet.ID_NOT_AVAILABLE : id);
        message.setTo(parser.getAttributeValue("", "to"));
        message.setFrom(parser.getAttributeValue("", "from"));
        message.setType(Message.Type.fromString(parser.getAttributeValue("", "type")));
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
        boolean done = false;
        String thread = null;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                if (elementName.equals("subject")) {
                    String xmlLang = getLanguageAttribute(parser);
                    if (xmlLang == null) {
                        xmlLang = defaultLanguage;
                    }

                    String subject = parseElementText(parser);

                    if (message.getSubject(xmlLang) == null) {
                        message.addSubject(xmlLang, subject);
                    }
                }
                else if (elementName.equals("body")) {
                    String xmlLang = getLanguageAttribute(parser);
                    if (xmlLang == null) {
                        xmlLang = defaultLanguage;
                    }

                    String body = parseElementText(parser);

                    if (message.getBody(xmlLang) == null) {
                        message.addBody(xmlLang, body);
                    }
                }
                else if (elementName.equals("thread")) {
                    if (thread == null) {
                        thread = parser.nextText();
                    }
                }
                else if (elementName.equals("error")) {
                    message.setError(parseError(parser));
                }
                // Otherwise, it must be a packet extension.
                else {
                    message.addExtension(
                    PacketParserUtils.parsePacketExtension(elementName, namespace, parser));
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("message")) {
                    done = true;
                }
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
    public static String parseElement(XmlPullParser parser) throws XmlPullParserException, IOException {
        return parseElement(parser, false);
    }

    public static String parseElement(XmlPullParser parser,
                    boolean fullNamespaces) throws XmlPullParserException,
                    IOException {
        assert (parser.getEventType() == XmlPullParser.START_TAG);
        return parseContentDepth(parser, parser.getDepth(), fullNamespaces);
    }

    /**
     * Returns the content of a element as string.
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
     * @return the content of a tag as string
     * @throws XmlPullParserException if parser encounters invalid XML
     * @throws IOException if an IO error occurs
     */
    public static String parseContent(XmlPullParser parser)
                    throws XmlPullParserException, IOException {
        assert(parser.getEventType() == XmlPullParser.START_TAG);
        if (parser.isEmptyElementTag()) {
            return "";
        }
        // Advance the parser, since we want to parse the content of the current element
        parser.next();
        return parseContentDepth(parser, parser.getDepth(), false);
    }

    public static String parseContentDepth(XmlPullParser parser, int depth)
                    throws XmlPullParserException, IOException {
        return parseContentDepth(parser, depth, false);
    }

    /**
     * Returns the content from the current position of the parser up to the closing tag of the
     * given depth. Note that only the outermost namespace attributes ("xmlns") will be returned,
     * not nested ones, if <code>fullNamespaces</code> is false. If it is true, then namespaces of
     * parent elements will be added to child elements that don't define a different namespace.
     * <p>
     * This method is able to parse the content with MX- and KXmlParser. In order to achieve
     * this some trade-off has to be make, because KXmlParser does not support xml-roundtrip (ie.
     * return a String on getText() on START_TAG and END_TAG). We are therefore required to work
     * around this limitation, which results in only partial support for XML namespaces ("xmlns"):
     * Only the outermost namespace of elements will be included in the resulting String, if
     * <code>fullNamespaces</code> is set to false.
     * </p>
     * 
     * @param parser
     * @param depth
     * @param fullNamespaces
     * @return the content of the current depth
     * @throws XmlPullParserException
     * @throws IOException
     */
    public static String parseContentDepth(XmlPullParser parser, int depth, boolean fullNamespaces) throws XmlPullParserException, IOException {
        XmlStringBuilder xml = new XmlStringBuilder();
        int event = parser.getEventType();
        boolean isEmptyElement = false;
        // XmlPullParser reports namespaces in nested elements even if *only* the outer ones defines
        // it. This 'flag' ensures that when a namespace is set for an element, it won't be set again
        // in a nested element. It's an ugly workaround that has the potential to break things.
        String namespaceElement = null;;
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
                    xml.rightAngelBracket();
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
        return xml.toString();
    }

    /**
     * Parses a presence packet.
     *
     * @param parser the XML parser, positioned at the start of a presence packet.
     * @return a Presence packet.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    public static Presence parsePresence(XmlPullParser parser) throws Exception {
        Presence.Type type = Presence.Type.available;
        String typeString = parser.getAttributeValue("", "type");
        if (typeString != null && !typeString.equals("")) {
            try {
                type = Presence.Type.valueOf(typeString);
            }
            catch (IllegalArgumentException iae) {
                LOGGER.warning("Found invalid presence type " + typeString);
            }
        }
        Presence presence = new Presence(type);
        presence.setTo(parser.getAttributeValue("", "to"));
        presence.setFrom(parser.getAttributeValue("", "from"));
        String id = parser.getAttributeValue("", "id");
        presence.setPacketID(id == null ? Packet.ID_NOT_AVAILABLE : id);

        String language = getLanguageAttribute(parser);
        if (language != null && !"".equals(language.trim())) {
        	presence.setLanguage(language);
        }
        presence.setPacketID(id == null ? Packet.ID_NOT_AVAILABLE : id);

        // Parse sub-elements
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                if (elementName.equals("status")) {
                    presence.setStatus(parser.nextText());
                }
                else if (elementName.equals("priority")) {
                    try {
                        int priority = Integer.parseInt(parser.nextText());
                        presence.setPriority(priority);
                    }
                    catch (NumberFormatException nfe) {
                        // Ignore.
                    }
                    catch (IllegalArgumentException iae) {
                        // Presence priority is out of range so assume priority to be zero
                        presence.setPriority(0);
                    }
                }
                else if (elementName.equals("show")) {
                    String modeText = parser.nextText();
                    try {
                        presence.setMode(Presence.Mode.valueOf(modeText));
                    }
                    catch (IllegalArgumentException iae) {
                        LOGGER.warning("Found invalid presence mode " + modeText);
                    }
                }
                else if (elementName.equals("error")) {
                    presence.setError(parseError(parser));
                }
                // Otherwise, it must be a packet extension.
                else {
                	try {
                        presence.addExtension(PacketParserUtils.parsePacketExtension(elementName, namespace, parser));
                	}
                	catch (Exception e) {
                		LOGGER.warning("Failed to parse extension packet in Presence packet.");
                	}
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("presence")) {
                    done = true;
                }
            }
        }
        return presence;
    }

    /**
     * Parses an IQ packet.
     *
     * @param parser the XML parser, positioned at the start of an IQ packet.
     * @return an IQ object.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    public static IQ parseIQ(XmlPullParser parser, XMPPConnection connection) throws Exception {
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
                else if (elementName.equals("query") && namespace.equals("jabber:iq:roster")) {
                    iqPacket = parseRoster(parser);
                }
                else if (elementName.equals("query") && namespace.equals("jabber:iq:register")) {
                    iqPacket = parseRegistration(parser);
                }
                else if (elementName.equals("bind") &&
                        namespace.equals("urn:ietf:params:xml:ns:xmpp-bind")) {
                    iqPacket = parseResourceBinding(parser);
                }
                // Otherwise, see if there is a registered provider for
                // this element name and namespace.
                else {
                    Object provider = ProviderManager.getIQProvider(elementName, namespace);
                    if (provider != null) {
                        if (provider instanceof IQProvider) {
                            iqPacket = ((IQProvider)provider).parseIQ(parser);
                        }
                        else if (provider instanceof Class) {
                            iqPacket = (IQ)PacketParserUtils.parseWithIntrospection(elementName,
                                    (Class<?>)provider, parser);
                        }
                    }
                    // Only handle unknown IQs of type result. Types of 'get' and 'set' which are not understood
                    // have to be answered with an IQ error response. See the code a few lines below
                    else if (IQ.Type.RESULT == type){
                        // No Provider found for the IQ stanza, parse it to an UnparsedIQ instance
                        // so that the content of the IQ can be examined later on
                        iqPacket = new UnparsedResultIQ(parseContent(parser));
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
            if (IQ.Type.GET == type || IQ.Type.SET == type ) {
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
                iqPacket.setType(IQ.Type.ERROR);
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

    private static RosterPacket parseRoster(XmlPullParser parser) throws Exception {
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

     private static Registration parseRegistration(XmlPullParser parser) throws Exception {
        Registration registration = new Registration();
        Map<String, String> fields = null;
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                // Any element that's in the jabber:iq:register namespace,
                // attempt to parse it if it's in the form <name>value</name>.
                if (parser.getNamespace().equals("jabber:iq:register")) {
                    String name = parser.getName();
                    String value = "";
                    if (fields == null) {
                        fields = new HashMap<String, String>();
                    }

                    if (parser.next() == XmlPullParser.TEXT) {
                        value = parser.getText();
                    }
                    // Ignore instructions, but anything else should be added to the map.
                    if (!name.equals("instructions")) {
                        fields.put(name, value);
                    }
                    else {
                        registration.setInstructions(value);
                    }
                }
                // Otherwise, it must be a packet extension.
                else {
                    registration.addExtension(
                        PacketParserUtils.parsePacketExtension(
                            parser.getName(),
                            parser.getNamespace(),
                            parser));
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }
        }
        registration.setAttributes(fields);
        return registration;
    }

    private static Bind parseResourceBinding(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        Bind bind = new Bind();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("resource")) {
                    bind.setResource(parser.nextText());
                }
                else if (parser.getName().equals("jid")) {
                    bind.setJid(parser.nextText());
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("bind")) {
                    done = true;
                }
            }
        }

        return bind;
    }

    /**
     * Parse the available SASL mechanisms reported from the server.
     *
     * @param parser the XML parser, positioned at the start of the mechanisms stanza.
     * @return a collection of Stings with the mechanisms included in the mechanisms stanza.
     * @throws Exception if an exception occurs while parsing the stanza.
     */
    public static Collection<String> parseMechanisms(XmlPullParser parser) throws Exception {
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
     * Parse the available compression methods reported from the server.
     *
     * @param parser the XML parser, positioned at the start of the compression stanza.
     * @return a collection of Stings with the methods included in the compression stanza.
     * @throws XmlPullParserException if an exception occurs while parsing the stanza.
     */
    public static Collection<String> parseCompressionMethods(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        List<String> methods = new ArrayList<String>();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                String elementName = parser.getName();
                if (elementName.equals("method")) {
                    methods.add(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("compression")) {
                    done = true;
                }
            }
        }
        return methods;
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
                if (name.equals("text") && !parser.isEmptyElementTag()) {
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
     * @throws Exception if an exception occurs while parsing the packet.
     */
    public static XMPPError parseError(XmlPullParser parser) throws Exception {
        final String errorNamespace = "urn:ietf:params:xml:ns:xmpp-stanzas";
        String type = null;
        String message = null;
        String condition = null;
        List<PacketExtension> extensions = new ArrayList<PacketExtension>();

        // Parse the error header
        for (int i=0; i<parser.getAttributeCount(); i++) {
            if (parser.getAttributeName(i).equals("type")) {
            	type = parser.getAttributeValue("", "type");
            }
        }
        boolean done = false;
        // Parse the text and condition tags
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("text")) {
                    message = parser.nextText();
                }
                else {
                	// Condition tag, it can be xmpp error or an application defined error.
                    String elementName = parser.getName();
                    String namespace = parser.getNamespace();
                    if (errorNamespace.equals(namespace)) {
                    	condition = elementName;
                    }
                    else {
                    	extensions.add(parsePacketExtension(elementName, namespace, parser));
                    }
                }
            }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("error")) {
                        done = true;
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
     * @throws Exception if a parsing error occurs.
     */
    public static PacketExtension parsePacketExtension(String elementName, String namespace, XmlPullParser parser)
            throws Exception
    {
        // See if a provider is registered to handle the extension.
        Object provider = ProviderManager.getExtensionProvider(elementName, namespace);
        if (provider != null) {
            if (provider instanceof PacketExtensionProvider) {
                return ((PacketExtensionProvider)provider).parseExtension(parser);
            }
            else if (provider instanceof Class) {
                return (PacketExtension)parseWithIntrospection(
                        elementName, (Class<?>)provider, parser);
            }
        }
        // No providers registered, so use a default extension.
        DefaultPacketExtension extension = new DefaultPacketExtension(elementName, namespace);
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
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
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(elementName)) {
                    done = true;
                }
            }
        }
        return extension;
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

    public static Object parseWithIntrospection(String elementName,
            Class<?> objectClass, XmlPullParser parser) throws Exception
    {
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

    /**
     * Decodes a String into an object of the specified type. If the object
     * type is not supported, null will be returned.
     *
     * @param type the type of the property.
     * @param value the encode String value to decode.
     * @return the String value decoded into the specified type.
     * @throws Exception If decoding failed due to an error.
     */
    private static Object decode(Class<?> type, String value) throws Exception {
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
        public UnparsedResultIQ(String content) {
            this.str = content;
        }

        private final String str;

        @Override
        public String getChildElementXML() {
            return this.str;
        }
    }
}
