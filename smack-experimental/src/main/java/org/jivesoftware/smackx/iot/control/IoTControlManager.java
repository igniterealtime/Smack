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
package org.jivesoftware.smackx.iot.control;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;

import org.jivesoftware.smackx.iot.IoTManager;
import org.jivesoftware.smackx.iot.Thing;
import org.jivesoftware.smackx.iot.control.element.IoTSetRequest;
import org.jivesoftware.smackx.iot.control.element.IoTSetResponse;
import org.jivesoftware.smackx.iot.control.element.SetData;
import org.jivesoftware.smackx.iot.element.NodeInfo;

import org.jxmpp.jid.FullJid;

/**
 * A manger for XEP-0325: Internet of Things - Control.
 *
 * @author Florian Schmaus {@literal <flo@geekplace.eu>}
 * @see <a href="http://xmpp.org/extensions/xep-0325.html">XEP-0323: Internet of Things - Control</a>
 */
public final class IoTControlManager extends IoTManager {

    private static final Map<XMPPConnection, IoTControlManager> INSTANCES = new WeakHashMap<>();

    /**
     * Get the manger instance responsible for the given connection.
     *
     * @param connection the XMPP connection.
     * @return a manager instance.
     */
    public static synchronized IoTControlManager getInstanceFor(XMPPConnection connection) {
        IoTControlManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new IoTControlManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    private final Map<NodeInfo, Thing> things = new ConcurrentHashMap<>();

    private IoTControlManager(XMPPConnection connection) {
        super(connection);

        connection.registerIQRequestHandler(new IoTIqRequestHandler(IoTSetRequest.ELEMENT, IoTSetRequest.NAMESPACE, IQ.Type.set, Mode.async) {
            @Override
            public IQ handleIoTIqRequest(IQ iqRequest) {
                // TODO Lookup thing and provide data.
                IoTSetRequest iotSetRequest = (IoTSetRequest) iqRequest;

                // TODO Add support for multiple things(/NodeInfos).
                final Thing thing = things.get(NodeInfo.EMPTY);
                if (thing == null) {
                    // TODO return error if not at least one thing registered.
                    return null;
                }

                ThingControlRequest controlRequest = thing.getControlRequestHandler();
                if (controlRequest == null) {
                    // TODO return error if no request handler for things.
                    return null;
                }

                try {
                    controlRequest.processRequest(iotSetRequest.getFrom(), iotSetRequest.getSetData());
                } catch (XMPPErrorException e) {
                    return IQ.createErrorResponse(iotSetRequest, e.getStanzaError());
                }

                return new IoTSetResponse(iotSetRequest);
            }
        });
    }

    /**
     * Control a thing by sending a collection of {@link SetData} instructions.
     *
     * @param jid TODO javadoc me please
     * @param data TODO javadoc me please
     * @return a IoTSetResponse
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @see #setUsingIq(FullJid, Collection)
     */
    public IoTSetResponse setUsingIq(FullJid jid, SetData data) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return setUsingIq(jid, Collections.singleton(data));
    }

    /**
     * Control a thing by sending a collection of {@link SetData} instructions.
     *
     * @param jid the thing to control.
     * @param data a collection of {@link SetData} instructions.
     * @return the {@link IoTSetResponse} if successful.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public IoTSetResponse setUsingIq(FullJid jid, Collection<? extends SetData> data) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        IoTSetRequest request = new IoTSetRequest(data);
        request.setTo(jid);
        IoTSetResponse response = connection().sendIqRequestAndWaitForResponse(request);
        return response;
    }

    /**
     * Install a thing in the manager. Activates control functionality (if provided by the thing).
     *
     * @param thing the thing to install.
     */
    public void installThing(Thing thing) {
        things.put(thing.getNodeInfo(), thing);
    }

    public Thing uninstallThing(Thing thing) {
        return uninstallThing(thing.getNodeInfo());
    }

    public Thing uninstallThing(NodeInfo nodeInfo) {
        return things.remove(nodeInfo);
    }
}
