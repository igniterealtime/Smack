/**
 *
 * Copyright © 2018 Paul Schaub
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
package org.jivesoftware.smackx.last_interaction.element;

import java.util.Date;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class IdleElement implements ExtensionElement {

    public static final String NAMESPACE = "urn:xmpp:idle:1";
    public static final String ELEMENT = "idle";
    public static final String ATTR_SINCE = "since";

    private final Date since;

    /**
     * Create a new IdleElement with the current date as date of last user interaction.
     */
    public IdleElement() {
        this(new Date());
    }

    /**
     * Create a new IdleElement.
     * @param since date of last user interaction
     */
    public IdleElement(Date since) {
        this.since = Objects.requireNonNull(since);
    }

    /**
     * Return the value of last user interaction.
     * @return date of last interaction
     */
    public Date getSince() {
        return since;
    }

    /**
     * Add an Idle element with current date to the presence.
     * @param presence presence
     */
    public static void addToPresence(Presence presence) {
        presence.addExtension(new IdleElement());
    }

    /**
     * Return the IdleElement from a presence.
     * Returns null, if no IdleElement found.
     *
     * @param presence presence
     * @return idleElement from presence or null
     */
    public static IdleElement fromPresence(Presence presence) {
        return presence.getExtension(ELEMENT, NAMESPACE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getElementName() {
        return ELEMENT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        return new XmlStringBuilder(this)
                .attribute(ATTR_SINCE, since)
                .closeEmptyElement();
    }
}
