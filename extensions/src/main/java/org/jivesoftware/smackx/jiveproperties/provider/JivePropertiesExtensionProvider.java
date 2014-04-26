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
package org.jivesoftware.smackx.jiveproperties.provider;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;
import org.jivesoftware.smackx.jiveproperties.packet.JivePropertiesExtension;
import org.xmlpull.v1.XmlPullParser;

public class JivePropertiesExtensionProvider implements PacketExtensionProvider {

    private static final Logger LOGGER = Logger.getLogger(JivePropertiesExtensionProvider.class.getName());

    /**
     * Parse a properties sub-packet. If any errors occur while de-serializing Java object
     * properties, an exception will be printed and not thrown since a thrown exception will shut
     * down the entire connection. ClassCastExceptions will occur when both the sender and receiver
     * of the packet don't have identical versions of the same class.
     * <p>
     * Note that you have to explicitly enabled Java object deserialization with @{link
     * {@link JivePropertiesManager#setJavaObjectEnabled(boolean)}
     * 
     * @param parser the XML parser, positioned at the start of a properties sub-packet.
     * @return a map of the properties.
     * @throws Exception if an error occurs while parsing the properties.
     */
    @Override
    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
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
                                value = Integer.valueOf(valueText);
                            }
                            else if ("long".equals(type))  {
                                value = Long.valueOf(valueText);
                            }
                            else if ("float".equals(type)) {
                                value = Float.valueOf(valueText);
                            }
                            else if ("double".equals(type)) {
                                value = Double.valueOf(valueText);
                            }
                            else if ("boolean".equals(type)) {
                                value = Boolean.valueOf(valueText);
                            }
                            else if ("string".equals(type)) {
                                value = valueText;
                            }
                            else if ("java-object".equals(type)) {
                                if (JivePropertiesManager.isJavaObjectEnabled()) {
                                    try {
                                        byte[] bytes = StringUtils.decodeBase64(valueText);
                                        ObjectInputStream in = new ObjectInputStream(
                                                        new ByteArrayInputStream(bytes));
                                        value = in.readObject();
                                    }
                                    catch (Exception e) {
                                        LOGGER.log(Level.SEVERE, "Error parsing java object", e);
                                    }
                                }
                                else {
                                    LOGGER.severe("JavaObject is not enabled. Enable with JivePropertiesManager.setJavaObjectEnabled(true)");
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
                if (parser.getName().equals(JivePropertiesExtension.ELEMENT)) {
                    break;
                }
            }
        }
        return new JivePropertiesExtension(properties);
    }

}
