/**
 *
 * Copyright 2020 Aditya Borikar.
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
package org.jivesoftware.smack.websocket.implementations;

import javax.net.ssl.SSLSession;

import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.websocket.rce.WebsocketRemoteConnectionEndpoint;

public abstract class AbstractWebsocket {

    protected enum WebsocketConnectionPhase {
        openFrameSent,
        exchangingTopLevelStreamElements
    }

    protected static String getStreamFromOpenElement(String openElement) {
        String streamElement = openElement.replaceFirst("\\A<open ", "<stream ")
                                          .replace("urn:ietf:params:xml:ns:xmpp-framing", "jabber:client")
                                          .replaceFirst("/>\\s*\\z", ">");
        return streamElement;
    }

    protected static boolean isOpenElement(String text) {
        if (text.startsWith("<open ")) {
            return true;
        }
        return false;
    }

    protected static boolean isCloseElement(String text) {
        if (text.startsWith("<close xmlns='urn:ietf:params:xml:ns:xmpp-framing'/>")) {
            return true;
        }
        return false;
    }

    public abstract void connect(WebsocketRemoteConnectionEndpoint endpoint) throws Throwable;

    public abstract void send(TopLevelStreamElement element);

    public abstract void disconnect(int code, String message);

    public abstract boolean isConnectionSecure();

    public abstract SSLSession getSSLSession();

    public abstract boolean isConnected();
}
