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
 *    contact webmaster@jivesoftware.com.
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

import java.util.*;

/**
 * Default implementation of the PacketExtension interface. Unless a PacketExtensionProvider
 * is registered with {@link org.jivesoftware.smack.provider.ProviderManager ProviderManager},
 * instances of this class will be returned when getting packet extensions.<p>
 *
 * This class provides a very simple representation of an XML sub-document. Each element
 * is a key in a Map with its CDATA being the value. For example, given the following
 * XML sub-document:
 *
 * <pre>
 * &lt;foo xmlns="http://bar.com"&gt;
 *     &lt;color&gt;blue&lt;/color&gt;
 *     &lt;food&gt;pizza&lt;/food&gt;
 * &lt;/foo&gt;</pre>
 *
 * In this case, getValue("color") would return "blue", and getValue("food") would
 * return "pizza". This parsing mechanism mechanism is very simplistic and will not work
 * as desired in all cases (for example, if some of the elements have attributes. In those
 * cases, a custom PacketExtensionProvider should be used.
 *
 * @author Matt Tucker
 */
public class DefaultPacketExtension implements PacketExtension {

    private String elementName;
    private String namespace;
    private Map map;

    /**
     * Creates a new generic packet extension.
     *
     * @param elementName the name of the element of the XML sub-document.
     * @param namespace the namespace of the element.
     */
    public DefaultPacketExtension(String elementName, String namespace) {
        this.elementName = elementName;
        this.namespace = namespace;
    }

     /**
     * Returns the XML element name of the extension sub-packet root element.
     *
     * @return the XML element name of the packet extension.
     */
    public String getElementName() {
        return elementName;
    }

    /**
     * Returns the XML namespace of the extension sub-packet root element.
     *
     * @return the XML namespace of the packet extension.
     */
    public String getNamespace() {
        return namespace;
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<").append(elementName).append(" xmlns=\"").append(namespace).append("\">");
        for (Iterator i=getNames(); i.hasNext(); ) {
            String name = (String)i.next();
            String value = getValue(name);
            buf.append("<").append(name).append(">");
            buf.append(value);
            buf.append("</").append(name).append(">");
        }
        buf.append("</").append(elementName).append(">");
        return buf.toString();
    }

    /**
     * Returns an Iterator for the names that can be used to get
     * values of the packet extension.
     *
     * @return an Iterator for the names.
     */
    public synchronized Iterator getNames() {
        if (map == null) {
            Collections.EMPTY_LIST.iterator();
        }
        return Collections.unmodifiableMap(new HashMap(map)).keySet().iterator();
    }

    /**
     * Returns a packet extension value given a name.
     *
     * @param name the name.
     * @return the value.
     */
    public synchronized String getValue(String name) {
        if (map == null) {
            return null;
        }
        return (String)map.get(name);
    }

    /**
     * Sets a packet extension value using the given name.
     *
     * @param name the name.
     * @param value the value.
     */
    public synchronized void setValue(String name, String value) {
        if (map == null) {
            map = new HashMap();
        }
        map.put(name, value);
    }
}
