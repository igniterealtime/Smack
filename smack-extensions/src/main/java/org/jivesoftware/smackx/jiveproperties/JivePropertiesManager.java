/**
 *
 * Copyright 2014 Florian Schmaus.
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

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.jiveproperties.packet.JivePropertiesExtension;

public class JivePropertiesManager {

    private static boolean javaObjectEnabled = false;

    /**
     * Enables deserialization of Java objects embedded in the 'properties' packet extension. Since
     * this is a security sensitive feature, it is disabled per default in Smack. Only enable it if
     * you are sure that you understand the potential security implications it can cause.
     * <p>
     * See also:
     * <ul>
     * <li> <a href="http://stackoverflow.com/questions/19054460/">"What is the security impact of deserializing untrusted data in Java?" on Stackoverflow<a>
     * <ul>
     * @param enabled true to enable Java object deserialization
     */
    public static void setJavaObjectEnabled(boolean enabled) {
        JivePropertiesManager.javaObjectEnabled = enabled;
    }

    public static boolean isJavaObjectEnabled() {
        return javaObjectEnabled;
    }

    /**
     * Convenience method to add a property to a packet.
     * 
     * @param packet the packet to add the property to.
     * @param name the name of the property to add.
     * @param value the value of the property to add.
     */
    public static void addProperty(Packet packet, String name, Object value) {
        JivePropertiesExtension jpe = (JivePropertiesExtension) packet.getExtension(JivePropertiesExtension.NAMESPACE);
        if (jpe == null) {
            jpe = new JivePropertiesExtension();
            packet.addExtension(jpe);
        }
        jpe.setProperty(name, value);
    }

    /**
     * Convenience method to get a property from a packet. Will return null if the packet contains
     * not property with the given name.
     * 
     * @param packet
     * @param name
     * @return the property or <tt>null</tt> if none found.
     */
    public static Object getProperty(Packet packet, String name) {
        Object res = null;
        JivePropertiesExtension jpe = (JivePropertiesExtension) packet.getExtension(JivePropertiesExtension.NAMESPACE);
        if (jpe != null) {
            res = jpe.getProperty(name);
        }
        return res;
    }

    /**
     * Return a collection of the names of all properties of the given packet. If the packet
     * contains no properties extension, then an empty collection will be returned.
     * 
     * @param packet
     * @return a collection of the names of all properties.
     */
    public static Collection<String> getPropertiesNames(Packet packet) {
        JivePropertiesExtension jpe = (JivePropertiesExtension) packet.getExtension(JivePropertiesExtension.NAMESPACE);
        if (jpe == null) {
            return Collections.emptyList();
        }
        return jpe.getPropertyNames();
    }

    /**
     * Return a map of all properties of the given packet. If the packet contains no properties
     * extension, an empty map will be returned.
     * 
     * @param packet
     * @return a map of all properties of the given packet.
     */
    public static Map<String, Object> getProperties(Packet packet) {
        JivePropertiesExtension jpe = (JivePropertiesExtension) packet.getExtension(JivePropertiesExtension.NAMESPACE);
        if (jpe == null) {
            return Collections.emptyMap();
        }
        return jpe.getProperties();
    }
}
