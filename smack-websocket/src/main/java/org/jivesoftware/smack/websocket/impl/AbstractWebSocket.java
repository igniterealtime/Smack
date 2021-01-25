/**
 *
 * Copyright 2020 Aditya Borikar, 2020-2021 Florian Schmaus
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

import javax.net.ssl.SSLSession;

import org.jivesoftware.smack.SmackFuture;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpoint;

public abstract class AbstractWebSocket {

    protected final ModularXmppClientToServerConnectionInternal connectionInternal;

    protected final WebSocketRemoteConnectionEndpoint endpoint;

    protected AbstractWebSocket(WebSocketRemoteConnectionEndpoint endpoint,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        this.endpoint = endpoint;
        this.connectionInternal = connectionInternal;
    }

    public final WebSocketRemoteConnectionEndpoint getEndpoint() {
        return endpoint;
    }

    private String streamOpen;
    private String streamClose;

    protected final void onIncomingWebSocketElement(String element) {
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
        String streamElement = openElement.replaceFirst("\\A<open ", "<stream ")
                                          .replace("urn:ietf:params:xml:ns:xmpp-framing", "jabber:client")
                                          .replaceFirst("/>\\s*\\z", ">");
        return streamElement;
    }

    // TODO: Make this method less fragile, e.g. by parsing a little bit into the element to ensure that this is an
    // <open/> element qualified by the correct namespace.
    static boolean isOpenElement(String text) {
        if (text.startsWith("<open ")) {
            return true;
        }
        return false;
    }

    // TODO: Make this method less fragile, e.g. by parsing a little bit into the element to ensure that this is an
    // <close/> element qualified by the correct namespace. The fragility comes due the fact that the element could,
    // inter alia, be specified as
    // <close:close xmlns:close="urn:ietf:params:xml:ns:xmpp-framing"/>
    static boolean isCloseElement(String text) {
        if (text.startsWith("<close xmlns='urn:ietf:params:xml:ns:xmpp-framing'/>")) {
            return true;
        }
        return false;
    }

    public abstract SmackFuture<AbstractWebSocket, Exception> getFuture();

    public final void send(TopLevelStreamElement element) {
        XmlEnvironment outgoingStreamXmlEnvironment = connectionInternal.getOutgoingStreamXmlEnvironment();
        String elementString = element.toXML(outgoingStreamXmlEnvironment).toString();
        send(elementString);
    }

    protected abstract void send(String element);

    public abstract void disconnect(int code, String message);

    public abstract boolean isConnectionSecure();

    public abstract SSLSession getSSLSession();

    public abstract boolean isConnected();
}
