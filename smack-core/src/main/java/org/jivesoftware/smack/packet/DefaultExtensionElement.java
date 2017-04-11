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

package org.jivesoftware.smack.packet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Default implementation of the ExtensionElement interface. Unless a ExtensionElementProvider
 * is registered with {@link org.jivesoftware.smack.provider.ProviderManager ProviderManager},
 * instances of this class will be returned when getting stanza(/packet) extensions.<p>
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
 * return "pizza". This parsing mechanism is very simplistic and will not work
 * as desired in all cases (for example, if some of the elements have attributes. In those
 * cases, a custom ExtensionElementProvider should be used.
 *
 * @author Matt Tucker
 * @deprecated use {@link org.jivesoftware.smack.packet.StandardExtensionElement} instead.
 */
@Deprecated
public class DefaultExtensionElement implements ExtensionElement {

    private String elementName;
    private String namespace;
    private Map<String,String> map;

    /**
     * Creates a new generic stanza(/packet) extension.
     *
     * @param elementName the name of the element of the XML sub-document.
     * @param namespace the namespace of the element.
     */
    public DefaultExtensionElement(String elementName, String namespace) {
        this.elementName = elementName;
        this.namespace = namespace;
    }

     /**
     * Returns the XML element name of the extension sub-packet root element.
     *
     * @return the XML element name of the stanza(/packet) extension.
     */
    @Override
    public String getElementName() {
        return elementName;
    }

    /**
     * Returns the XML namespace of the extension sub-packet root element.
     *
     * @return the XML namespace of the stanza(/packet) extension.
     */
    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder buf = new XmlStringBuilder();
        buf.halfOpenElement(elementName).xmlnsAttribute(namespace).rightAngleBracket();
        for (String name : getNames()) {
            String value = getValue(name);
            buf.element(name, value);
        }
        buf.closeElement(elementName);
        return buf;
    }

    /**
     * Returns an unmodifiable collection of the names that can be used to get
     * values of the stanza(/packet) extension.
     *
     * @return the names.
     */
    public synchronized Collection<String> getNames() {
        if (map == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashMap<String,String>(map).keySet());
    }

    /**
     * Returns a stanza(/packet) extension value given a name.
     *
     * @param name the name.
     * @return the value.
     */
    public synchronized String getValue(String name) {
        if (map == null) {
            return null;
        }
        return map.get(name);
    }

    /**
     * Sets a stanza(/packet) extension value using the given name.
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
