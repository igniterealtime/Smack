/**
 * Copyright the original author or authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.omemo.util;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.elements.OmemoBundleElement;
import org.jivesoftware.smackx.omemo.elements.OmemoDeviceListElement;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.pubsub.*;
import org.jxmpp.jid.BareJid;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.PEP_NODE_BUNDLE_FROM_DEVICE_ID;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.PEP_NODE_DEVICE_LIST;

/**
 * Really dirty Workaround for the PubSub Node problem...
 *
 * @author Paul Schaub
 */
public class PubSubHelper {

    private static final Logger LOGGER = Logger.getLogger(PubSubHelper.class.getName());

    private final OmemoManager manager;

    public PubSubHelper(OmemoManager manager) {
        this.manager = manager;
    }

    /**
     * Try to get a LeafNode via PubSub
     *
     * @param contact  bareJid of the user that owns the node we want to get
     * @param nodeName the name of the node
     * @return the LeafNode
     * @throws SmackException.NotConnectedException when
     * @throws InterruptedException                 something
     * @throws SmackException.NoResponseException   fails
     */
    public LeafNode getNode(BareJid contact, String nodeName) throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        BareJid owner = (contact == null ? manager.getConnection().getUser().asBareJid() : contact);
        PubSubManager pm = PubSubManager.getInstance(manager.getConnection(), owner);
        try { //The classic way
            return pm.getNode(nodeName);
        }
        //Node does not exist:
        catch (AssertionError e) {
            if (manager.getConnection().getUser().asBareJid().equals(contact)) {
                LOGGER.log(Level.INFO, "It looks like the node does not exist. Create it.");
                try {
                    return pm.createNode(nodeName);
                } catch (XMPPException.XMPPErrorException e1) {
                    LOGGER.log(Level.INFO, "Could not create node the classic way.");
                    e1.printStackTrace();
                    return null;
                }
            } else {
                LOGGER.log(Level.INFO, "It looks like the node does not exist.");
                return null;
            }
        }
        //Prosody workaround
        catch (XMPPException.XMPPErrorException e) {
            LOGGER.log(Level.INFO, "Server " + owner.getDomain() + " does not support some PubSub features needed by Smack. Probably a Prosody Server. Try workaround.");
            try {
                return getNodeWorkaround(pm, nodeName);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException reflectionError) {
                LOGGER.log(Level.INFO, "Could not create a node via reflections: " + reflectionError.getMessage());
                return null;
            }
            //TODO: What if Node does not exist?
        }
    }

    /**
     * This is black magic!
     * Get a {@link LeafNode} from a prosody server.
     * Using conventional methods fails on prosody.
     * See <href>https://prosody.im/issues/issue/805</href> for more information
     *
     * @param pm       PubSubManager
     * @param nodeName name of the node
     * @return the LeafNode
     * @throws IllegalAccessException    When Access is denied
     * @throws InvocationTargetException and
     * @throws InstantiationException    when instantiating the node via reflections fails.
     */
    public LeafNode getNodeWorkaround(PubSubManager pm, String nodeName) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<?> constructor = LeafNode.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        return (LeafNode) constructor.newInstance(pm, nodeName);
    }

    /**
     * Directly fetch the device list of a contact
     *
     * @param contact BareJid of the contact
     * @return The OmemoDeviceListElement of the contact
     * @throws XMPPException.XMPPErrorException     When
     * @throws SmackException.NotConnectedException something
     * @throws InterruptedException                 goes
     * @throws SmackException.NoResponseException   wrong
     */
    public OmemoDeviceListElement fetchDeviceList(BareJid contact) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        LOGGER.log(Level.INFO, "Fetching device list of " + contact + "...");
        LeafNode node = getNode(contact, PEP_NODE_DEVICE_LIST);
        OmemoDeviceListElement list = extractDeviceListFrom(node);
        LOGGER.log(Level.INFO, "Device list of " + contact + " fetched: " + list);
        return list;
    }

    /**
     * Fetch the OmemoBundleElement of the contact
     *
     * @param contact the contacts BareJid
     * @return the OmemoBundleElement of the contact
     * @throws XMPPException.XMPPErrorException     When
     * @throws SmackException.NotConnectedException something
     * @throws InterruptedException                 goes
     * @throws SmackException.NoResponseException   wrong
     */
    public OmemoBundleElement fetchBundle(OmemoDevice contact) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        LOGGER.log(Level.INFO, "Fetching bundle of " + contact + "...");
        LeafNode node = getNode(contact.getJid(), PEP_NODE_BUNDLE_FROM_DEVICE_ID(contact.getDeviceId()));
        if (node != null) {
            OmemoBundleElement bundle = extractBundleFrom(node);
            LOGGER.log(Level.INFO, "Bundle of " + contact + " fetched.");
            return bundle;
        } else {
            return null;
        }
    }

    /**
     * Extract the OmemoBundleElement of a contact from a LeafNode
     *
     * @param node typically a LeafNode containing the OmemoBundles of a contact
     * @return the OmemoBundleElement
     * @throws XMPPException.XMPPErrorException     When
     * @throws SmackException.NotConnectedException something
     * @throws InterruptedException                 goes
     * @throws SmackException.NoResponseException   wrong
     */
    public OmemoBundleElement extractBundleFrom(LeafNode node) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        if (node == null) {
            LOGGER.log(Level.INFO, "Node is null!");
            return null;
        }
        try {
            return (OmemoBundleElement) ((PayloadItem<?>) node.getItems().get(0)).getPayload();
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Extract the OmemoDeviceListElement of a contact from a node containing his OmemoDeviceListElement
     *
     * @param node typically a LeafNode containing the OmemoDeviceListElement of a contact
     * @return the extracted OmemoDeviceListElement.
     * @throws XMPPException.XMPPErrorException     When
     * @throws SmackException.NotConnectedException something
     * @throws InterruptedException                 goes
     * @throws SmackException.NoResponseException   wrong
     */
    public OmemoDeviceListElement extractDeviceListFrom(LeafNode node) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        if (node == null) {
            LOGGER.log(Level.INFO, "Node is null!");
            return null;
        }
        if (node.getItems().size() != 0) {
            return (OmemoDeviceListElement) ((PayloadItem<?>) node.getItems().get(0)).getPayload();
        } else {
            LOGGER.log(Level.INFO, "Node has no items.");
            return new OmemoDeviceListElement();
        }
    }
}
