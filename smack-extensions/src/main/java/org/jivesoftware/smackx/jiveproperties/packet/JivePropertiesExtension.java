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
package org.jivesoftware.smackx.jiveproperties.packet;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Properties provide an easy mechanism for clients to share data. Each property has a
 * String name, and a value that is a Java primitive (int, long, float, double, boolean)
 * or any Serializable object (a Java object is Serializable when it implements the
 * Serializable interface).
 *
 */
public class JivePropertiesExtension implements PacketExtension {
    /**
     * Namespace used to store packet properties.
     */
    public static final String NAMESPACE = "http://www.jivesoftware.com/xmlns/xmpp/properties";

    public static final String ELEMENT = "properties";

    private static final Logger LOGGER = Logger.getLogger(JivePropertiesExtension.class.getName());
    
    private final Map<String, Object> properties;

    public JivePropertiesExtension() {
        properties = new HashMap<String, Object>();
    }

    public JivePropertiesExtension(Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * Returns the packet property with the specified name or <tt>null</tt> if the
     * property doesn't exist. Property values that were originally primitives will
     * be returned as their object equivalent. For example, an int property will be
     * returned as an Integer, a double as a Double, etc.
     *
     * @param name the name of the property.
     * @return the property, or <tt>null</tt> if the property doesn't exist.
     */
    public synchronized Object getProperty(String name) {
        if (properties == null) {
            return null;
        }
        return properties.get(name);
    }

    /**
     * Sets a property with an Object as the value. The value must be Serializable
     * or an IllegalArgumentException will be thrown.
     *
     * @param name the name of the property.
     * @param value the value of the property.
     */
    public synchronized void setProperty(String name, Object value) {
        if (!(value instanceof Serializable)) {
            throw new IllegalArgumentException("Value must be serialiazble");
        }
        properties.put(name, value);
    }

    /**
     * Deletes a property.
     *
     * @param name the name of the property to delete.
     */
    public synchronized void deleteProperty(String name) {
        if (properties == null) {
            return;
        }
        properties.remove(name);
    }

    /**
     * Returns an unmodifiable collection of all the property names that are set.
     *
     * @return all property names.
     */
    public synchronized Collection<String> getPropertyNames() {
        if (properties == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<String>(properties.keySet()));
    }

    /**
     * Returns an unmodifiable map of all properties.
     *
     * @return all properties.
     */
    public synchronized Map<String, Object> getProperties() {
        if (properties == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<String, Object>(properties));
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngelBracket();
        // Loop through all properties and write them out.
        for (String name : getPropertyNames()) {
            Object value = getProperty(name);
            xml.openElement("property");
            xml.element("name", name);
            xml.halfOpenElement("value");

            String type;
            String valueStr;
            if (value instanceof Integer) {
                type = "integer";
                valueStr = Integer.toString((Integer) value);
            }
            else if (value instanceof Long) {
                type = "long";
                valueStr = Long.toString((Long) value);
            }
            else if (value instanceof Float) {
                type = "float";
                valueStr = Float.toString((Float) value);
            }
            else if (value instanceof Double) {
                type = "double";
                valueStr = Double.toString((Double) value);
            }
            else if (value instanceof Boolean) {
                type = "boolean";
                valueStr = Boolean.toString((Boolean) value);
            }
            else if (value instanceof String) {
                type = "string";
                valueStr = (String) value;
            }
            // Otherwise, it's a generic Serializable object. Serialized objects are in
            // a binary format, which won't work well inside of XML. Therefore, we base-64
            // encode the binary data before adding it.
            else {
                ByteArrayOutputStream byteStream = null;
                ObjectOutputStream out = null;
                try {
                    byteStream = new ByteArrayOutputStream();
                    out = new ObjectOutputStream(byteStream);
                    out.writeObject(value);
                    type = "java-object";
                    valueStr = StringUtils.encodeBase64(byteStream.toByteArray());
                }
                catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error encoding java object", e);
                    type = "java-object";
                    valueStr = "Serializing error: " + e.getMessage();
                }
                finally {
                    if (out != null) {
                        try {
                            out.close();
                        }
                        catch (Exception e) {
                            // Ignore.
                        }
                    }
                    if (byteStream != null) {
                        try {
                            byteStream.close();
                        }
                        catch (Exception e) {
                            // Ignore.
                        }
                    }
                }
            }
            xml.attribute("type", type);
            xml.rightAngelBracket();
            xml.escape(valueStr);
            xml.closeElement("value");
            xml.closeElement("property");
        }
        xml.closeElement(this);

        return xml;
    }

}
