/**
 *
 * Copyright 2017 Fernando Ramirez
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
package org.jivesoftware.smackx.avatar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.util.SHA1;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.avatar.element.DataExtension;
import org.jivesoftware.smackx.avatar.element.MetadataExtension;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverItems.Item;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;

/**
 * User Avatar manager class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0084.html">XEP-0084: User
 *      Avatar</a>
 */
public final class UserAvatarManager extends Manager {

    public static final String DATA_NAMESPACE = "urn:xmpp:avatar:data";
    public static final String METADATA_NAMESPACE = "urn:xmpp:avatar:metadata";

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    private static final Map<XMPPConnection, UserAvatarManager> INSTANCES = new WeakHashMap<>();

    /**
     * Get the singleton instance of UserAvatarManager.
     *
     * @param connection
     * @return the instance of UserAvatarManager
     */
    public static synchronized UserAvatarManager getInstanceFor(XMPPConnection connection) {
        UserAvatarManager userAvatarManager = INSTANCES.get(connection);

        if (userAvatarManager == null) {
            userAvatarManager = new UserAvatarManager(connection);
            INSTANCES.put(connection, userAvatarManager);
        }

        return userAvatarManager;
    }

    private UserAvatarManager(XMPPConnection connection) {
        super(connection);
    }

    /**
     * Returns true if User Avatar is supported by the server.
     * 
     * @return true if User Avatar is supported by the server.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public boolean isSupportedByServer()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return findNodeItem(DATA_NAMESPACE) && findNodeItem(METADATA_NAMESPACE);
    }

    private boolean findNodeItem(String nodeName)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection());
        List<Item> items = serviceDiscoveryManager.discoverItems(connection().getUser().asBareJid(), nodeName)
                .getItems();
        return items != null && items.size() > 0;
    }

    /**
     * Get the data node.
     * 
     * @return the data node
     * @throws NoResponseException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws XMPPErrorException
     */
    public LeafNode getDataNode()
            throws NoResponseException, NotConnectedException, InterruptedException, XMPPErrorException {
        return PubSubManager.getInstance(connection()).getOrCreateLeafNode(DATA_NAMESPACE);
    }

    /**
     * Get the metadata node.
     * 
     * @return the metadata node
     * @throws NoResponseException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws XMPPErrorException
     */
    public LeafNode getMetadataNode()
            throws NoResponseException, NotConnectedException, InterruptedException, XMPPErrorException {
        return PubSubManager.getInstance(connection()).getOrCreateLeafNode(METADATA_NAMESPACE);
    }

    /**
     * Publish avatar data.
     * 
     * @param data
     * @param itemId
     * @throws NoResponseException
     * @throws NotConnectedException
     * @throws XMPPErrorException
     * @throws InterruptedException
     */
    public void publishAvatarData(byte[] data, String itemId)
            throws NoResponseException, NotConnectedException, XMPPErrorException, InterruptedException {
        DataExtension dataExtension = new DataExtension(data);
        getDataNode().publish(new PayloadItem<DataExtension>(itemId, dataExtension));
    }

    /**
     * Publish avatar data.
     * 
     * @param data
     * @return created item id
     * @throws NoResponseException
     * @throws NotConnectedException
     * @throws XMPPErrorException
     * @throws InterruptedException
     */
    public String publishAvatarData(byte[] data)
            throws NoResponseException, NotConnectedException, XMPPErrorException, InterruptedException {
        String itemId = Base64.encodeToString(SHA1.bytes(data));
        publishAvatarData(data, itemId);
        return itemId;
    }

    /**
     * Publish avatar metadata.
     * 
     * @param itemId
     * @param info
     * @param pointers
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void publishAvatarMetadata(String itemId, MetadataInfo info, List<MetadataPointer> pointers)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        List<MetadataInfo> infos = new ArrayList<>();
        infos.add(info);
        publishAvatarMetadata(itemId, infos, pointers);
    }

    /**
     * Publish avatar metadata.
     * 
     * @param itemId
     * @param infos
     * @param pointers
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void publishAvatarMetadata(String itemId, List<MetadataInfo> infos, List<MetadataPointer> pointers)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MetadataExtension metadataExtension = new MetadataExtension(infos, pointers);
        getMetadataNode().publish(new PayloadItem<MetadataExtension>(itemId, metadataExtension));
    }

    /**
     * Publish HTTP avatar metadata.
     * 
     * @param itemId
     * @param id
     * @param url
     * @param bytes
     * @param type
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void publishHTTPAvatarMetadata(String itemId, String id, String url, long bytes, String type)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MetadataInfo info = new MetadataInfo(id, url, bytes, type, 0, 0);
        publishAvatarMetadata(itemId, info, null);
    }

    /**
     * Publish HTTP avatar metadata with its size in pixels.
     * 
     * @param itemId
     * @param id
     * @param url
     * @param bytes
     * @param type
     * @param pixelsHeight
     * @param pixelsWidth
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void publishHTTPAvatarMetadataWithSize(String itemId, String id, String url, long bytes, String type,
            int pixelsHeight, int pixelsWidth)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MetadataInfo info = new MetadataInfo(id, url, bytes, type, pixelsHeight, pixelsWidth);
        publishAvatarMetadata(itemId, info, null);
    }

    /**
     * Publish avatar metadata.
     * 
     * @param itemId
     * @param id
     * @param bytes
     * @param type
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void publishAvatarMetadata(String itemId, String id, long bytes, String type)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MetadataInfo info = new MetadataInfo(id, null, bytes, type, 0, 0);
        publishAvatarMetadata(itemId, info, null);
    }

    /**
     * Publish avatar metadata with its size in pixels.
     * 
     * @param itemId
     * @param id
     * @param bytes
     * @param type
     * @param pixelsHeight
     * @param pixelsWidth
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void publishAvatarMetadataWithSize(String itemId, String id, long bytes, String type, int pixelsHeight,
            int pixelsWidth)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MetadataInfo info = new MetadataInfo(id, null, bytes, type, pixelsHeight, pixelsWidth);
        publishAvatarMetadata(itemId, info, null);
    }

    /**
     * Get last data of an item.
     * 
     * @param itemId
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void requestLastData(String itemId)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        List<String> ids = new ArrayList<>();
        ids.add(itemId);
        getDataNode().getItems(ids);
    }

    /**
     * Disable avatar publishing.
     * 
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void disableAvatarPublishing()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        getMetadataNode().publish(new PayloadItem<MetadataExtension>(new MetadataExtension(null)));
    }

}
