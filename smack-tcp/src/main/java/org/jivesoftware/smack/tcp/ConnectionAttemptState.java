/**
 *
 * Copyright 2019-2020 Florian Schmaus
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
package org.jivesoftware.smack.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.SmackException.ConnectionException;
import org.jivesoftware.smack.SmackException.EndpointConnectionException;
import org.jivesoftware.smack.SynchronizationPoint;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.tcp.XmppTcpTransportModule.EstablishingTcpConnectionState;
import org.jivesoftware.smack.tcp.rce.Rfc6120TcpRemoteConnectionEndpoint;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpoint;
import org.jivesoftware.smack.util.rce.RemoteConnectionException;

public final class ConnectionAttemptState {

    private final ModularXmppClientToServerConnectionInternal connectionInternal;

    private final XmppTcpTransportModule.XmppTcpNioTransport.DiscoveredTcpEndpoints discoveredEndpoints;

    private final EstablishingTcpConnectionState establishingTcpConnectionState;

    // TODO: Check if we can re-use the socket channel in case some InetSocketAddress fail to connect to.
    final SocketChannel socketChannel;

    final List<RemoteConnectionException<?>> connectionExceptions;
    final SynchronizationPoint<ConnectionException> tcpConnectionEstablishedSyncPoint;

    final Iterator<Rfc6120TcpRemoteConnectionEndpoint> connectionEndpointIterator;
    /** The current connection endpoint we are trying */
    Rfc6120TcpRemoteConnectionEndpoint connectionEndpoint;
    Iterator<? extends InetAddress> inetAddressIterator;

    ConnectionAttemptState(ModularXmppClientToServerConnectionInternal connectionInternal,
                    XmppTcpTransportModule.XmppTcpNioTransport.DiscoveredTcpEndpoints discoveredEndpoints,
                    EstablishingTcpConnectionState establishingTcpConnectionState) throws IOException {
        this.connectionInternal = connectionInternal;
        this.discoveredEndpoints = discoveredEndpoints;
        this.establishingTcpConnectionState = establishingTcpConnectionState;

        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        connectionEndpointIterator = discoveredEndpoints.result.discoveredRemoteConnectionEndpoints.iterator();
        connectionEndpoint = connectionEndpointIterator.next();
        connectionExceptions = new ArrayList<>(discoveredEndpoints.result.discoveredRemoteConnectionEndpoints.size());

        tcpConnectionEstablishedSyncPoint = new SynchronizationPoint<>(connectionInternal.connection,
                "TCP connection establishment");
    }

    void establishTcpConnection() {
        RemoteConnectionEndpoint.InetSocketAddressCoupling<Rfc6120TcpRemoteConnectionEndpoint> address = nextAddress();
        establishTcpConnection(address);
    }

    private void establishTcpConnection(
                    RemoteConnectionEndpoint.InetSocketAddressCoupling<Rfc6120TcpRemoteConnectionEndpoint> address) {
        TcpHostEvent.ConnectingToHostEvent connectingToHostEvent = new TcpHostEvent.ConnectingToHostEvent(
                        establishingTcpConnectionState, address);
        connectionInternal.invokeConnectionStateMachineListener(connectingToHostEvent);

        final boolean connected;
        final InetSocketAddress inetSocketAddress = address.getInetSocketAddress();
        try {
            connected = socketChannel.connect(inetSocketAddress);
        } catch (IOException e) {
            onIOExceptionWhenEstablishingTcpConnection(e, address);
            return;
        }

        if (connected) {
            TcpHostEvent.ConnectedToHostEvent connectedToHostEvent = new TcpHostEvent.ConnectedToHostEvent(
                            establishingTcpConnectionState, address, true);
            connectionInternal.invokeConnectionStateMachineListener(connectedToHostEvent);

            tcpConnectionEstablishedSyncPoint.reportSuccess();
            return;
        }

        try {
            connectionInternal.registerWithSelector(socketChannel, SelectionKey.OP_CONNECT,
                    (selectedChannel, selectedSelectionKey) -> {
                        SocketChannel selectedSocketChannel = (SocketChannel) selectedChannel;

                        boolean finishConnected;
                        try {
                            finishConnected = selectedSocketChannel.finishConnect();
                        } catch (IOException e) {
                            Async.go(() -> onIOExceptionWhenEstablishingTcpConnection(e, address));
                            return;
                        }

                        if (!finishConnected) {
                            Async.go(() -> onIOExceptionWhenEstablishingTcpConnection(new IOException("finishConnect() failed"), address));
                            return;
                        }

                        TcpHostEvent.ConnectedToHostEvent connectedToHostEvent = new TcpHostEvent.ConnectedToHostEvent(
                                        establishingTcpConnectionState, address, false);
                        connectionInternal.invokeConnectionStateMachineListener(connectedToHostEvent);

                        // Do not set 'state' here, since this is processed by a reactor thread, which doesn't hold
                        // the objects lock.
                        tcpConnectionEstablishedSyncPoint.reportSuccess();
                    });
        } catch (ClosedChannelException e) {
            onIOExceptionWhenEstablishingTcpConnection(e, address);
        }
    }

    private void onIOExceptionWhenEstablishingTcpConnection(IOException exception,
                    RemoteConnectionEndpoint.InetSocketAddressCoupling<Rfc6120TcpRemoteConnectionEndpoint> failedAddress) {
        RemoteConnectionEndpoint.InetSocketAddressCoupling<Rfc6120TcpRemoteConnectionEndpoint> nextInetSocketAddress = nextAddress();
        if (nextInetSocketAddress == null) {
            EndpointConnectionException connectionException = EndpointConnectionException.from(
                            discoveredEndpoints.result.lookupFailures, connectionExceptions);
            tcpConnectionEstablishedSyncPoint.reportFailure(connectionException);
            return;
        }

        tcpConnectionEstablishedSyncPoint.resetTimeout();

        RemoteConnectionException<Rfc6120TcpRemoteConnectionEndpoint> rce = new RemoteConnectionException<>(
                        failedAddress, exception);
        connectionExceptions.add(rce);

        TcpHostEvent.ConnectionToHostFailedEvent connectionToHostFailedEvent = new TcpHostEvent.ConnectionToHostFailedEvent(
                        establishingTcpConnectionState, nextInetSocketAddress, exception);
        connectionInternal.invokeConnectionStateMachineListener(connectionToHostFailedEvent);

        establishTcpConnection(nextInetSocketAddress);
    }

    private RemoteConnectionEndpoint.InetSocketAddressCoupling<Rfc6120TcpRemoteConnectionEndpoint> nextAddress() {
        if (inetAddressIterator == null || !inetAddressIterator.hasNext()) {
            if (!connectionEndpointIterator.hasNext()) {
                return null;
            }

            connectionEndpoint = connectionEndpointIterator.next();
            inetAddressIterator = connectionEndpoint.getInetAddresses().iterator();
            // Every valid connection addresspoint must have a non-empty collection of inet addresses.
            assert inetAddressIterator.hasNext();
        }

        InetAddress inetAddress = inetAddressIterator.next();

        return new RemoteConnectionEndpoint.InetSocketAddressCoupling<>(connectionEndpoint, inetAddress);
    }
}
