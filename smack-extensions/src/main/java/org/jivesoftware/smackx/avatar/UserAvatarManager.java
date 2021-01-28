/*
 *
 * Copyright 2017 Fernando Ramirez, 2019 Paul Schaub
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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.SHA1;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.avatar.element.DataExtension;
import org.jivesoftware.smackx.avatar.element.MetadataExtension;
import org.jivesoftware.smackx.avatar.listener.AvatarListener;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.pep.PepEventListener;
import org.jivesoftware.smackx.pep.PepManager;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException;
import org.jivesoftware.smackx.pubsub.PubSubManager;

import org.jxmpp.jid.EntityBareJid;

/**
 * <h1>User Avatar manager class.</h1>
 * This manager allows publication of user avatar images via PubSub, as well as publication of
 * {@link MetadataExtension MetadataExtensions} containing {@link MetadataInfo} elements for avatars
 * available via PubSub, HTTP or external third-party services via {@link MetadataPointer} elements.
 * <p>
 * The easiest way to publish a PNG avatar (support for PNG files is REQUIRED), is to use
 * {@link #publishPNGAvatar(File, int, int)} which will publish the image data to PubSub and inform
 * any subscribers via a metadata update.
 * <p>
 * Uploading avatars via HTTP is not in the scope of this manager (you could use Smacks
 * HTTPFileUploadManager from smack-experimental for that), but publishing metadata updates pointing
 * to HTTP resources is supported. Use {@link #publishHttpPNGAvatarMetadata(String, URL, long, int, int)} for that.
 * <p>
 * By calling {@link #enable()}, the {@link UserAvatarManager} will start receiving metadata updates from
 * contacts and other entities. If you want to get informed about those updates, you can register listeners
 * by calling {@link #addAvatarListener(AvatarListener)}.
 * <p>
 * If you store avatars locally, it is recommended to also set an {@link AvatarMetadataStore}, which is responsible
 * for keeping track of which avatar files are available locally. If you register such a store via
 * {@link #setAvatarMetadataStore(AvatarMetadataStore)}, your registered {@link AvatarListener AvatarListeners}
 * will only inform you about those avatars that are not yet locally available.
 * <p>
 * To fetch an avatar from PubSub, use {@link #fetchAvatarFromPubSub(EntityBareJid, MetadataInfo)} which will
 * retrieve the avatar data from PubSub and mark the avatar as locally available in the {@link AvatarMetadataStore}
 * if one is registered.
 * <p>
 * Fetching avatars published via HTTP is out of scope of this manager. If you do implement it, remember to mark the
 * avatar as locally available in your {@link AvatarMetadataStore} after you retrieved it.
 *
 * @author Fernando Ramirez
 * @author Paul Schaub
 * @see <a href="http://xmpp.org/extensions/xep-0084.html">XEP-0084: User Avatar</a>
 */
public final class UserAvatarManager extends Manager {

    public static final String DATA_NAMESPACE = "urn:xmpp:avatar:data";
    public static final String METADATA_NAMESPACE = "urn:xmpp:avatar:metadata";
    public static final String FEATURE_METADATA = METADATA_NAMESPACE + "+notify";

    private static final Map<XMPPConnection, UserAvatarManager> INSTANCES = new WeakHashMap<>();

    public static final String TYPE_PNG = "image/png";
    public static final String TYPE_GIF = "image/gif";
    public static final String TYPE_JPEG = "image/jpeg";

    private final PepManager pepManager;
    private final ServiceDiscoveryManager serviceDiscoveryManager;

    private AvatarMetadataStore metadataStore;
    private final Set<AvatarListener> avatarListeners = new HashSet<>();

