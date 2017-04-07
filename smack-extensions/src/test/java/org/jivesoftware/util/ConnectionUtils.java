/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.util;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * A collection of utility methods to create mocked XMPP connections.
 * 
 * @author Henning Staib
 */
public class ConnectionUtils {

    /**
     * Creates a mocked XMPP connection that stores every stanza(/packet) that is send over this
     * connection in the given protocol instance and returns the predefined answer packets
     * form the protocol instance.
     * <p>
     * This mocked connection can used to collect packets that require a reply using a
     * StanzaCollector.
     * 
     * <pre>
     * <code>
     *   StanzaCollector collector = connection.createStanzaCollector(new PacketFilter());
     *   connection.sendStanza(packet);
     *   Stanza(/Packet) reply = collector.nextResult();
     * </code>
     * </pre>
     * 
     * @param protocol protocol helper containing answer packets
     * @param initiatorJID the user associated to the XMPP connection
     * @param xmppServer the XMPP server associated to the XMPP connection
     * @return a mocked XMPP connection
     * @throws SmackException 
     * @throws XMPPErrorException 
     * @throws InterruptedException 
     */
    public static XMPPConnection createMockedConnection(final Protocol protocol,
                    EntityFullJid initiatorJID, DomainBareJid xmppServer) throws SmackException, XMPPErrorException, InterruptedException {

        // mock XMPP connection
        XMPPConnection connection = mock(XMPPConnection.class);
        when(connection.getUser()).thenReturn(initiatorJID);
        when(connection.getXMPPServiceDomain()).thenReturn(xmppServer);

        // mock packet collector
        final StanzaCollector collector = mock(StanzaCollector.class);
        when(connection.createStanzaCollector(isA(StanzaFilter.class))).thenReturn(
                        collector);
        Answer<StanzaCollector> collectorAndSend = new Answer<StanzaCollector>() {
            @Override
            public StanzaCollector answer(InvocationOnMock invocation) throws Throwable {
                Stanza packet = (Stanza) invocation.getArguments()[0];
                protocol.getRequests().add(packet);
                return collector;
            }

        };
        when(connection.createStanzaCollectorAndSend(isA(IQ.class))).thenAnswer(collectorAndSend);

        // mock send method
        Answer<Object> addIncoming = new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                protocol.getRequests().add((Stanza) invocation.getArguments()[0]);
                return null;
            }
        };
        doAnswer(addIncoming).when(connection).sendStanza(isA(Stanza.class));

        // mock receive methods
        Answer<Stanza> answer = new Answer<Stanza>() {
            @Override
            public Stanza answer(InvocationOnMock invocation) throws Throwable {
                return protocol.getResponses().poll();
            }
        };
        when(collector.nextResult(anyInt())).thenAnswer(answer);
        when(collector.nextResult()).thenAnswer(answer);
        Answer<Stanza> answerOrThrow = new Answer<Stanza>() {
            @Override
            public Stanza answer(InvocationOnMock invocation) throws Throwable {
                Stanza packet = protocol.getResponses().poll();
                if (packet == null) return packet;
                XMPPErrorException.ifHasErrorThenThrow(packet);
                return packet;
            }
        };
        when(collector.nextResultOrThrow()).thenAnswer(answerOrThrow);
        when(collector.nextResultOrThrow(anyLong())).thenAnswer(answerOrThrow);

        // initialize service discovery manager for this connection
        ServiceDiscoveryManager.getInstanceFor(connection);

        return connection;
    }

}
