/**
 *
 * Copyright 2019 Aditya Borikar, 2020 Florian Schmaus.
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
package org.jivesoftware.smackx.usertune;

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Message;

import org.jivesoftware.smackx.pep.PepEventListener;
import org.jivesoftware.smackx.pep.PepManager;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException.NotALeafNodeException;
import org.jivesoftware.smackx.usertune.element.UserTuneElement;

/**
 * Entry point for Smacks API for XEP-0118: User Tune.
 * <br>
 * To publish a UserTune, please use {@link #publishUserTune(UserTuneElement)} method. This will publish the node.
 * <br>
 * To stop publishing a UserTune, please use {@link #clearUserTune()} method. This will send a disabling publish signal.
 * <br>
 * To add a UserTune listener in order to remain updated with other users UserTune, use {@link #addUserTuneListener(PepEventListener)} method.
 * <br>
 * To link a UserTuneElement with {@link Message}, use 'message.addExtension(userTuneElement)'.
 * <br>
 * An example to illustrate is provided inside UserTuneElementTest inside the test package.
 * <br>
 * @see <a href="https://xmpp.org/extensions/xep-0118.html">
 *     XEP-0118: User Tune</a>
 */
public final class UserTuneManager extends Manager {

    public static final String USERTUNE_NODE = "http://jabber.org/protocol/tune";

    private static final Map<XMPPConnection, UserTuneManager> INSTANCES = new WeakHashMap<>();

    private final PepManager pepManager;

    public static synchronized UserTuneManager getInstanceFor(XMPPConnection connection) throws NotLoggedInException {
        UserTuneManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new UserTuneManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    private UserTuneManager(XMPPConnection connection) {
        super(connection);
        pepManager = PepManager.getInstanceFor(connection);
    }

    public void clearUserTune() throws NotLoggedInException, NotALeafNodeException, NoResponseException, NotConnectedException, XMPPErrorException, InterruptedException {
        publishUserTune(UserTuneElement.EMPTY_USER_TUNE);
    }

    public void publishUserTune(UserTuneElement userTuneElement) throws NotLoggedInException, NotALeafNodeException, NoResponseException, NotConnectedException, XMPPErrorException, InterruptedException {
        // TODO: To prevent a large number of updates when a user is skipping through tracks, an implementation SHOULD wait several seconds before publishing new tune information.
        pepManager.publish(USERTUNE_NODE, new PayloadItem<>(userTuneElement));
    }

    public boolean addUserTuneListener(PepEventListener<UserTuneElement> listener) {
        return pepManager.addPepEventListener(USERTUNE_NODE, UserTuneElement.class, listener);
    }

    public boolean removeUserTuneListener(PepEventListener<UserTuneElement> listener) {
        return pepManager.removePepEventListener(listener);
    }
}
