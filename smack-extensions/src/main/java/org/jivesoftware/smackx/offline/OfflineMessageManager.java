/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.offline;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.offline.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.offline.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.xdata.Form;

/**
 * The OfflineMessageManager helps manage offline messages even before the user has sent an
 * available presence. When a user asks for his offline messages before sending an available
 * presence then the server will not send a flood with all the offline messages when the user
 * becomes online. The server will not send a flood with all the offline messages to the session
 * that made the offline messages request or to any other session used by the user that becomes
 * online.<p>
 *
 * Once the session that made the offline messages request has been closed and the user becomes
 * offline in all the resources then the server will resume storing the messages offline and will
 * send all the offline messages to the user when he becomes online. Therefore, the server will
 * flood the user when he becomes online unless the user uses this class to manage his offline
 * messages.
 *
 * @author Gaston Dombiak
 */
public class OfflineMessageManager {

    private static final Logger LOGGER = Logger.getLogger(OfflineMessageManager.class.getName());

    private static final String namespace = "http://jabber.org/protocol/offline";

    private final XMPPConnection connection;

    private static final StanzaFilter PACKET_FILTER = new AndFilter(new StanzaExtensionFilter(
                    new OfflineMessageInfo()), StanzaTypeFilter.MESSAGE);

    public OfflineMessageManager(XMPPConnection connection) {
        this.connection = connection;
    }

    /**
     * Returns true if the server supports Flexible Offline Message Retrieval. When the server
     * supports Flexible Offline Message Retrieval it is possible to get the header of the offline
     * messages, get specific messages, delete specific messages, etc.
     *
     * @return a boolean indicating if the server supports Flexible Offline Message Retrieval.
     * @throws XMPPErrorException If the user is not allowed to make this request.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public boolean supportsFlexibleRetrieval() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection).serverSupportsFeature(namespace);
    }

    /**
     * Returns the number of offline messages for the user of the connection.
     *
     * @return the number of offline messages for the user of the connection.
     * @throws XMPPErrorException If the user is not allowed to make this request or the server does
     *                       not support offline message retrieval.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public int getMessageCount() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        DiscoverInfo info = ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(null,
                namespace);
        Form extendedInfo = Form.getFormFrom(info);
        if (extendedInfo != null) {
            String value = extendedInfo.getField("number_of_messages").getFirstValue();
            return Integer.parseInt(value);
        }
        return 0;
    }

    /**
     * Returns a List of <code>OfflineMessageHeader</code> that keep information about the
     * offline message. The OfflineMessageHeader includes a stamp that could be used to retrieve
     * the complete message or delete the specific message.
     *
     * @return a List of <code>OfflineMessageHeader</code> that keep information about the offline
     *         message.
     * @throws XMPPErrorException If the user is not allowed to make this request or the server does
     *                       not support offline message retrieval.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public List<OfflineMessageHeader> getHeaders() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        List<OfflineMessageHeader> answer = new ArrayList<>();
        DiscoverItems items = ServiceDiscoveryManager.getInstanceFor(connection).discoverItems(
                null, namespace);
        for (DiscoverItems.Item item : items.getItems()) {
            answer.add(new OfflineMessageHeader(item));
        }
        return answer;
    }

    /**
     * Returns a List of the offline <code>Messages</code> whose stamp matches the specified
     * request. The request will include the list of stamps that uniquely identifies
     * the offline messages to retrieve. The returned offline messages will not be deleted
     * from the server. Use {@link #deleteMessages(java.util.List)} to delete the messages.
     *
     * @param nodes the list of stamps that uniquely identifies offline message.
     * @return a List with the offline <code>Messages</code> that were received as part of
     *         this request.
     * @throws XMPPErrorException If the user is not allowed to make this request or the server does
     *                       not support offline message retrieval.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public List<Message> getMessages(final List<String> nodes) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        List<Message> messages = new ArrayList<>(nodes.size());
        OfflineMessageRequest request = new OfflineMessageRequest();
        for (String node : nodes) {
            OfflineMessageRequest.Item item = new OfflineMessageRequest.Item(node);
            item.setAction("view");
            request.addItem(item);
        }
        // Filter offline messages that were requested by this request
        StanzaFilter messageFilter = new AndFilter(PACKET_FILTER, new StanzaFilter() {
            @Override
            public boolean accept(Stanza packet) {
                OfflineMessageInfo info = packet.getExtension(OfflineMessageInfo.class);
                return nodes.contains(info.getNode());
            }
        });
        int pendingNodes = nodes.size();
        try (StanzaCollector messageCollector = connection.createStanzaCollector(messageFilter)) {
            connection.createStanzaCollectorAndSend(request).nextResultOrThrow();
            // Collect the received offline messages
            Message message;
            do {
                message = messageCollector.nextResult();
                if (message != null) {
                    messages.add(message);
                    pendingNodes--;
                } else if (message == null && pendingNodes > 0) {
                    LOGGER.log(Level.WARNING,
                                    "Did not receive all expected offline messages. " + pendingNodes + " are missing.");
                }
            } while (message != null && pendingNodes > 0);
        }
        return messages;
    }

    /**
     * Returns a List of Messages with all the offline <code>Messages</code> of the user. The returned offline
     * messages will not be deleted from the server. Use {@link #deleteMessages(java.util.List)}
     * to delete the messages.
     *
     * @return a List with all the offline <code>Messages</code> of the user.
     * @throws XMPPErrorException If the user is not allowed to make this request or the server does
     *                       not support offline message retrieval.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public List<Message> getMessages() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        OfflineMessageRequest request = new OfflineMessageRequest();
        request.setFetch(true);

        StanzaCollector resultCollector = connection.createStanzaCollectorAndSend(request);
        StanzaCollector.Configuration messageCollectorConfiguration = StanzaCollector.newConfiguration().setStanzaFilter(PACKET_FILTER).setCollectorToReset(resultCollector);

        List<Message> messages;
        try (StanzaCollector messageCollector = connection.createStanzaCollector(messageCollectorConfiguration)) {
            resultCollector.nextResultOrThrow();
            // Be extra safe, cancel the message collector right here so that it does not collector
            // other messages that eventually match (although I've no idea how this could happen in
            // case of XEP-13).
            messageCollector.cancel();
            messages = new ArrayList<>(messageCollector.getCollectedCount());
            Message message;
            while ((message = messageCollector.pollResult()) != null) {
                messages.add(message);
            }
        }
        return messages;
    }

    /**
     * Deletes the specified list of offline messages. The request will include the list of
     * stamps that uniquely identifies the offline messages to delete.
     *
     * @param nodes the list of stamps that uniquely identifies offline message.
     * @throws XMPPErrorException If the user is not allowed to make this request or the server does
     *                       not support offline message retrieval.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void deleteMessages(List<String> nodes) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        OfflineMessageRequest request = new OfflineMessageRequest();
        request.setType(IQ.Type.set);
        for (String node : nodes) {
            OfflineMessageRequest.Item item = new OfflineMessageRequest.Item(node);
            item.setAction("remove");
            request.addItem(item);
        }
        connection.createStanzaCollectorAndSend(request).nextResultOrThrow();
    }

    /**
     * Deletes all offline messages of the user.
     *
     * @throws XMPPErrorException If the user is not allowed to make this request or the server does
     *                       not support offline message retrieval.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void deleteMessages() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        OfflineMessageRequest request = new OfflineMessageRequest();
        request.setType(IQ.Type.set);
        request.setPurge(true);
        connection.createStanzaCollectorAndSend(request).nextResultOrThrow();
    }
}
