/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@coolservlets.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */

package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.util.StringUtils;

import java.util.*;
import java.io.*;

/**
 * Base class for XMPP packets. Every packet has a unique ID (which is automatically
 * generated, but can be overriden). Optionally, the "to" and "from" fields can be set,
 * as well as an arbitrary number of properties.
 *
 * Properties provide an easy mechanism for clients to share data. Each property has a
 * String name, and a value that is a Java primitive (int, long, float, double, boolean)
 * or any Serializable object (a Java object is Serializable when it implements the
 * Serializable interface).
 *
 * @author Matt Tucker
 */
public abstract class Packet {

    /**
     * A prefix helps to make sure that ID's are unique across mutliple instances.
     */
    private static String prefix = StringUtils.randomString(3);

    /**
     * Keeps track of the current increment, which is appended to the prefix to
     * forum a unique ID.
     */
    private static long id = 0;

    /**
     * Returns the next unique id. Each id made up of a short alphanumeric
     * prefix along with a unique numeric value.
     *
     * @return the next id.
     */
    private static synchronized String nextID() {
        return prefix + Long.toString(id++);
    }

    private String packetID = nextID();
    private String to = null;
    private String from = null;
    private Map properties = new HashMap();
    private Error error = null;

    /**
     * Returns the unique ID of the packet.
     *
     * @return the packet's unique ID.
     */
    public String getPacketID() {
        return packetID;
    }

    /**
     * Sets the unique ID of the packet.
     *
     * @param packetID the unique ID for the packet.
     */
    public void setPacketID(String packetID) {
        this.packetID = packetID;
    }

    /**
     * Returns who the packet is being sent "to", or <tt>null</tt> if
     * the value is not set. The XMPP protocol often makes the "to"
     * attribute optional, so it does not always need to be set.
     *
     * @return who the packet is being sent to, or <tt>null</tt> if the
     *      value has not been set.
     */
    public String getTo() {
        return to;
    }

    /**
     * Sets who the packet is being sent "to". The XMPP protocol often makes
     * the "to" attribute optional, so it does not always need to be set.
     *
     * @param to who the packet is being sent to.
     */
    public void setTo(String to) {
        this.to = to;
    }

    /**
     * Returns who the packet is being sent "from" or <tt>null</tt> if
     * the value is not set. The XMPP protocol often makes the "from"
     * attribute optional, so it does not always need to be set.
     *
     * @return who the packet is being sent from, or <tt>null</tt> if the
     *      valud has not been set.
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets who the packet is being sent "from". The XMPP protocol often
     * makes the "from" attribute optional, so it does not always need to
     * be set.
     *
     * @param from who the packet is being sent to.
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Returns the error associated with this packet, or <tt>null</tt> if there are
     * no errors.
     *
     * @return the error sub-packet or <tt>null</tt> if there isn't an error.
     */
    public Error getError() {
        return error;
    }

    /**
     * Sets the error for this packet.
     *
     * @param error the error to associate with this packet.
     */
    public void setError(Error error) {
        this.error = error;
    }

    /**
     * Returns the packet property with the specified name or <tt>null</tt> if the
     * property doesn't exist. Property values that were orginally primitives will
     * be returned as their object equivalent. For example, an int property will be
     * returned as an Integer, a double as a Double, etc.
     *
     * @param name the name of the property.
     * @return the property, or <tt>null</tt> if the property doesn't exist.
     */
    public synchronized Object getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Sets a packet property with an int value.
     *
     * @param name the name of the property.
     * @param value the value of the property.
     */
    public void setProperty(String name, int value) {
        setProperty(name, new Integer(value));
    }

    /**
     * Sets a packet property with a long value.
     *
     * @param name the name of the property.
     * @param value the value of the property.
     */
    public void setProperty(String name, long value) {
        setProperty(name, new Long(value));
    }

    /**
     * Sets a packet property with a float value.
     *
     * @param name the name of the property.
     * @param value the value of the property.
     */
    public void setProperty(String name, float value) {
        setProperty(name, new Float(value));
    }

    /**
     * Sets a packet property with a double value.
     *
     * @param name the name of the property.
     * @param value the value of the property.
     */
    public void setProperty(String name, double value) {
        setProperty(name, new Double(value));
    }

    /**
     * Sets a packet property with a bboolean value.
     *
     * @param name the name of the property.
     * @param value the value of the property.
     */
    public void setProperty(String name, boolean value) {
        setProperty(name, new Boolean(value));
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
        properties.remove(name);
    }

    /**
     * Returns an Iterator for all the property names that are set.
     *
     * @return an Iterator for all property names.
     */
    public synchronized Iterator getPropertyNames() {
        return properties.keySet().iterator();
    }

    /**
     * Returns the packet as XML. Every concrete extension of Packet must implement
     * this method. In addition to writing out packet-specific data, each extension should
     * also write out the error and the properties data if they are defined.
     *
     * @return the XML format of the packet as a String.
     */
    public abstract String toXML();

    /**
     * Returns the properties portion of the packet as XML or <tt>null</tt> if there are
     * no properties.
     *
     * @return the properties data as XML or <tt>null</tt> if there are no properties.
     */
    protected synchronized String getPropertiesXML() {
        // Return null if there are no properties.
        if (properties.isEmpty()) {
            return null;
        }
        StringBuffer buf = new StringBuffer();
        buf.append("<x xmlns=\"http://www.jivesoftware.com/xmlns/xmpp/properties\">");
        // Loop through all properties and write them out.
        for (Iterator i=getPropertyNames(); i.hasNext(); ) {
            String name = (String)i.next();
            Object value = getProperty(name);
            buf.append("<property>");
            buf.append("<name>").append(StringUtils.escapeForXML(name)).append("</name>");
            buf.append("<value type=\"");
            if (value instanceof Integer) {
                buf.append("integer\">").append(value).append("</value>");
            }
            else if (value instanceof Long) {
                buf.append("long\">").append(value).append("</value>");
            }
            else if (value instanceof Float) {
                buf.append("float\">").append(value).append("</value>");
            }
            else if (value instanceof Double) {
                buf.append("double\">").append(value).append("</value>");
            }
            else if (value instanceof Boolean) {
                buf.append("boolean\">").append(value).append("</value>");
            }
            else if (value instanceof String) {
                buf.append("string\">");
                buf.append(StringUtils.escapeForXML((String)value));
                buf.append("</value>");
            }
            // Otherwise, it's a generic Serializable object. Serialized objects are in
            // a binary format, which won't work well inside of XML. Therefore, we base-64
            // encode the binary data before adding it to the SOAP payload.
            else {
                try {
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    ObjectOutputStream out = new ObjectOutputStream(byteStream);
                    out.writeObject(value);
                    String encodedVal = StringUtils.encodeBase64(byteStream.toByteArray());
                    buf.append("java-object\">");
                    buf.append(encodedVal).append("</value");
                }
                catch (Exception e) {

                }
            }
            buf.append("</property>");
        }
        buf.append("</x>");
        return buf.toString();
    }
}
