/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2004 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.xmlpull.v1.XmlPullParser;

import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class that helps to parse packets. Any parsing packets method that must be shared
 * between many clients must be placed in this utility class.
 *
 * @author Gaston Dombiak
 */
public class PacketParserUtils {

    /**
     * Namespace used to store packet properties.
     */
    private static final String PROPERTIES_NAMESPACE =
            "http://www.jivesoftware.com/xmlns/xmpp/properties";

    /**
     * Parses a message packet.
     *
     * @param parser the XML parser, positioned at the start of a message packet.
     * @return a Message packet.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    public static Packet parseMessage(XmlPullParser parser) throws Exception {
        Message message = new Message();
        String id = parser.getAttributeValue("", "id");
        message.setPacketID(id == null ? Packet.ID_NOT_AVAILABLE : id);
        message.setTo(parser.getAttributeValue("", "to"));
        message.setFrom(parser.getAttributeValue("", "from"));
        message.setType(Message.Type.fromString(parser.getAttributeValue("", "type")));

        // Parse sub-elements. We include extra logic to make sure the values
        // are only read once. This is because it's possible for the names to appear
        // in arbitrary sub-elements.
        boolean done = false;
        String subject = null;
        String body = null;
        String thread = null;
        Map<String, Object> properties = null;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                if (elementName.equals("subject")) {
                    if (subject == null) {
                        subject = parser.nextText();
                    }
                }
                else if (elementName.equals("body")) {
                    if (body == null) {
                        body = parser.nextText();
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
                else if (elementName.equals("properties") &&
                        namespace.equals(PROPERTIES_NAMESPACE))
                {
                    properties = parseProperties(parser);
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
        message.setSubject(subject);
        message.setBody(body);
        message.setThread(thread);
        // Set packet properties.
        if (properties != null) {
            for (String name : properties.keySet()) {
                message.setProperty(name, properties.get(name));
            }
        }
        return message;
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
                System.err.println("Found invalid presence type " + typeString);
            }
        }

        Presence presence = new Presence(type);
        presence.setTo(parser.getAttributeValue("", "to"));
        presence.setFrom(parser.getAttributeValue("", "from"));
        String id = parser.getAttributeValue("", "id");
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
                        System.err.println("Found invalid presence mode " + modeText);
                    }
                }
                else if (elementName.equals("error")) {
                    presence.setError(parseError(parser));
                }
                else if (elementName.equals("properties") &&
                        namespace.equals(PROPERTIES_NAMESPACE))
                {
                    Map<String,Object> properties = parseProperties(parser);
                    // Set packet properties.
                    for (String name : properties.keySet()) {
                        presence.setProperty(name, properties.get(name));
                    }
                }
                // Otherwise, it must be a packet extension.
                else {
                    presence.addExtension(
                        PacketParserUtils.parsePacketExtension(elementName, namespace, parser));
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
     * Parse a properties sub-packet. If any errors occur while de-serializing Java object
     * properties, an exception will be printed and not thrown since a thrown
     * exception will shut down the entire connection. ClassCastExceptions will occur
     * when both the sender and receiver of the packet don't have identical versions
     * of the same class.
     *
     * @param parser the XML parser, positioned at the start of a properties sub-packet.
     * @return a map of the properties.
     * @throws Exception if an error occurs while parsing the properties.
     */
    public static Map<String, Object> parseProperties(XmlPullParser parser) throws Exception {
        Map<String, Object> properties = new HashMap<String, Object>();
        while (true) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("property")) {
                // Parse a property
                boolean done = false;
                String name = null;
                String type = null;
                String valueText = null;
                Object value = null;
                while (!done) {
                    eventType = parser.next();
                    if (eventType == XmlPullParser.START_TAG) {
                        String elementName = parser.getName();
                        if (elementName.equals("name")) {
                            name = parser.nextText();
                        }
                        else if (elementName.equals("value")) {
                            type = parser.getAttributeValue("", "type");
                            valueText = parser.nextText();
                        }
                    }
                    else if (eventType == XmlPullParser.END_TAG) {
                        if (parser.getName().equals("property")) {
                            if ("integer".equals(type)) {
                                value = new Integer(valueText);
                            }
                            else if ("long".equals(type))  {
                                value = new Long(valueText);
                            }
                            else if ("float".equals(type)) {
                                value = new Float(valueText);
                            }
                            else if ("double".equals(type)) {
                                value = new Double(valueText);
                            }
                            else if ("boolean".equals(type)) {
                                value = Boolean.valueOf(valueText);
                            }
                            else if ("string".equals(type)) {
                                value = valueText;
                            }
                            else if ("java-object".equals(type)) {
                                try {
                                    byte [] bytes = StringUtils.decodeBase64(valueText);
                                    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
                                    value = in.readObject();
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (name != null && value != null) {
                                properties.put(name, value);
                            }
                            done = true;
                        }
                    }
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("properties")) {
                    break;
                }
            }
        }
        return properties;
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
    	String errorCode = "-1";
        String type = "";
        String message = null;
        String condition = null;
        List<PacketExtension> extensions = new ArrayList<PacketExtension>();

        // Parse the error header
        for (int i=0; i<parser.getAttributeCount(); i++) {
            if (parser.getAttributeName(i).equals("code")) {
                errorCode = parser.getAttributeValue("", "code");
            }
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
            errorType = XMPPError.Type.valueOf(type.toUpperCase());
        }
        catch (IllegalArgumentException iae) {
            // Print stack trace. We shouldn't be getting an illegal error type.
            iae.printStackTrace();
        }
        return new XMPPError(Integer.parseInt(errorCode), errorType, condition, message, extensions);
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
                        elementName, (Class)provider, parser);
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

    public static Object parseWithIntrospection(String elementName,
            Class objectClass, XmlPullParser parser) throws Exception
    {
        boolean done = false;
        Object object = objectClass.newInstance();
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();
                String stringValue = parser.nextText();
                PropertyDescriptor descriptor = new PropertyDescriptor(name, objectClass);
                // Load the class type of the property.
                Class propertyType = descriptor.getPropertyType();
                // Get the value of the property by converting it from a
                // String to the correct object type.
                Object value = decode(propertyType, stringValue);
                // Set the value of the bean.
                descriptor.getWriteMethod().invoke(object, value);
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
     */
    private static Object decode(Class type, String value) throws Exception {
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
}