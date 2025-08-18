/**
 *
 * Copyright 2018 Miguel Hincapie.
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
package org.jivesoftware.smackx.nick;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jivesoftware.smack.AsyncButOrdered;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.nick.filter.NickFilter;
import org.jivesoftware.smackx.nick.packet.Nick;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;


/**
 * Implementation of XEP-0172.
 *
 * @author Miguel Hincapie
 * @see <a href="http://xmpp.org/extensions/xep-0172.html">XEP-0172: User Nickname</a>
 */
public final class NickManager extends Manager {

    private static final Map<XMPPConnection, NickManager> INSTANCES = new WeakHashMap<>();

    private static final StanzaFilter INCOMING_MESSAGE_FILTER = NickFilter.INSTANCE;

    private final Set<NickListener> nickListeners = new HashSet<>();

    private final AsyncButOrdered<Jid> asyncButOrdered = new AsyncButOrdered<>();

    private NickManager(XMPPConnection connection) {
        super(connection);

        connection.addSyncStanzaListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza packet)
                    throws
                    SmackException.NotConnectedException,
                    InterruptedException,
                    SmackException.NotLoggedInException {
                final Message message = (Message) packet;

                Jid from = message.getFrom();
                if (from != null) {
                    asyncButOrdered.performAsyncButOrdered(from, new Runnable() {
                        @Override
                        public void run() {
                            synchronized (nickListeners) {
                                for (NickListener listener : nickListeners) {
                                    listener.newNickMessage(message);
                                }
                            }
                        }
                    });
                }

            }
        }, INCOMING_MESSAGE_FILTER);
    }

    public static synchronized NickManager getInstanceFor(XMPPConnection connection) {
        NickManager nickManager = INSTANCES.get(connection);
        if (nickManager == null) {
            nickManager = new NickManager(connection);
            INSTANCES.put(connection, nickManager);
        }
        return nickManager;
    }

    public synchronized boolean addNickMessageListener(NickListener listener) {
        return nickListeners.add(listener);
    }

    public synchronized boolean removeNickMessageListener(NickListener listener) {
        return nickListeners.remove(listener);
    }

    public void sendNickMessage(EntityBareJid to, String nickname) throws
            SmackException.NotLoggedInException,
            InterruptedException,
            SmackException.NotConnectedException {
        sendStanza(createNickMessage(to, nickname));
    }

    /**
     * Create a Smack's message stanza to update the user's nickName.
     *
     * @param to       the receiver.
     * @param nickName the new nickName.
     * @return instance of Message stanza.
     */
    private static Message createNickMessage(EntityBareJid to, String nickName) {
        Message message = new Message();
        message.setTo(to);
        message.setType(Message.Type.chat);
        message.setStanzaId();
        message.addExtension(new Nick(nickName));
        return message;
    }

    private void sendStanza(Message message)
            throws
            SmackException.NotLoggedInException,
            SmackException.NotConnectedException,
            InterruptedException {
        XMPPConnection connection = getAuthenticatedConnectionOrThrow();
        connection.sendStanza(message);
    }
}
