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
package org.jivesoftware.smackx.iot.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.iot.IoTManager;
import org.jivesoftware.smackx.iot.Thing;
import org.jivesoftware.smackx.iot.data.element.IoTDataField;
import org.jivesoftware.smackx.iot.data.element.IoTDataReadOutAccepted;
import org.jivesoftware.smackx.iot.data.element.IoTDataRequest;
import org.jivesoftware.smackx.iot.data.element.IoTFieldsExtension;
import org.jivesoftware.smackx.iot.data.filter.IoTFieldsExtensionFilter;
import org.jivesoftware.smackx.iot.element.NodeInfo;
import org.jxmpp.jid.EntityFullJid;

/**
 * A manager for XEP-0323: Internet of Things - Sensor Data.
 * 
 * @author Florian Schmaus {@literal <flo@geekplace.eu>}
 * @see <a href="http://xmpp.org/extensions/xep-0323.html">XEP-0323: Internet of Things - Sensor Data</a>
 */
public final class IoTDataManager extends IoTManager {

    private static final Logger LOGGER = Logger.getLogger(IoTDataManager.class.getName());

    private static final Map<XMPPConnection, IoTDataManager> INSTANCES = new WeakHashMap<>();

    // Ensure a IoTDataManager exists for every connection.
    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                if (!isAutoEnableActive()) return;
                getInstanceFor(connection);
            }
        });
    }

    /**
     * Get the manger instance responsible for the given connection.
     *
     * @param connection the XMPP connection.
     * @return a manager instance.
     */
    public static synchronized IoTDataManager getInstanceFor(XMPPConnection connection) {
        IoTDataManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new IoTDataManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    private final AtomicInteger nextSeqNr = new AtomicInteger();

    private final Map<NodeInfo, Thing> things = new ConcurrentHashMap<>();

    private IoTDataManager(XMPPConnection connection) {
        super(connection);
        connection.registerIQRequestHandler(new IoTIqRequestHandler(IoTDataRequest.ELEMENT,
                        IoTDataRequest.NAMESPACE, IQ.Type.get, Mode.async) {
            @Override
            public IQ handleIoTIqRequest(IQ iqRequest) {
                final IoTDataRequest dataRequest = (IoTDataRequest) iqRequest;

                if (!dataRequest.isMomentary()) {
                    // TODO return error IQ that non momentary requests are not implemented yet.
                    return null;
                }

                // TODO Add support for multiple things(/NodeInfos).
                final Thing thing = things.get(NodeInfo.EMPTY);
                if (thing == null) {
                    // TODO return error if not at least one thing registered.
                    return null;
                }

                ThingMomentaryReadOutRequest readOutRequest = thing.getMomentaryReadOutRequestHandler();
                if (readOutRequest == null) {
                    // TODO Thing does not provide momentary read-out
                    return null;
                }

                // Callback hell begins here. :) XEP-0323 decouples the read-out results from the IQ result. I'm not
                // sure if I would have made the same design decision but the reasons where likely being able to get a
                // fast read-out acknowledgement back to the requester even with sensors that take "a long time" to
                // read-out their values. I had designed that as special case and made the "results in IQ response" the
                // normal case.
                readOutRequest.momentaryReadOutRequest(new ThingMomentaryReadOutResult() {
                    @Override
                    public void momentaryReadOut(List<? extends IoTDataField> results) {
                        IoTFieldsExtension iotFieldsExtension = IoTFieldsExtension.buildFor(dataRequest.getSequenceNr(), true, thing.getNodeInfo(), results);
                        Message message = new Message(dataRequest.getFrom());
                        message.addExtension(iotFieldsExtension);
                        try {
                            connection().sendStanza(message);
                        }
                        catch (NotConnectedException | InterruptedException e) {
                            LOGGER.log(Level.SEVERE, "Could not send read-out response " + message, e);
                        }
                    }
                });

                return new IoTDataReadOutAccepted(dataRequest);
            }
        });
    }

    /**
     * Install a thing in the manager. Activates data read out functionality (if provided by the
     * thing).
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

    /**
     * Try to read out a things momentary values.
     *
     * @param jid the full JID of the thing to read data from.
     * @return a list with the read out data.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public List<IoTFieldsExtension> requestMomentaryValuesReadOut(EntityFullJid jid)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final XMPPConnection connection = connection();
        final int seqNr = nextSeqNr.incrementAndGet();
        IoTDataRequest iotDataRequest = new IoTDataRequest(seqNr, true);
        iotDataRequest.setTo(jid);

        StanzaFilter doneFilter = new IoTFieldsExtensionFilter(seqNr, true);
        StanzaFilter dataFilter = new IoTFieldsExtensionFilter(seqNr, false);

        // Setup the IoTFieldsExtension message collectors before sending the IQ to avoid a data race.
        StanzaCollector doneCollector = connection.createStanzaCollector(doneFilter);

        StanzaCollector.Configuration dataCollectorConfiguration = StanzaCollector.newConfiguration().setStanzaFilter(
                        dataFilter).setCollectorToReset(doneCollector);
        StanzaCollector dataCollector = connection.createStanzaCollector(dataCollectorConfiguration);

        try {
            connection.createStanzaCollectorAndSend(iotDataRequest).nextResultOrThrow();
            // Wait until a message with an IoTFieldsExtension and the done flag comes in.
            doneCollector.nextResult();
        }
        finally {
            // Ensure that the two collectors are canceled in any case.
            dataCollector.cancel();
            doneCollector.cancel();
        }

        int collectedCount = dataCollector.getCollectedCount();
        List<IoTFieldsExtension> res = new ArrayList<>(collectedCount);
        for (int i = 0; i < collectedCount; i++) {
            Message message = dataCollector.pollResult();
            IoTFieldsExtension iotFieldsExtension = IoTFieldsExtension.from(message);
            res.add(iotFieldsExtension);
        }

        return res;
    }
}
