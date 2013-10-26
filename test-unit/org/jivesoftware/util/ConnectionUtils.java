/**
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.util;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * A collection of utility methods to create mocked XMPP connections.
 * 
 * @author Henning Staib
 */
public class ConnectionUtils {

    /**
     * Creates a mocked XMPP connection that stores every packet that is send over this
     * connection in the given protocol instance and returns the predefined answer packets
     * form the protocol instance.
     * <p>
     * This mocked connection can used to collect packets that require a reply using a
     * PacketCollector.
     * 
     * <pre>
     * <code>
     *   PacketCollector collector = connection.createPacketCollector(new PacketFilter());
     *   connection.sendPacket(packet);
     *   Packet reply = collector.nextResult();
     * </code>
     * </pre>
     * 
     * @param protocol protocol helper containing answer packets
     * @param initiatorJID the user associated to the XMPP connection
     * @param xmppServer the XMPP server associated to the XMPP connection
     * @return a mocked XMPP connection
     */
    public static Connection createMockedConnection(final Protocol protocol,
                    String initiatorJID, String xmppServer) {

        // mock XMPP connection
        Connection connection = mock(Connection.class);
        when(connection.getUser()).thenReturn(initiatorJID);
        when(connection.getServiceName()).thenReturn(xmppServer);

        // mock packet collector
        PacketCollector collector = mock(PacketCollector.class);
        when(connection.createPacketCollector(isA(PacketFilter.class))).thenReturn(
                        collector);
        Answer<Object> addIncoming = new Answer<Object>() {

            public Object answer(InvocationOnMock invocation) throws Throwable {
                protocol.getRequests().add((Packet) invocation.getArguments()[0]);
                return null;
            }
        };

        // mock send method
        doAnswer(addIncoming).when(connection).sendPacket(isA(Packet.class));
        Answer<Packet> answer = new Answer<Packet>() {

            public Packet answer(InvocationOnMock invocation) throws Throwable {
                return protocol.getResponses().poll();
            }
        };

        // mock nextResult method
        when(collector.nextResult(anyInt())).thenAnswer(answer);
        when(collector.nextResult()).thenAnswer(answer);

        // initialize service discovery manager for this connection
        ServiceDiscoveryManager.getInstanceFor(connection);

        return connection;
    }

}