    /**
     * Get the singleton instance of UserAvatarManager.
     *
     * @param connection {@link XMPPConnection}.
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
        this.pepManager = PepManager.getInstanceFor(connection);
        this.serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
    }

    /**
     * Returns true if User Avatar publishing is supported by the server.
     * In order to support User Avatars the server must have support for XEP-0163: Personal Eventing Protocol (PEP).
     *
     * @return true if User Avatar is supported by the server.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0163.html">XEP-0163: Personal Eventing Protocol</a>
     *
     * @throws NoResponseException if the server does not respond
     * @throws XMPPErrorException if a protocol level error occurs
     * @throws NotConnectedException if the connection is not connected
     * @throws InterruptedException if the thread is interrupted
     */
    public boolean isSupportedByServer()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return pepManager.isSupported();
    }

    /**
     * Announce support for User Avatars and start receiving avatar updates.
     */
    public void enable() {
        pepManager.addPepEventListener(FEATURE_METADATA, MetadataExtension.class, metadataExtensionListener);
        serviceDiscoveryManager.addFeature(FEATURE_METADATA);
    }

    /**
     * Stop receiving avatar updates.
     */
    public void disable() {
        serviceDiscoveryManager.removeFeature(FEATURE_METADATA);
        pepManager.removePepEventListener(metadataExtensionListener);
    }

    /**
     * Set an {@link AvatarMetadataStore} which is used to store information about the local availability of avatar
     * data.
     * @param metadataStore metadata store
     */
    public void setAvatarMetadataStore(AvatarMetadataStore metadataStore) {
        this.metadataStore = metadataStore;
    }

    /**
     * Register an {@link AvatarListener} in order to be notified about incoming avatar metadata updates.
     *
     * @param listener listener
     * @return true if the set of listeners did not already contain the listener
     */
    public synchronized boolean addAvatarListener(AvatarListener listener) {
        return avatarListeners.add(listener);
    }

    /**
     * Unregister an {@link AvatarListener} to stop being notified about incoming avatar metadata updates.
     *
     * @param listener listener
     * @return true if the set of listeners contained the listener
     */
    public synchronized boolean removeAvatarListener(AvatarListener listener) {
        return avatarListeners.remove(listener);
    }

    private LeafNode getOrCreateDataNode()
            throws NoResponseException, NotConnectedException, InterruptedException, XMPPErrorException, PubSubException.NotALeafNodeException {
        return pepManager.getPepPubSubManager().getOrCreateLeafNode(DATA_NAMESPACE);
    }

    private LeafNode getOrCreateMetadataNode()
            throws NoResponseException, NotConnectedException, InterruptedException, XMPPErrorException, PubSubException.NotALeafNodeException {
        return pepManager.getPepPubSubManager().getOrCreateLeafNode(METADATA_NAMESPACE);
    }

    /**
     * Publish a PNG avatar and its metadata to PubSub.
     * If you know what the dimensions of the image are, use {@link #publishPNGAvatar(byte[], int, int)} instead.
     *
     * @param data raw bytes of the avatar
     *
     * @throws XMPPErrorException if a protocol level error occurs
     * @throws PubSubException.NotALeafNodeException if either the metadata node or the data node is not a
     *                                               {@link LeafNode}
     * @throws NotConnectedException if the connection is not connected
     * @throws InterruptedException if the thread is interrupted
     * @throws NoResponseException if the server does not respond
     */
    public void publishPNGAvatar(byte[] data) throws XMPPErrorException, PubSubException.NotALeafNodeException,
            NotConnectedException, InterruptedException, NoResponseException {
        publishPNGAvatar(data, 0, 0);
    }

    /**
     * Publish a PNG Avatar and its metadata to PubSub.
     *
     * @param data raw bytes of the avatar
     * @param height height of the image in pixels
     * @param width width of the image in pixels
     *
     * @throws XMPPErrorException if a protocol level error occurs
     * @throws PubSubException.NotALeafNodeException if either the metadata node or the data node is not a
     *                                               {@link LeafNode}
     * @throws NotConnectedException if the connection is not connected
     * @throws InterruptedException if the thread is interrupted
     * @throws NoResponseException if the server does not respond
     */
    public void publishPNGAvatar(byte[] data, int height, int width)
            throws XMPPErrorException, PubSubException.NotALeafNodeException, NotConnectedException,
            InterruptedException, NoResponseException {
        String id = publishPNGAvatarData(data);
        publishPNGAvatarMetadata(id, data.length, height, width);
    }

    /**
     * Publish a PNG avatar and its metadata to PubSub.
     * If you know the dimensions of the image, use {@link #publishPNGAvatar(File, int, int)} instead.
     *
     * @param pngFile PNG File
     *
     * @throws IOException if an {@link IOException} occurs while reading the file
     * @throws XMPPErrorException if a protocol level error occurs
     * @throws PubSubException.NotALeafNodeException if either the metadata node or the data node is not a valid
     * {@link LeafNode}
     * @throws NotConnectedException if the connection is not connected
     * @throws InterruptedException if the thread is interrupted
     * @throws NoResponseException if the server does not respond
     */
    public void publishPNGAvatar(File pngFile) throws NotConnectedException, InterruptedException,
            PubSubException.NotALeafNodeException, NoResponseException, IOException, XMPPErrorException {
        publishPNGAvatar(pngFile, 0, 0);
    }

    /**
     * Publish a PNG avatar and its metadata to PubSub.
     *
     * @param pngFile PNG File
     * @param height height of the image
     * @param width width of the image
     *
     * @throws IOException if an {@link IOException} occurs while reading the file
     * @throws XMPPErrorException if a protocol level error occurs
     * @throws PubSubException.NotALeafNodeException if either the metadata node or the data node is not a valid
     * {@link LeafNode}
     * @throws NotConnectedException if the connection is not connected
     * @throws InterruptedException if the thread is interrupted
     * @throws NoResponseException if the server does not respond
     */
    public void publishPNGAvatar(File pngFile, int height, int width)
            throws IOException, XMPPErrorException, PubSubException.NotALeafNodeException, NotConnectedException,
            InterruptedException, NoResponseException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream((int) pngFile.length());
             InputStream in = new BufferedInputStream(new FileInputStream(pngFile))) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            byte[] bytes = out.toByteArray();
            publishPNGAvatar(bytes, height, width);
        }
    }

    /**
     * Fetch a published user avatar from their PubSub service.
     *
     * @param from {@link EntityBareJid} of the avatars owner
     * @param metadataInfo {@link MetadataInfo} of the avatar that shall be fetched
     *
     * @return bytes of the avatar
     *
     * @throws InterruptedException if the thread gets interrupted
     * @throws PubSubException.NotALeafNodeException if the data node is not a {@link LeafNode}
     * @throws NoResponseException if the server does not respond
     * @throws NotConnectedException if the connection is not connected
     * @throws XMPPErrorException if a protocol level error occurs
     * @throws PubSubException.NotAPubSubNodeException if the data node is not a valid PubSub node
     * @throws UserAvatarException.AvatarMetadataMismatchException if the data in the data node does not match whats
     * promised by the {@link MetadataInfo} element
     * @throws UserAvatarException.NotAPubSubAvatarInfoElementException if the user tries to fetch the avatar using an
     * info element that points to a HTTP resource
     */
    public byte[] fetchAvatarFromPubSub(EntityBareJid from, MetadataInfo metadataInfo)
            throws InterruptedException, PubSubException.NotALeafNodeException, NoResponseException,
            NotConnectedException, XMPPErrorException, PubSubException.NotAPubSubNodeException,
            UserAvatarException.AvatarMetadataMismatchException {
        if (metadataInfo.getUrl() != null) {
            throw new UserAvatarException.NotAPubSubAvatarInfoElementException("Provided MetadataInfo element points to " +
                    "a HTTP resource, not to a PubSub item.");
        }
        LeafNode dataNode = PubSubManager.getInstanceFor(connection(), from)
                .getLeafNode(DATA_NAMESPACE);

        List<PayloadItem<DataExtension>> dataItems = dataNode.getItems(1, metadataInfo.getId());
        DataExtension data = dataItems.get(0).getPayload();
        if (data.getData().length != metadataInfo.getBytes().intValue()) {
            throw new UserAvatarException.AvatarMetadataMismatchException("Avatar Data with itemId '" + metadataInfo.getId() +
                    "' of " + from.asUnescapedString() + " does not match the Metadata (metadata promises " +
                    metadataInfo.getBytes().intValue() + " bytes, data contains " + data.getData().length + " bytes)");
        }
        if (metadataStore != null) {
            metadataStore.setAvatarAvailable(from, metadataInfo.getId());
        }
        return data.getData();
    }

    private String publishPNGAvatarData(byte[] data)
            throws NoResponseException, NotConnectedException, XMPPErrorException, InterruptedException,
            PubSubException.NotALeafNodeException {
        String itemId = Base64.encodeToString(SHA1.bytes(data));
        publishAvatarData(data, itemId);
        return itemId;
    }

    /**
     * Publish some avatar image data to PubSub.
     * Note, that if the image is an image of type {@link #TYPE_PNG}, the itemId MUST be the SHA-1 sum of that image.
     * If however the image is not of type {@link #TYPE_PNG}, the itemId MUST be the SHA-1 sum of the PNG encoded
     * representation of this image (an avatar can be published in several image formats, but at least one of them
     * must be of type {@link #TYPE_PNG}).
     *
     * @param data raw bytes of the image
     * @param itemId SHA-1 sum of the PNG encoded representation of this image.
     *
     * @throws NoResponseException if the server does not respond
     * @throws NotConnectedException if the connection is not connected
     * @throws XMPPErrorException if a protocol level error occurs
     * @throws InterruptedException if the thread is interrupted
     * @throws PubSubException.NotALeafNodeException if the data node is not a {@link LeafNode}.
     */
    public void publishAvatarData(byte[] data, String itemId)
            throws NoResponseException, NotConnectedException, XMPPErrorException, InterruptedException, PubSubException.NotALeafNodeException {
        DataExtension dataExtension = new DataExtension(data);
        getOrCreateDataNode().publish(new PayloadItem<>(itemId, dataExtension));
    }

    /**
     * Publish metadata about an avatar of type {@link #TYPE_PNG} to the metadata node.
     *
     * @param itemId SHA-1 sum of the image of type {@link #TYPE_PNG}
     * @param info info element containing metadata of the file
     * @param pointers optional list of metadata pointer elements
     *
     * @throws NoResponseException if the server does not respond
     * @throws XMPPErrorException if a protocol level error occurs
     * @throws NotConnectedException of the connection is not connected
     * @throws InterruptedException if the thread is interrupted
     * @throws PubSubException.NotALeafNodeException if the metadata node is not a {@link LeafNode}
     * @throws UserAvatarException.AvatarMetadataMissingPNGInfoException if the info element does not point to an
     * avatar image of type {@link #TYPE_PNG} available in PubSub.
     */
    public void publishPNGAvatarMetadata(String itemId, MetadataInfo info, List<MetadataPointer> pointers)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotALeafNodeException {
        publishAvatarMetadata(itemId, Collections.singletonList(info), pointers);
    }

    /**
     * Publish avatar metadata.
     * The list of {@link MetadataInfo} elements can contain info about several image data types. However, there must
     * be at least one {@link MetadataInfo} element about an image of type {@link #TYPE_PNG} which is destined to
     * publication in PubSub. Its id MUST equal the itemId parameter.
     *
     * @param itemId SHA-1 sum of the avatar image representation of type {@link #TYPE_PNG}
     * @param infos list of metadata elements
     * @param pointers optional list of pointer elements
     *
     * @throws NoResponseException if the server does not respond
     * @throws XMPPErrorException if a protocol level error occurs
     * @throws NotConnectedException if the connection is not connected
     * @throws InterruptedException if the thread is interrupted
     * @throws PubSubException.NotALeafNodeException if the metadata node is not a {@link LeafNode}
     * @throws UserAvatarException.AvatarMetadataMissingPNGInfoException if the list of {@link MetadataInfo} elements
     * does not contain at least one PNG image
     *
     * @see <a href="https://xmpp.org/extensions/xep-0084.html#proto-info">
     *     §4.2.1 Info Element - About the restriction that at least one info element must describe a PNG image.</a>
     */
    public void publishAvatarMetadata(String itemId, List<MetadataInfo> infos, List<MetadataPointer> pointers)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotALeafNodeException {
        // Check if metadata extension contains at least one png image available in PubSub
        boolean containsPng = false;
        for (MetadataInfo info : infos) {
            if (TYPE_PNG.equals(info.getType())) {
                containsPng = true;
                break;
            }
        }
        if (!containsPng) {
            throw new UserAvatarException.AvatarMetadataMissingPNGInfoException(
                    "The MetadataExtension must contain at least one info element describing an image of type " +
                            "\"" + TYPE_PNG + "\"");
        }

        MetadataExtension metadataExtension = new MetadataExtension(infos, pointers);
        getOrCreateMetadataNode().publish(new PayloadItem<>(itemId, metadataExtension));

        if (metadataStore == null) {
            return;
        }
        // Mark our own avatar as locally available so that we don't get updates for it
        metadataStore.setAvatarAvailable(connection().getUser().asEntityBareJidOrThrow(), itemId);
    }

    /**
     * Publish metadata about a PNG avatar available via HTTP.
     * This method can be used together with HTTP File Upload as an alternative to PubSub for avatar publishing.
     *
     * @param itemId SHA-1 sum of the avatar image file.
     * @param url HTTP(S) Url of the image file.
     * @param bytes size of the file in bytes
     * @param pixelsHeight height of the image file in pixels
     * @param pixelsWidth width of the image file in pixels
     *
     * @throws NoResponseException if the server does not respond
     * @throws XMPPErrorException of a protocol level error occurs
     * @throws NotConnectedException if the connection is not connected
     * @throws InterruptedException if the thread is interrupted
     * @throws PubSubException.NotALeafNodeException if the metadata node is not a {@link LeafNode}
     */
    public void publishHttpPNGAvatarMetadata(String itemId, URL url, long bytes,
                                          int pixelsHeight, int pixelsWidth)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotALeafNodeException {
        MetadataInfo info = new MetadataInfo(itemId, url, bytes, TYPE_PNG, pixelsHeight, pixelsWidth);
        publishPNGAvatarMetadata(itemId, info, null);
    }

    /**
     * Publish avatar metadata about a PNG avatar with its size in pixels.
     *
     * @param itemId SHA-1 hash of the PNG encoded image
     * @param bytes number of bytes of this particular image data array
     * @param pixelsHeight height of this image in pixels
     * @param pixelsWidth width of this image in pixels
     *
     * @throws NoResponseException if the server does not respond
     * @throws XMPPErrorException if a protocol level error occurs
     * @throws NotConnectedException if the connection is not connected
     * @throws PubSubException.NotALeafNodeException if the metadata node is not a {@link LeafNode}
     * @throws InterruptedException if the thread is interrupted
     */
    public void publishPNGAvatarMetadata(String itemId, long bytes, int pixelsHeight, int pixelsWidth)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotALeafNodeException {
        MetadataInfo info = new MetadataInfo(itemId, null, bytes, TYPE_PNG, pixelsHeight, pixelsWidth);
        publishPNGAvatarMetadata(itemId, info, null);
    }

    /**
     * Publish an empty metadata element to disable avatar publishing.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0084.html#proto-meta">§4.2 Metadata Element</a>
     *
     * @throws NoResponseException if the server does not respond
     * @throws XMPPErrorException if a protocol level error occurs
     * @throws NotConnectedException if the connection is not connected
     * @throws InterruptedException if the thread is interrupted
     * @throws PubSubException.NotALeafNodeException if the metadata node is not a {@link LeafNode}
     */
    public void unpublishAvatar()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotALeafNodeException {
        getOrCreateMetadataNode().publish(new PayloadItem<>(new MetadataExtension()));
    }

    @SuppressWarnings("UnnecessaryAnonymousClass")
    private final PepEventListener<MetadataExtension> metadataExtensionListener = new PepEventListener<MetadataExtension>() {
        @Override
        public void onPepEvent(EntityBareJid from, MetadataExtension event, String id, Message carrierMessage) {
            if (metadataStore != null && metadataStore.hasAvatarAvailable(from, id)) {
                // The metadata store implies that we have a local copy of the published image already. Skip.
                return;
            }

            for (AvatarListener listener : avatarListeners) {
                listener.onAvatarUpdateReceived(from, event);
            }
        }
    };
}
