/**
 *
 * Copyright 2016 Florian Schmaus
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
package org.jivesoftware.smackx.iot;

import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.iot.provisioning.IoTProvisioningManager;
import org.jxmpp.jid.Jid;

public abstract class IoTManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(IoTManager.class.getName());

    private final IoTProvisioningManager ioTProvisioningManager;

    private boolean allowNonFriends;

    protected IoTManager(XMPPConnection connection) {
        super(connection);

        ioTProvisioningManager = IoTProvisioningManager.getInstanceFor(connection);
    }

    /**
     * Set whether or not non friends should be able to use the services provided by this manager. Those non-friend
     * entities still need to know the full JID for IQ based requests.
     *
     * @param allowNonFriends true to allow everyone to use the services.
     */
    public void setAllowNonFriends(boolean allowNonFriends) {
        this.allowNonFriends = allowNonFriends;
    }

    protected boolean isAllowed(Jid jid) {
        if (allowNonFriends) return true;

        return ioTProvisioningManager.isMyFriend(jid);
    }

    protected abstract class IoTIqRequestHandler extends AbstractIqRequestHandler {

        protected IoTIqRequestHandler(String element, String namespace, Type type, Mode mode) {
            super(element, namespace, type, mode);
        }

        @Override
        public final IQ handleIQRequest(IQ iqRequest) {
            if (!isAllowed(iqRequest.getFrom())) {
                LOGGER.warning("Ignoring IQ request " + iqRequest);
                return null;
            }

            return handleIoTIqRequest(iqRequest);
        }

        protected abstract IQ handleIoTIqRequest(IQ iqRequest);
    }
}
