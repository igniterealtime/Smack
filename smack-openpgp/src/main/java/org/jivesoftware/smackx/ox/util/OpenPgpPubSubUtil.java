/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.StanzaError;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.ox.OpenPgpManager;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.pep.PepManager;
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.BareJid;
import org.pgpainless.key.OpenPgpV4Fingerprint;

public class OpenPgpPubSubUtil {

    private static final Logger LOGGER = Logger.getLogger(OpenPgpPubSubUtil.class.getName());

    /**
     * Name of the OX metadata node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#announcing-pubkey-list">XEP-0373 §4.2</a>
     */
    public static final String PEP_NODE_PUBLIC_KEYS = "urn:xmpp:openpgp:0:public-keys";

    /**
     * Name of the OX secret key node.
     */
    public static final String PEP_NODE_SECRET_KEY = "urn:xmpp:openpgp:0:secret-key";

    /**
     * Feature to be announced using the {@link ServiceDiscoveryManager} to subscribe to the OX metadata node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#pubsub-notifications">XEP-0373 §4.4</a>
     */
    public static final String PEP_NODE_PUBLIC_KEYS_NOTIFY = PEP_NODE_PUBLIC_KEYS + "+notify";

    /**
     * Name of the OX public key node, which contains the key with id {@code id}.
     *
     * @param id upper case hex encoded OpenPGP v4 fingerprint of the key.
     * @return PEP node name.
     */
    public static String PEP_NODE_PUBLIC_KEY(OpenPgpV4Fingerprint id) {
        return PEP_NODE_PUBLIC_KEYS + ":" + id;
    }

