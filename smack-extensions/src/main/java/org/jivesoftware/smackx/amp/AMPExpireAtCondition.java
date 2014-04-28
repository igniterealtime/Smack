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
import org.jivesoftware.smack.util.XmppDateTime;
import org.jivesoftware.smackx.amp.packet.AMPExtension;

import java.util.Date;


public class AMPExpireAtCondition implements AMPExtension.Condition {

    public static final String NAME = "expire-at";

    /**
     * Check if server supports expire-at condition
     * @param connection Smack connection instance
     * @return true if expire-at condition is supported.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     */
    public static boolean isSupported(XMPPConnection connection) throws NoResponseException, XMPPErrorException, NotConnectedException  {
        return AMPManager.isConditionSupported(connection, NAME);
    }

    private final String value;

    /**
     * Create new expire-at amp condition with value setted as XEP-0082 formatted date.
     * @param utcDateTime Date instance of time
     *                    that will be used as value parameter after formatting to XEP-0082 format. Can't be null.
     */
    public AMPExpireAtCondition(Date utcDateTime) {
        if (utcDateTime == null)
            throw new NullPointerException("Can't create AMPExpireAtCondition with null value");
        this.value = XmppDateTime.formatXEP0082Date(utcDateTime);
    }

    /**
     * Create new expire-at amp condition with defined value.
     * @param utcDateTime UTC time string that will be used as value parameter.
     *                    Should be formatted as XEP-0082 Date format. Can't be null.
     */
    public AMPExpireAtCondition(String utcDateTime) {
        if (utcDateTime == null)
            throw new NullPointerException("Can't create AMPExpireAtCondition with null value");
        this.value = utcDateTime;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getValue() {
        return value;
    }

}
