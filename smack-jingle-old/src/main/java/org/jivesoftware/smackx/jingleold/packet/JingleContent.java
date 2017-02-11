/**
 *
 * Copyright 2003-2005 Jive Software.
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
package org.jivesoftware.smackx.jingleold.packet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;

/**
 * Jingle content.
 * 
 * @author Jeff Williams
 */
public class JingleContent implements ExtensionElement {

    public static final String NODENAME = "content";
    public static final String CREATOR = "creator";
    public static final String NAME = "name";

    private String creator;
    private String name;

    private JingleDescription description;
    private final List<JingleTransport> transports = new ArrayList<JingleTransport>();

    /**
     * Creates a content description..
     */
    public JingleContent(String creator, String name) {
        super();
        this.creator = creator;
        this.name = name;
    }

    public String getCreator() {
        return creator;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the XML element name of the element.
     * 
     * @return the XML element name of the element.
     */
    @Override
    public String getElementName() {
        return NODENAME;
    }

    /**
     * Return the namespace.
     * 
     * @return The namespace
     */
    @Override
    public String getNamespace() {
        // There is no namespace for <content>
        return "";
    }

    /**
     * Sets the description for this Jingle content.
     * 
     * @param description
     *            The description
     */
    public void setDescription(JingleDescription description) {
        this.description = description;
    }

    /**
     * Gets the description for this Jingle content.
     * 
     * @return The description.
     */
    public JingleDescription getDescription() {
        return description;
    }

    /**
     * Adds a JingleTransport type to the packet.
     * 
     * @param transport
     *            the JignleTransport to add.
     */
    public void addJingleTransport(final JingleTransport transport) {
        synchronized (transports) {
            transports.add(transport);
        }
    }

    /**
     * Adds a list of transports to add to the packet.
     * 
     * @param transports
     *            the transports to add.
     */
    public void addTransports(final List<JingleTransport> transports) {
        synchronized (transports) {
            for (JingleTransport transport : transports) {
                addJingleTransport(transport);
            }
        }
    }

    /**
     * Returns an Iterator for the JingleTransports in the packet.
     * 
     * @return an Iterator for the JingleTransports in the packet.
     */
    public Iterator<JingleTransport> getJingleTransports() {
        return Collections.unmodifiableList(getJingleTransportsList()).iterator();
    }

    /**
     * Returns a list for the JingleTransports in the packet.
     * 
     * @return a list for the JingleTransports in the packet.
     */
    public List<JingleTransport> getJingleTransportsList() {
        synchronized (transports) {
            return new ArrayList<JingleTransport>(transports);
        }
    }

    /**
     * Returns a count of the JingleTransports in the Jingle packet.
     * 
     * @return the number of the JingleTransports in the Jingle packet.
     */
    public int getJingleTransportsCount() {
        synchronized (transports) {
            return transports.size();
        }
    }

    /**
     * Convert a Jingle description to XML.
     * 
     * @return a string with the XML representation
     */
    @Override
    public String toXML() {
        StringBuilder buf = new StringBuilder();

        synchronized (transports) {

            buf.append('<').append(getElementName());

            buf.append(" creator='" + creator + "' name='" + name + "'>");

            // Add the description.
            if (description != null) {
                buf.append(description.toXML());
            }

            // Add all of the transports.
            for (JingleTransport transport : transports) {
                buf.append(transport.toXML());
            }
            buf.append("</").append(getElementName()).append('>');
        }
        return buf.toString();
    }

}
