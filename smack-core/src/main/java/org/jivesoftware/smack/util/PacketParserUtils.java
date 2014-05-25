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

    public static XmlPullParser getParserFor(String stanza) throws XmlPullParserException, IOException {
        return getParserFor(new StringReader(stanza));
    }

    public static XmlPullParser getParserFor(Reader reader) throws XmlPullParserException, IOException {
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(reader);

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
        final int eventType = parser.getEventType();
        if (eventType != XmlPullParser.START_TAG) {
            throw new IllegalArgumentException("Parser not at start tag");
        }
        final String name = parser.getName();
        switch (name) {
        case "message":
            return parseMessage(parser);
        case "iq":
            return parseIQ(parser, connection);
        case "presence":
            return parsePresence(parser);
        default:
            return null;
        }
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

                    String subject = parseContent(parser);

                    if (message.getSubject(xmlLang) == null) {
                        message.addSubject(xmlLang, subject);
                    }
                }
                else if (elementName.equals("body")) {
                    String xmlLang = getLanguageAttribute(parser);
                    if (xmlLang == null) {
                        xmlLang = defaultLanguage;
                    }

                    String body = parseContent(parser);
                    
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
     * Returns the content of a tag as string regardless of any tags included.
     * 
     * @param parser the XML pull parser
     * @return the content of a tag as string
     * @throws XmlPullParserException if parser encounters invalid XML
     * @throws IOException if an IO error occurs
     */
    private static String parseContent(XmlPullParser parser)
                    throws XmlPullParserException, IOException {
        int parserDepth = parser.getDepth();
        return parseContentDepth(parser, parserDepth);
    }

    public static String parseContentDepth(XmlPullParser parser, int depth) throws XmlPullParserException, IOException {
        StringBuilder content = new StringBuilder();
        while (!(parser.next() == XmlPullParser.END_TAG && parser.getDepth() == depth)) {
            String text = parser.getText();
            if (text == null) {
                throw new IllegalStateException("Parser should never return 'null' on getText() here");
            }
            content.append(text);
        }
        return content.toString();
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
     * @param connection the optional XMPPConnection used to send feature-not-implemented replies.
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
            if (connection != null && (IQ.Type.GET == type || IQ.Type.SET == type)) {
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
     * @throws Exception 
     */
    public static PacketExtension parsePacketExtension(String elementName, String namespace,
                    XmlPullParser parser) throws Exception {
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
