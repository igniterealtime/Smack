/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.packet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Default implementation of the PrivateData interface. Unless a PrivateDataProvider
 * is registered with the PrivateDataManager class, instances of this class will be
 * returned when getting private data.<p>
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
 * cases, a custom {@link org.jivesoftware.smackx.provider.PrivateDataProvider} should be used.
 *
 * @author Matt Tucker
 */
public class DefaultPrivateData implements PrivateData {

    private String elementName;
    private String namespace;
    private Map<String, String> map;

    /**
     * Creates a new generic private data object.
     *
     * @param elementName the name of the element of the XML sub-document.
     * @param namespace the namespace of the element.
     */
    public DefaultPrivateData(String elementName, String namespace) {
        this.elementName = elementName;
        this.namespace = namespace;
    }

     /**
     * Returns the XML element name of the private data sub-packet root element.
     *
     * @return the XML element name of the packet extension.
     */
    public String getElementName() {
        return elementName;
    }

    /**
     * Returns the XML namespace of the private data sub-packet root element.
     *
     * @return the XML namespace of the packet extension.
     */
    public String getNamespace() {
        return namespace;
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<").append(elementName).append(" xmlns=\"").append(namespace).append("\">");
        for (Iterator<String> i=getNames(); i.hasNext(); ) {
            String name = i.next();
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
     * values of the private data.
     *
     * @return an Iterator for the names.
     */
    public synchronized Iterator<String> getNames() {
        if (map == null) {
            return Collections.<String>emptyList().iterator();
        }
        return Collections.unmodifiableSet(map.keySet()).iterator();
    }

    /**
     * Returns a value given a name.
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
     * Sets a value given the name.
     *
     * @param name the name.
     * @param value the value.
     */
    public synchronized void setValue(String name, String value) {
        if (map == null) {
            map = new HashMap<String,String>();
        }
        map.put(name, value);
    }
}