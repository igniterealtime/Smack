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

import java.beans.PropertyDescriptor;

import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.xmlpull.v1.XmlPullParser;

/**
 * Utility class that helps to parse packets. Any parsing packets method that must be shared
 * between many clients must be placed in this utility class.   
 * 
 * @author Gaston Dombiak
 */
public class PacketParserUtils {

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
                descriptor.getWriteMethod().invoke(object, new Object[] { value });
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
