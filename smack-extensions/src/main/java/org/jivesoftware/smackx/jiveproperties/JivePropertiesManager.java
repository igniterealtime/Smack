/*
 *
 * Copyright 2014-2024 Florian Schmaus.
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
package org.jivesoftware.smackx.jiveproperties;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.packet.StanzaView;

import org.jivesoftware.smackx.jiveproperties.packet.JivePropertiesExtension;

public class JivePropertiesManager {

    private static boolean javaObjectEnabled = false;

    /**
     * Enables deserialization of Java objects embedded in the 'properties' stanza extension. Since
     * this is a security sensitive feature, it is disabled per default in Smack. Only enable it if
     * you are sure that you understand the potential security implications it can cause.
     * <p>
     * See also:
     * </p>
     * <ul>
     * <li> <a href="http://stackoverflow.com/questions/19054460/">"What is the security impact of deserializing untrusted data in Java?" on Stackoverflow</a>
     * </ul>
     * @param enabled true to enable Java object deserialization
     */
    public static void setJavaObjectEnabled(boolean enabled) {
        JivePropertiesManager.javaObjectEnabled = enabled;
    }

    public static boolean isJavaObjectEnabled() {
        return javaObjectEnabled;
    }

    /**
     * Convenience method to add a property to a stanza.
     *
     * @param stanzaBuilder the stanza to add the property to.
     * @param name the name of the property to add.
     * @param value the value of the property to add.
     */
    public static void addProperty(StanzaBuilder<?> stanzaBuilder, String name, Object value) {
        JivePropertiesExtension jpe = (JivePropertiesExtension) stanzaBuilder.getExtension(JivePropertiesExtension.QNAME);
        if (jpe == null) {
            jpe = new JivePropertiesExtension();
            stanzaBuilder.addExtension(jpe);
        }
        jpe.setProperty(name, value);
    }

    /**
     * Convenience method to get a property from a packet. Will return null if the stanza contains
     * not property with the given name.
     *
     * @param packet TODO javadoc me please
     * @param name TODO javadoc me please
     * @return the property or <code>null</code> if none found.
     */
    public static Object getProperty(StanzaView packet, String name) {
        Object res = null;
        JivePropertiesExtension jpe = packet.getExtension(JivePropertiesExtension.class);
        if (jpe != null) {
            res = jpe.getProperty(name);
        }
        return res;
    }

    /**
     * Return a collection of the names of all properties of the given packet. If the packet
     * contains no properties extension, then an empty collection will be returned.
     *
     * @param packet TODO javadoc me please
     * @return a collection of the names of all properties.
     */
    public static Collection<String> getPropertiesNames(Stanza packet) {
        JivePropertiesExtension jpe = (JivePropertiesExtension) packet.getExtension(JivePropertiesExtension.NAMESPACE);
        if (jpe == null) {
            return Collections.emptyList();
        }
        return jpe.getPropertyNames();
    }

    /**
     * Return a map of all properties of the given packet. If the stanza contains no properties
     * extension, an empty map will be returned.
     *
     * @param packet TODO javadoc me please
     * @return a map of all properties of the given packet.
     */
    public static Map<String, Object> getProperties(Stanza packet) {
        JivePropertiesExtension jpe = (JivePropertiesExtension) packet.getExtension(JivePropertiesExtension.NAMESPACE);
        if (jpe == null) {
            return Collections.emptyMap();
        }
        return jpe.getProperties();
    }
}