    /**
     * Query the access model of {@code node}. If it is different from {@code accessModel}, change the access model
     * of the node to {@code accessModel}.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0060.html#accessmodels">XEP-0060 §4.5 - Node Access Models</a>
     *
     * @param node {@link LeafNode} whose PubSub access model we want to change
     * @param accessModel new access model.
     *
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws SmackException.NotConnectedException if we are not connected.
     * @throws InterruptedException if the thread is interrupted.
     * @throws SmackException.NoResponseException if the server doesn't respond.
     */
    public static void changeAccessModelIfNecessary(LeafNode node, AccessModel accessModel)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        ConfigureForm current = node.getNodeConfiguration();
        if (current.getAccessModel() != accessModel) {
            ConfigureForm updateConfig = new ConfigureForm(DataForm.Type.submit);
            updateConfig.setAccessModel(accessModel);
            node.sendConfigurationForm(updateConfig);
        }
    }

    /**
     * Publish the users OpenPGP public key to the public key node if necessary.
     * Also announce the key to other users by updating the metadata node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#annoucning-pubkey">XEP-0373 §4.1</a>
     *
     * @param pepManager The PEP manager.
     * @param pubkeyElement {@link PubkeyElement} containing the public key
     * @param fingerprint fingerprint of the public key
     *
     * @throws InterruptedException if the thread gets interrupted.
     * @throws PubSubException.NotALeafNodeException if either the metadata node or the public key node is not a
     *                                               {@link LeafNode}.
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws SmackException.NotConnectedException if we are not connected.
     * @throws SmackException.NoResponseException if the server doesn't respond.
     */
    public static void publishPublicKey(PepManager pepManager, PubkeyElement pubkeyElement, OpenPgpV4Fingerprint fingerprint)
            throws InterruptedException, PubSubException.NotALeafNodeException,
            XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException {

        String keyNodeName = PEP_NODE_PUBLIC_KEY(fingerprint);
        PubSubManager pm = pepManager.getPepPubSubManager();

        // Check if key available at data node
        // If not, publish key to data node
        LeafNode keyNode = pm.getOrCreateLeafNode(keyNodeName);
        changeAccessModelIfNecessary(keyNode, AccessModel.open);
        List<Item> items = keyNode.getItems(1);
        if (items.isEmpty()) {
            LOGGER.log(Level.FINE, "Node " + keyNodeName + " is empty. Publish.");
            keyNode.publish(new PayloadItem<>(pubkeyElement));
        } else {
            LOGGER.log(Level.FINE, "Node " + keyNodeName + " already contains key. Skip.");
        }

        // Fetch IDs from metadata node
        LeafNode metadataNode = pm.getOrCreateLeafNode(PEP_NODE_PUBLIC_KEYS);
        changeAccessModelIfNecessary(metadataNode, AccessModel.open);
        List<PayloadItem<PublicKeysListElement>> metadataItems = metadataNode.getItems(1);

        PublicKeysListElement.Builder builder = PublicKeysListElement.builder();
        if (!metadataItems.isEmpty() && metadataItems.get(0).getPayload() != null) {
            // Add old entries back to list.
            PublicKeysListElement publishedList = metadataItems.get(0).getPayload();
            for (PublicKeysListElement.PubkeyMetadataElement meta : publishedList.getMetadata().values()) {
                builder.addMetadata(meta);
            }
        }
        builder.addMetadata(new PublicKeysListElement.PubkeyMetadataElement(fingerprint, new Date()));

        // Publish IDs to metadata node
        metadataNode.publish(new PayloadItem<>(builder.build()));
    }

    /**
     * Consult the public key metadata node and fetch a list of all of our published OpenPGP public keys.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#discover-pubkey-list">
     *      XEP-0373 §4.3: Discovering Public Keys of a User</a>
     *
     * @param connection XMPP connection
     * @return content of our metadata node.
     *
     * @throws InterruptedException if the thread gets interrupted.
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol exception.
     * @throws PubSubException.NotAPubSubNodeException in case the queried entity is not a PubSub node
     * @throws PubSubException.NotALeafNodeException in case the queried node is not a {@link LeafNode}
     * @throws SmackException.NotConnectedException in case we are not connected
     * @throws SmackException.NoResponseException in case the server doesn't respond
     */
    public static PublicKeysListElement fetchPubkeysList(XMPPConnection connection)
            throws InterruptedException, XMPPException.XMPPErrorException, PubSubException.NotAPubSubNodeException,
            PubSubException.NotALeafNodeException, SmackException.NotConnectedException, SmackException.NoResponseException {
        return fetchPubkeysList(connection, null);
    }


    /**
     * Consult the public key metadata node of {@code contact} to fetch the list of their published OpenPGP public keys.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#discover-pubkey-list">
     *     XEP-0373 §4.3: Discovering Public Keys of a User</a>
     *
     * @param connection XMPP connection
     * @param contact {@link BareJid} of the user we want to fetch the list from.
     * @return content of {@code contact}'s metadata node.
     *
     * @throws InterruptedException if the thread gets interrupted.
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol exception.
     * @throws SmackException.NoResponseException in case the server doesn't respond
     * @throws PubSubException.NotALeafNodeException in case the queried node is not a {@link LeafNode}
     * @throws SmackException.NotConnectedException in case we are not connected
     * @throws PubSubException.NotAPubSubNodeException in case the queried entity is not a PubSub node
     */
    public static PublicKeysListElement fetchPubkeysList(XMPPConnection connection, BareJid contact)
            throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NoResponseException,
            PubSubException.NotALeafNodeException, SmackException.NotConnectedException, PubSubException.NotAPubSubNodeException {
        PubSubManager pm = PubSubManager.getInstanceFor(connection, contact);

        LeafNode node = getLeafNode(pm, PEP_NODE_PUBLIC_KEYS);
        List<PayloadItem<PublicKeysListElement>> list = node.getItems(1);

        if (list.isEmpty()) {
            return null;
        }

        return list.get(0).getPayload();
    }

    /**
     * Delete our metadata node.
     *
     * @param pepManager The PEP manager.
     *
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws SmackException.NotConnectedException if we are not connected.
     * @throws InterruptedException if the thread is interrupted.
     * @throws SmackException.NoResponseException if the server doesn't respond.
     * @return <code>true</code> if the node existed and was deleted, <code>false</code> if the node did not exist.
     */
    public static boolean deletePubkeysListNode(PepManager pepManager)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        PubSubManager pm = pepManager.getPepPubSubManager();
        return pm.deleteNode(PEP_NODE_PUBLIC_KEYS);
    }

    /**
     * Delete the public key node of the key with fingerprint {@code fingerprint}.
     *
     * @param pepManager The PEP manager.
     * @param fingerprint fingerprint of the key we want to delete
     *
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws SmackException.NotConnectedException if we are not connected.
     * @throws InterruptedException if the thread gets interrupted.
     * @throws SmackException.NoResponseException if the server doesn't respond.
     * @return <code>true</code> if the node existed and was deleted, <code>false</code> if the node did not exist.
     */
    public static boolean deletePublicKeyNode(PepManager pepManager, OpenPgpV4Fingerprint fingerprint)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        PubSubManager pm = pepManager.getPepPubSubManager();
        return pm.deleteNode(PEP_NODE_PUBLIC_KEY(fingerprint));
    }


    /**
     * Fetch the OpenPGP public key of a {@code contact}, identified by its OpenPGP {@code v4_fingerprint}.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#discover-pubkey">XEP-0373 §4.3</a>
     *
     * @param connection XMPP connection
     * @param contact {@link BareJid} of the contact we want to fetch a key from.
     * @param v4_fingerprint upper case, hex encoded v4 fingerprint of the contacts key.
     * @return {@link PubkeyElement} containing the requested public key.
     *
     * @throws InterruptedException if the thread gets interrupted.A
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws PubSubException.NotAPubSubNodeException in case the targeted entity is not a PubSub node.
     * @throws PubSubException.NotALeafNodeException in case the fetched node is not a {@link LeafNode}.
     * @throws SmackException.NotConnectedException in case we are not connected.
     * @throws SmackException.NoResponseException if the server doesn't respond.
     */
    public static PubkeyElement fetchPubkey(XMPPConnection connection, BareJid contact, OpenPgpV4Fingerprint v4_fingerprint)
            throws InterruptedException, XMPPException.XMPPErrorException, PubSubException.NotAPubSubNodeException,
            PubSubException.NotALeafNodeException, SmackException.NotConnectedException, SmackException.NoResponseException {
        PubSubManager pm = PubSubManager.getInstanceFor(connection, contact);
        String nodeName = PEP_NODE_PUBLIC_KEY(v4_fingerprint);

        LeafNode node = getLeafNode(pm, nodeName);

        List<PayloadItem<PubkeyElement>> list = node.getItems(1);

        if (list.isEmpty()) {
            return null;
        }

        return list.get(0).getPayload();
    }

    /**
     * Try to get a {@link LeafNode} the traditional way (first query information using disco#info), then query the node.
     * If that fails, query the node directly.
     *
     * @param pm PubSubManager
     * @param nodeName name of the node
     * @return node TODO javadoc me please
     *
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws PubSubException.NotALeafNodeException if the queried node is not a {@link LeafNode}.
     * @throws InterruptedException in case the thread gets interrupted
     * @throws PubSubException.NotAPubSubNodeException in case the queried entity is not a PubSub node.
     * @throws SmackException.NotConnectedException in case the connection is not connected.
     * @throws SmackException.NoResponseException in case the server doesn't respond.
     */
    static LeafNode getLeafNode(PubSubManager pm, String nodeName)
            throws XMPPException.XMPPErrorException, PubSubException.NotALeafNodeException, InterruptedException,
            PubSubException.NotAPubSubNodeException, SmackException.NotConnectedException, SmackException.NoResponseException {
        LeafNode node;
        try {
            node = pm.getLeafNode(nodeName);
        } catch (XMPPException.XMPPErrorException e) {
            // It might happen, that the server doesn't allow disco#info queries from strangers.
            // In that case we have to fetch the node directly
            if (e.getStanzaError().getCondition() == StanzaError.Condition.subscription_required) {
                node = getOpenLeafNode(pm, nodeName);
            } else {
                throw e;
            }
        }

        return node;
    }

    /**
     * Publishes a {@link SecretkeyElement} to the secret key node.
     * The node will be configured to use the whitelist access model to prevent access from subscribers.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#synchro-pep">
     *     XEP-0373 §5. Synchronizing the Secret Key with a Private PEP Node</a>
     *
     * @param connection {@link XMPPConnection} of the user
     * @param element a {@link SecretkeyElement} containing the encrypted secret key of the user
     *
     * @throws InterruptedException if the thread gets interrupted.
     * @throws PubSubException.NotALeafNodeException if something is wrong with the PubSub node
     * @throws XMPPException.XMPPErrorException in case of an protocol related error
     * @throws SmackException.NotConnectedException if we are not connected
     * @throws SmackException.NoResponseException /watch?v=0peBq89ZTrc
     * @throws SmackException.FeatureNotSupportedException if the Server doesn't support the whitelist access model
     */
    public static void depositSecretKey(XMPPConnection connection, SecretkeyElement element)
            throws InterruptedException, PubSubException.NotALeafNodeException,
            XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException,
            SmackException.FeatureNotSupportedException {
        if (!OpenPgpManager.serverSupportsSecretKeyBackups(connection)) {
            throw new SmackException.FeatureNotSupportedException("http://jabber.org/protocol/pubsub#access-whitelist");
        }
        PubSubManager pm = PepManager.getInstanceFor(connection).getPepPubSubManager();
        LeafNode secretKeyNode = pm.getOrCreateLeafNode(PEP_NODE_SECRET_KEY);
        OpenPgpPubSubUtil.changeAccessModelIfNecessary(secretKeyNode, AccessModel.whitelist);

        secretKeyNode.publish(new PayloadItem<>(element));
    }

    /**
     * Fetch the latest {@link SecretkeyElement} from the private backup node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#synchro-pep">
     *      XEP-0373 §5. Synchronizing the Secret Key with a Private PEP Node</a>
     *
     * @param pepManager the PEP manager.
     * @return the secret key node or null, if it doesn't exist.
     *
     * @throws InterruptedException if the thread gets interrupted
     * @throws PubSubException.NotALeafNodeException if there is an issue with the PubSub node
     * @throws XMPPException.XMPPErrorException if there is an XMPP protocol related issue
     * @throws SmackException.NotConnectedException if we are not connected
     * @throws SmackException.NoResponseException /watch?v=7U0FzQzJzyI
     */
    public static SecretkeyElement fetchSecretKey(PepManager pepManager)
            throws InterruptedException, PubSubException.NotALeafNodeException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        PubSubManager pm = pepManager.getPepPubSubManager();
        LeafNode secretKeyNode = pm.getOrCreateLeafNode(PEP_NODE_SECRET_KEY);
        List<PayloadItem<SecretkeyElement>> list = secretKeyNode.getItems(1);
        if (list.size() == 0) {
            LOGGER.log(Level.INFO, "No secret key published!");
            return null;
        }
        SecretkeyElement secretkeyElement = list.get(0).getPayload();
        return secretkeyElement;
    }

    /**
     * Delete the private backup node.
     *
     * @param pepManager the PEP manager.
     *
     * @throws XMPPException.XMPPErrorException if there is an XMPP protocol related issue
     * @throws SmackException.NotConnectedException if we are not connected
     * @throws InterruptedException if the thread gets interrupted
     * @throws SmackException.NoResponseException if the server sends no response
     * @return <code>true</code> if the node existed and was deleted, <code>false</code> if the node did not exist.
     */
    public static boolean deleteSecretKeyNode(PepManager pepManager)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        PubSubManager pm = pepManager.getPepPubSubManager();
        return pm.deleteNode(PEP_NODE_SECRET_KEY);
    }

    /**
     * Use reflection magic to get a {@link LeafNode} without doing a disco#info query.
     * This method is useful for fetching nodes that are configured with the access model 'open', since
     * some servers that announce support for that access model do not allow disco#info queries from contacts
     * which are not subscribed to the node owner. Therefore this method fetches the node directly and puts it
     * into the {@link PubSubManager}s node map.
     *
     * Note: Due to the alck of a disco#info query, it might happen, that the node doesn't exist on the server,
     * even though we add it to the node map.
     *
     * @see <a href="https://github.com/processone/ejabberd/issues/2483">Ejabberd bug tracker about the issue</a>
     * @see <a href="https://mail.jabber.org/pipermail/standards/2018-June/035206.html">
     *     Topic on the standards mailing list</a>
     *
     * @param pubSubManager pubsub manager
     * @param nodeName name of the node
     * @return leafNode TODO javadoc me please
     *
     * @throws PubSubException.NotALeafNodeException in case we already have the node cached, but it is not a LeafNode.
     */
    @SuppressWarnings("unchecked")
    public static LeafNode getOpenLeafNode(PubSubManager pubSubManager, String nodeName)
            throws PubSubException.NotALeafNodeException {

        try {

            // Get access to the PubSubManager's nodeMap
            Field field = pubSubManager.getClass().getDeclaredField("nodeMap");
            field.setAccessible(true);
            Map<String, Node> nodeMap = (Map) field.get(pubSubManager);

            // Check, if the node already exists
            Node existingNode = nodeMap.get(nodeName);
            if (existingNode != null) {

                if (existingNode instanceof LeafNode) {
                    // We already know that node
                    return (LeafNode) existingNode;

                } else {
                    // Throw a new NotALeafNodeException, as the node is not a LeafNode.
                    // Again use reflections to access the exceptions constructor.
                    Constructor<PubSubException.NotALeafNodeException> exceptionConstructor =
                            PubSubException.NotALeafNodeException.class.getDeclaredConstructor(String.class, BareJid.class);
                    exceptionConstructor.setAccessible(true);
                    throw exceptionConstructor.newInstance(nodeName, pubSubManager.getServiceJid());
                }
            }

            // Node does not exist. Create the node
            Constructor<LeafNode> constructor;
            constructor = LeafNode.class.getDeclaredConstructor(PubSubManager.class, String.class);
            constructor.setAccessible(true);
            LeafNode node = constructor.newInstance(pubSubManager, nodeName);

            // Add it to the node map
            nodeMap.put(nodeName, node);

            // And return
            return node;

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException |
                NoSuchFieldException e) {
            LOGGER.log(Level.SEVERE, "Using reflections to create a LeafNode and put it into PubSubManagers nodeMap failed.", e);
            throw new AssertionError(e);
        }
    }
}
