/**
 *
 * Copyright 2014 Vyacheslav Blinov
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
package org.jivesoftware.smackx.amp;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.amp.packet.AMPExtension;

public class AMPDeliverCondition implements AMPExtension.Condition {

    public static final String NAME = "deliver";

    /**
     * Check if server supports deliver condition
     * @param connection Smack connection instance
     * @return true if deliver condition is supported.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     */
    public static boolean isSupported(XMPPConnection connection) throws NoResponseException, XMPPErrorException, NotConnectedException {
        return AMPManager.isConditionSupported(connection, NAME);
    }

    private final Value value;

    /**
     * Create new amp deliver condition with value setted to one of defined by XEP-0079.
     * See http://xmpp.org/extensions/xep-0079.html#conditions-def-deliver
     * @param value AMPDeliveryCondition.Value instance that will be used as value parameter. Can't be null.
     */
    public AMPDeliverCondition(Value value) {
        if (value == null)
            throw new NullPointerException("Can't create AMPDeliverCondition with null value");
        this.value = value;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getValue() {
        return value.toString();
    }

    /**
     * Value for amp deliver condition as defined by XEP-0079.
     * See http://xmpp.org/extensions/xep-0079.html#conditions-def-deliver
     */
    public static enum Value {
        /**
         * The message would be immediately delivered to the intended recipient or routed to the next hop.
         */
        direct,
        /**
         * The message would be forwarded to another XMPP address or account.
         */
        forward,
        /**
         * The message would be sent through a gateway to an address or account on a non-XMPP system.
         */
        gateway,
        /**
         * The message would not be delivered at all (e.g., because the intended recipient is offline and message storage is not enabled).
         */
        none,
        /**
         * The message would be stored offline for later delivery to the intended recipient.
         */
        stored
    }
}
