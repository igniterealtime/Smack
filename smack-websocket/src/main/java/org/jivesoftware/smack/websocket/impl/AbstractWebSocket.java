/**
 *
 * Copyright 2020 Aditya Borikar, 2020-2023 Florian Schmaus
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
package org.jivesoftware.smack.websocket.impl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSession;
import javax.xml.namespace.QName;

import org.jivesoftware.smack.SmackFuture;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.debugger.SmackDebugger;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.websocket.WebSocketException;
import org.jivesoftware.smack.websocket.elements.WebSocketCloseElement;
import org.jivesoftware.smack.websocket.elements.WebSocketOpenElement;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpoint;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

public abstract class AbstractWebSocket {

    protected static final Logger LOGGER = Logger.getLogger(AbstractWebSocket.class.getName());

    protected static final String SEC_WEBSOCKET_PROTOCOL_HEADER_FILED_NAME = "Sec-WebSocket-Protocol";
    protected static final String SEC_WEBSOCKET_PROTOCOL_HEADER_FILED_VALUE_XMPP = "xmpp";

    protected final SmackFuture.InternalSmackFuture<AbstractWebSocket, Exception> future = new SmackFuture.InternalSmackFuture<>();

    protected final ModularXmppClientToServerConnectionInternal connectionInternal;

    protected final WebSocketRemoteConnectionEndpoint endpoint;

    private final SmackWebSocketDebugger debugger;

    protected AbstractWebSocket(WebSocketRemoteConnectionEndpoint endpoint,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        this.endpoint = endpoint;
        this.connectionInternal = connectionInternal;

        final SmackDebugger smackDebugger = connectionInternal.smackDebugger;
        if (smackDebugger != null) {
            debugger = new SmackWebSocketDebugger(smackDebugger);
        } else {
            debugger = null;
        }
    }

    public final WebSocketRemoteConnectionEndpoint getEndpoint() {
        return endpoint;
    }

    private String streamOpen;
    private String streamClose;

    protected final void onIncomingWebSocketElement(String element) {
        if (debugger != null) {
            debugger.incoming(element);
        }

        // TODO: Once smack-websocket-java15 is there, we have to re-evaluate if the async operation here is still
        // required, or if it should only be performed if OkHTTP is used.
        if (isOpenElement(element)) {
            // Transform the XMPP WebSocket <open/> element to a RFC 6120 <stream> open tag.
            streamOpen = getStreamFromOpenElement(element);
            streamClose = connectionInternal.onStreamOpen(streamOpen);
            return;
        }

        if (isCloseElement(element)) {
            connectionInternal.onStreamClosed();
            return;
        }

        connectionInternal.withSmackDebugger(debugger -> debugger.onIncomingElementCompleted());

        // TODO: Do we need to wrap the element again in the stream open to get the
        // correct XML scoping (just like the modular TCP connection does)? It appears
        // that this not really required, as onStreamOpen() will set the incomingStreamEnvironment, which is used for
        // parsing.
        String wrappedCompleteElement = streamOpen + element + streamClose;
        connectionInternal.parseAndProcessElement(wrappedCompleteElement);
    }

    static String getStreamFromOpenElement(String openElement) {
        String streamElement = openElement.replaceFirst("\\A<open ", "<stream:stream ")
                                          .replace("urn:ietf:params:xml:ns:xmpp-framing", "jabber:client")
                                          .replaceFirst("/>\\s*\\z", " xmlns:stream='http://etherx.jabber.org/streams'>")
                                          .replaceFirst("></open>\\s*\\z", " xmlns:stream='http://etherx.jabber.org/streams'>");

        return streamElement;
    }

    static boolean isOpenElement(String text) {
        XmlPullParser parser;
        try {
            parser = PacketParserUtils.getParserFor(text);
            QName qname = parser.getQName();
            return qname.equals(WebSocketOpenElement.QNAME);
        } catch (XmlPullParserException | IOException e) {
            LOGGER.log(Level.WARNING, "Could not inspect \"" + text + "\" for open element", e);
            return false;
        }
    }

    static boolean isCloseElement(String text) {
        XmlPullParser parser;
        try {
            parser = PacketParserUtils.getParserFor(text);
            QName qname = parser.getQName();
            return qname.equals(WebSocketCloseElement.QNAME);
        } catch (XmlPullParserException | IOException e) {
            LOGGER.log(Level.WARNING, "Could not inspect \"" + text + "\" for close element", e);
            return false;
        }
    }

    protected void onWebSocketFailure(Throwable throwable) {
        WebSocketException websocketException = new WebSocketException(throwable);

        // If we are already connected, then we need to notify the connection that it got tear down. Otherwise we
        // need to notify the thread calling connect() that the connection failed.
        if (future.wasSuccessful()) {
            connectionInternal.notifyConnectionError(websocketException);
        } else {
            future.setException(websocketException);
        }
    }

    public final SmackFuture<AbstractWebSocket, Exception> getFuture() {
        return future;
    }

    public final void send(TopLevelStreamElement element) {
        XmlEnvironment outgoingStreamXmlEnvironment = connectionInternal.getOutgoingStreamXmlEnvironment();
        String elementString = element.toXML(outgoingStreamXmlEnvironment).toString();

        // TODO: We could make use of Java 11's WebSocket (is)last feature when sending
        if (debugger != null) {
            debugger.outgoing(elementString);
        }

        send(elementString);
    }

    protected abstract void send(String element);

    public abstract void disconnect(int code, String message);

    public boolean isConnectionSecure() {
        return endpoint.isSecureEndpoint();
    }

    public abstract SSLSession getSSLSession();

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + connectionInternal.connection + "]";
    }
}
