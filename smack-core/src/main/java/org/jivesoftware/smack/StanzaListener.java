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

package org.jivesoftware.smack;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.packet.Stanza;

/**
 * Provides a mechanism to listen for packets that pass a specified filter.
 * This allows event-style programming -- every time a new stanza is found,
 * the {@link #processStanza(Stanza)} method will be called. This is the
 * opposite approach to the functionality provided by a {@link StanzaCollector}
 * which lets you block while waiting for results.
 * <p>
 * Additionally you are able to intercept Packets that are going to be send and
 * make modifications to them. You can register a PacketListener as interceptor
 * by using {@link XMPPConnection#addStanzaInterceptor(StanzaListener,
 * org.jivesoftware.smack.filter.StanzaFilter)}
 * </p>
 *
 * @see XMPPConnection#addAsyncStanzaListener(StanzaListener, org.jivesoftware.smack.filter.StanzaFilter)
 * @author Matt Tucker
 */
public interface StanzaListener {

    /**
     * Process the next stanza sent to this stanza listener.
     * <p>
     * If this listener is synchronous, then a single thread is responsible for invoking all listeners, so
     * it's very important that implementations of this method not block
     * for any extended period of time.
     * </p>
     *
     * @param packet the stanza to process.
     * @throws NotConnectedException
     * @throws InterruptedException 
     * @throws NotLoggedInException 
     */
    void processStanza(Stanza packet) throws NotConnectedException, InterruptedException, NotLoggedInException;

}
