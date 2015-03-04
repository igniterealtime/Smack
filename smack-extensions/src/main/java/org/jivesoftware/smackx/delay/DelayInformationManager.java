/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smackx.delay;

import java.util.Date;

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.delay.packet.DelayInformation;

/**
 * Delayed Delivery (XEP-203)
 *
 * @author Florian Schmaus
 * @see <a href="http://xmpp.org/extensions/xep-0203.html">Delayed Delivery (XEP-203)</a>
 *
 */
public class DelayInformationManager {

    public static final String LEGACY_DELAYED_DELIVERY_NAMESPACE = "jabber:x:delay";
    public static final String LEGACY_DELAYED_DELIVERY_ELEMENT = "x";


    /**
     * Get Delayed Delivery information as defined in XEP-203
     * <p>
     * Prefer {@link #getDelayInformation(Stanza)} over this method for backwards compatibility.
     * </p>
     * @param packet
     * @return the Delayed Delivery information or <code>null</code>
     */
    public static DelayInformation getXep203DelayInformation(Stanza packet) {
        return DelayInformation.from(packet);
    }

    /**
     * Get Delayed Delivery information as defined in XEP-91
     * <p>
     * Prefer {@link #getDelayInformation(Stanza)} over this method for backwards compatibility.
     * </p>
     * @param packet
     * @return the Delayed Delivery information or <code>null</code>
     */
    public static DelayInformation getLegacyDelayInformation(Stanza packet) {
        return packet.getExtension(LEGACY_DELAYED_DELIVERY_ELEMENT, LEGACY_DELAYED_DELIVERY_NAMESPACE);
    }

    /**
     * Get Delayed Delivery information. This method first looks for a PacketExtension with the
     * XEP-203 namespace and falls back to the XEP-91 namespace.
     *
     * @param packet
     * @return the Delayed Delivery information or <code>null</code>
     */
    public static DelayInformation getDelayInformation(Stanza packet) {
        DelayInformation delayInformation = getXep203DelayInformation(packet);
        if (delayInformation != null) {
            return delayInformation;
        }
        return getLegacyDelayInformation(packet);
    }

    /**
     * Get the Delayed Delivery timestamp or <code>null</code>
     *
     * @param packet
     * @return the Delayed Delivery timestamp or <code>null</code>
     */
    public static Date getDelayTimestamp(Stanza packet) {
        DelayInformation delayInformation = getDelayInformation(packet);
        if (delayInformation == null) {
            return null;
        }
        return delayInformation.getStamp();
    }

     /**
     * Check if the given stanza is a delayed stanza as of XEP-203.
     *
     * @param packet
     * @return true if the stanza got delayed.
     */
    public static boolean isDelayedStanza(Stanza packet) {
        ExtensionElement packetExtension = getDelayInformation(packet);
        return packetExtension != null;
    }
}
