/**
 * Copyright 2009 Jonas Ådahl.
 * Copyright 2011-2013 Florian Schmaus
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.entitycaps;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketInterceptor;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.util.Base64;
import org.jivesoftware.smack.util.Cache;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.NodeInformationProvider;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.entitycaps.cache.EntityCapsPersistentCache;
import org.jivesoftware.smackx.entitycaps.packet.CapsExtension;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.packet.DiscoverInfo.Feature;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Keeps track of entity capabilities.
 * 
 * @author Florian Schmaus
 */
public class EntityCapsManager {

    public static final String NAMESPACE = "http://jabber.org/protocol/caps";
    public static final String ELEMENT = "c";

    private static final String ENTITY_NODE = "http://www.igniterealtime.org/projects/smack";
    private static final Map<String, MessageDigest> SUPPORTED_HASHES = new HashMap<String, MessageDigest>();

    protected static EntityCapsPersistentCache persistentCache;

    private static Map<Connection, EntityCapsManager> instances = Collections
            .synchronizedMap(new WeakHashMap<Connection, EntityCapsManager>());

    /**
     * Map of (node + '#" + hash algorithm) to DiscoverInfo data
     */
    protected static Map<String, DiscoverInfo> caps = new Cache<String, DiscoverInfo>(1000, -1);

    /**
     * Map of Full JID -&gt; DiscoverInfo/null. In case of c2s connection the
     * key is formed as user@server/resource (resource is required) In case of
     * link-local connection the key is formed as user@host (no resource) In
     * case of a server or component the key is formed as domain
     */
    protected static Map<String, NodeVerHash> jidCaps = new Cache<String, NodeVerHash>(10000, -1);

    static {
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(Connection connection) {
                getInstanceFor(connection);
            }
        });

        try {
            MessageDigest sha1MessageDigest = MessageDigest.getInstance("SHA-1");
            SUPPORTED_HASHES.put("sha-1", sha1MessageDigest);
        } catch (NoSuchAlgorithmException e) {
            // Ignore
        }
    }

    private WeakReference<Connection> weakRefConnection;
    private ServiceDiscoveryManager sdm;
    private boolean entityCapsEnabled;
    private String currentCapsVersion;
    private boolean presenceSend = false;
    private Queue<String> lastLocalCapsVersions = new ConcurrentLinkedQueue<String>();

    /**
     * Add DiscoverInfo to the database.
     * 
     * @param nodeVer
     *            The node and verification String (e.g.
     *            "http://psi-im.org#q07IKJEyjvHSyhy//CH0CxmKi8w=").
     * @param info
     *            DiscoverInfo for the specified node.
     */
    public static void addDiscoverInfoByNode(String nodeVer, DiscoverInfo info) {
        caps.put(nodeVer, info);

        if (persistentCache != null)
            persistentCache.addDiscoverInfoByNodePersistent(nodeVer, info);
    }

    /**
     * Get the Node version (node#ver) of a JID. Returns a String or null if
     * EntiyCapsManager does not have any information.
     * 
     * @param user
     *            the user (Full JID)
     * @return the node version (node#ver) or null
     */
    public static String getNodeVersionByJid(String jid) {
        NodeVerHash nvh = jidCaps.get(jid);
        if (nvh != null) {
            return nvh.nodeVer;
        } else {
            return null;
        }
    }

    public static NodeVerHash getNodeVerHashByJid(String jid) {
        return jidCaps.get(jid);
    }

    /**
     * Get the discover info given a user name. The discover info is returned if
     * the user has a node#ver associated with it and the node#ver has a
     * discover info associated with it.
     * 
     * @param user
     *            user name (Full JID)
     * @return the discovered info
     */
    public static DiscoverInfo getDiscoverInfoByUser(String user) {
        NodeVerHash nvh = jidCaps.get(user);
        if (nvh == null)
            return null;

        return getDiscoveryInfoByNodeVer(nvh.nodeVer);
    }

    /**
     * Retrieve DiscoverInfo for a specific node.
     * 
     * @param nodeVer
     *            The node name (e.g.
     *            "http://psi-im.org#q07IKJEyjvHSyhy//CH0CxmKi8w=").
     * @return The corresponding DiscoverInfo or null if none is known.
     */
    public static DiscoverInfo getDiscoveryInfoByNodeVer(String nodeVer) {
        DiscoverInfo info = caps.get(nodeVer);
        if (info != null)
            info = new DiscoverInfo(info);

        return info;
    }

    /**
     * Set the persistent cache implementation
     * 
     * @param cache
     * @throws IOException
     */
    public static void setPersistentCache(EntityCapsPersistentCache cache) throws IOException {
        if (persistentCache != null)
            throw new IllegalStateException("Entity Caps Persistent Cache was already set");
        persistentCache = cache;
        persistentCache.replay();
    }

    /**
     * Sets the maximum Cache size for the JID to nodeVer Cache
     * 
     * @param maxCacheSize
     */
    @SuppressWarnings("rawtypes")
    public static void setJidCapsMaxCacheSize(int maxCacheSize) {
        ((Cache) jidCaps).setMaxCacheSize(maxCacheSize);
    }

    /**
     * Sets the maximum Cache size for the nodeVer to DiscoverInfo Cache
     * 
     * @param maxCacheSize
     */
    @SuppressWarnings("rawtypes")
    public static void setCapsMaxCacheSize(int maxCacheSize) {
        ((Cache) caps).setMaxCacheSize(maxCacheSize);
    }

    private EntityCapsManager(Connection connection) {
        this.weakRefConnection = new WeakReference<Connection>(connection);
        this.sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        instances.put(connection, this);

        connection.addConnectionListener(new ConnectionListener() {
            public void connectionClosed() {
                presenceSend = false;
            }

            public void connectionClosedOnError(Exception e) {
                presenceSend = false;
            }

            public void reconnectionFailed(Exception e) {
                // ignore
            }

            public void reconnectingIn(int seconds) {
                // ignore
            }

            public void reconnectionSuccessful() {
                // ignore
            }
        });

        // This calculates the local entity caps version
        updateLocalEntityCaps();

        if (SmackConfiguration.autoEnableEntityCaps())
            enableEntityCaps();

        PacketFilter packetFilter = new AndFilter(new PacketTypeFilter(Presence.class), new PacketExtensionFilter(
                ELEMENT, NAMESPACE));
        connection.addPacketListener(new PacketListener() {
            // Listen for remote presence stanzas with the caps extension
            // If we receive such a stanza, record the JID and nodeVer
            @Override
            public void processPacket(Packet packet) {
                if (!entityCapsEnabled())
                    return;

                CapsExtension ext = (CapsExtension) packet.getExtension(EntityCapsManager.ELEMENT,
                        EntityCapsManager.NAMESPACE);

                String hash = ext.getHash().toLowerCase();
                if (!SUPPORTED_HASHES.containsKey(hash))
                    return;

                String from = packet.getFrom();
                String node = ext.getNode();
                String ver = ext.getVer();

                jidCaps.put(from, new NodeVerHash(node, ver, hash));
            }

        }, packetFilter);

        packetFilter = new AndFilter(new PacketTypeFilter(Presence.class), new NotFilter(new PacketExtensionFilter(
                ELEMENT, NAMESPACE)));
        connection.addPacketListener(new PacketListener() {
            @Override
            public void processPacket(Packet packet) {
                // always remove the JID from the map, even if entityCaps are
                // disabled
                String from = packet.getFrom();
                jidCaps.remove(from);
            }
        }, packetFilter);

        packetFilter = new PacketTypeFilter(Presence.class);
        connection.addPacketSendingListener(new PacketListener() {
            @Override
            public void processPacket(Packet packet) {
                presenceSend = true;
            }
        }, packetFilter);

        // Intercept presence packages and add caps data when intended.
        // XEP-0115 specifies that a client SHOULD include entity capabilities
        // with every presence notification it sends.
        PacketFilter capsPacketFilter = new PacketTypeFilter(Presence.class);
        PacketInterceptor packetInterceptor = new PacketInterceptor() {
            public void interceptPacket(Packet packet) {
                if (!entityCapsEnabled)
                    return;

                CapsExtension caps = new CapsExtension(ENTITY_NODE, getCapsVersion(), "sha-1");
                packet.addExtension(caps);
            }
        };
        connection.addPacketInterceptor(packetInterceptor, capsPacketFilter);
        // It's important to do this as last action. Since it changes the
        // behavior of the SDM in some ways
        sdm.setEntityCapsManager(this);
    }

    public static synchronized EntityCapsManager getInstanceFor(Connection connection) {
        if (SUPPORTED_HASHES.size() <= 0)
            throw new IllegalStateException("No supported hashes for EntityCapsManager");

        EntityCapsManager entityCapsManager = instances.get(connection);

        if (entityCapsManager == null) {
            entityCapsManager = new EntityCapsManager(connection);
        }

        return entityCapsManager;
    }

    public void enableEntityCaps() {
        // Add Entity Capabilities (XEP-0115) feature node.
        sdm.addFeature(NAMESPACE);
        updateLocalEntityCaps();
        entityCapsEnabled = true;
    }

    public void disableEntityCaps() {
        entityCapsEnabled = false;
        sdm.removeFeature(NAMESPACE);
    }

    public boolean entityCapsEnabled() {
        return entityCapsEnabled;
    }

    /**
     * Remove a record telling what entity caps node a user has.
     * 
     * @param user
     *            the user (Full JID)
     */
    public void removeUserCapsNode(String user) {
        jidCaps.remove(user);
    }

    /**
     * Get our own caps version. The version depends on the enabled features. A
     * caps version looks like '66/0NaeaBKkwk85efJTGmU47vXI='
     * 
     * @return our own caps version
     */
    public String getCapsVersion() {
        return currentCapsVersion;
    }

    /**
     * Returns the local entity's NodeVer (e.g.
     * "http://www.igniterealtime.org/projects/smack/#66/0NaeaBKkwk85efJTGmU47vXI=
     * )
     * 
     * @return
     */
    public String getLocalNodeVer() {
        return ENTITY_NODE + '#' + getCapsVersion();
    }

    /**
     * Returns true if Entity Caps are supported by a given JID
     * 
     * @param jid
     * @return
     */
    public boolean areEntityCapsSupported(String jid) {
        if (jid == null)
            return false;

        try {
            DiscoverInfo result = sdm.discoverInfo(jid);
            return result.containsFeature(NAMESPACE);
        } catch (XMPPException e) {
            return false;
        }
    }

    /**
     * Returns true if Entity Caps are supported by the local service/server
     * 
     * @return
     */
    public boolean areEntityCapsSupportedByServer() {
        return areEntityCapsSupported(weakRefConnection.get().getServiceName());
    }

    /**
     * Updates the local user Entity Caps information with the data provided
     * 
     * If we are connected and there was already a presence send, another
     * presence is send to inform others about your new Entity Caps node string.
     * 
     * @param discoverInfo
     *            the local users discover info (mostly the service discovery
     *            features)
     * @param identityType
     *            the local users identity type
     * @param identityName
     *            the local users identity name
     * @param extendedInfo
     *            the local users extended info
     */
    public void updateLocalEntityCaps() {
        Connection connection = weakRefConnection.get();

        DiscoverInfo discoverInfo = new DiscoverInfo();
        discoverInfo.setType(IQ.Type.RESULT);
        discoverInfo.setNode(getLocalNodeVer());
        if (connection != null)
            discoverInfo.setFrom(connection.getUser());
        sdm.addDiscoverInfoTo(discoverInfo);

        currentCapsVersion = generateVerificationString(discoverInfo, "sha-1");
        addDiscoverInfoByNode(ENTITY_NODE + '#' + currentCapsVersion, discoverInfo);
        if (lastLocalCapsVersions.size() > 10) {
            String oldCapsVersion = lastLocalCapsVersions.poll();
            sdm.removeNodeInformationProvider(ENTITY_NODE + '#' + oldCapsVersion);
        }
        lastLocalCapsVersions.add(currentCapsVersion);

        caps.put(currentCapsVersion, discoverInfo);
        if (connection != null)
            jidCaps.put(connection.getUser(), new NodeVerHash(ENTITY_NODE, currentCapsVersion, "sha-1"));

        final List<Identity> identities = new LinkedList<Identity>(ServiceDiscoveryManager.getInstanceFor(connection).getIdentities());
        sdm.setNodeInformationProvider(ENTITY_NODE + '#' + currentCapsVersion, new NodeInformationProvider() {
            List<String> features = sdm.getFeaturesList();
            List<PacketExtension> packetExtensions = sdm.getExtendedInfoAsList();

            @Override
            public List<Item> getNodeItems() {
                return null;
            }

            @Override
            public List<String> getNodeFeatures() {
                return features;
            }

            @Override
            public List<Identity> getNodeIdentities() {
                return identities;
            }

            @Override
            public List<PacketExtension> getNodePacketExtensions() {
                return packetExtensions;
            }
        });

        // Send an empty presence, and let the packet intercepter
        // add a <c/> node to it.
        // See http://xmpp.org/extensions/xep-0115.html#advertise
        // We only send a presence packet if there was already one send
        // to respect ConnectionConfiguration.isSendPresence()
        if (connection != null && connection.isAuthenticated() && presenceSend) {
            Presence presence = new Presence(Presence.Type.available);
            connection.sendPacket(presence);
        }
    }

    /**
     * Verify DisoverInfo and Caps Node as defined in XEP-0115 5.4 Processing
     * Method
     * 
     * @see <a href="http://xmpp.org/extensions/xep-0115.html#ver-proc">XEP-0115
     *      5.4 Processing Method</a>
     * 
     * @param capsNode
     *            the caps node (i.e. node#ver)
     * @param info
     * @return true if it's valid and should be cache, false if not
     */
    public static boolean verifyDiscoverInfoVersion(String ver, String hash, DiscoverInfo info) {
        // step 3.3 check for duplicate identities
        if (info.containsDuplicateIdentities())
            return false;

        // step 3.4 check for duplicate features
        if (info.containsDuplicateFeatures())
            return false;

        // step 3.5 check for well-formed packet extensions
        if (verifyPacketExtensions(info))
            return false;

        String calculatedVer = generateVerificationString(info, hash);

        if (!ver.equals(calculatedVer))
            return false;

        return true;
    }

    /**
     * 
     * @param info
     * @return true if the packet extensions is ill-formed
     */
    protected static boolean verifyPacketExtensions(DiscoverInfo info) {
        List<FormField> foundFormTypes = new LinkedList<FormField>();
        for (Iterator<PacketExtension> i = info.getExtensions().iterator(); i.hasNext();) {
            PacketExtension pe = i.next();
            if (pe.getNamespace().equals(Form.NAMESPACE)) {
                DataForm df = (DataForm) pe;
                for (Iterator<FormField> it = df.getFields(); it.hasNext();) {
                    FormField f = it.next();
                    if (f.getVariable().equals("FORM_TYPE")) {
                        for (FormField fft : foundFormTypes) {
                            if (f.equals(fft))
                                return true;
                        }
                        foundFormTypes.add(f);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Generates a XEP-115 Verification String
     * 
     * @see <a href="http://xmpp.org/extensions/xep-0115.html#ver">XEP-115
     *      Verification String</a>
     * 
     * @param discoverInfo
     * @param hash
     *            the used hash function
     * @return The generated verification String or null if the hash is not
     *         supported
     */
    protected static String generateVerificationString(DiscoverInfo discoverInfo, String hash) {
        MessageDigest md = SUPPORTED_HASHES.get(hash.toLowerCase());
        if (md == null)
            return null;

        DataForm extendedInfo = (DataForm) discoverInfo.getExtension(Form.ELEMENT, Form.NAMESPACE);

        // 1. Initialize an empty string S ('sb' in this method).
        StringBuilder sb = new StringBuilder(); // Use StringBuilder as we don't
                                                // need thread-safe StringBuffer

        // 2. Sort the service discovery identities by category and then by
        // type and then by xml:lang
        // (if it exists), formatted as CATEGORY '/' [TYPE] '/' [LANG] '/'
        // [NAME]. Note that each slash is included even if the LANG or
        // NAME is not included (in accordance with XEP-0030, the category and
        // type MUST be included.
        SortedSet<DiscoverInfo.Identity> sortedIdentities = new TreeSet<DiscoverInfo.Identity>();

        for (Iterator<DiscoverInfo.Identity> it = discoverInfo.getIdentities(); it.hasNext();)
            sortedIdentities.add(it.next());

        // 3. For each identity, append the 'category/type/lang/name' to S,
        // followed by the '<' character.
        for (Iterator<DiscoverInfo.Identity> it = sortedIdentities.iterator(); it.hasNext();) {
            DiscoverInfo.Identity identity = it.next();
            sb.append(identity.getCategory());
            sb.append("/");
            sb.append(identity.getType());
            sb.append("/");
            sb.append(identity.getLanguage() == null ? "" : identity.getLanguage());
            sb.append("/");
            sb.append(identity.getName() == null ? "" : identity.getName());
            sb.append("<");
        }

        // 4. Sort the supported service discovery features.
        SortedSet<String> features = new TreeSet<String>();
        for (Iterator<Feature> it = discoverInfo.getFeatures(); it.hasNext();)
            features.add(it.next().getVar());

        // 5. For each feature, append the feature to S, followed by the '<'
        // character
        for (String f : features) {
            sb.append(f);
            sb.append("<");
        }

        // only use the data form for calculation is it has a hidden FORM_TYPE
        // field
        // see XEP-0115 5.4 step 3.6
        if (extendedInfo != null && extendedInfo.hasHiddenFormTypeField()) {
            synchronized (extendedInfo) {
                // 6. If the service discovery information response includes
                // XEP-0128 data forms, sort the forms by the FORM_TYPE (i.e.,
                // by the XML character data of the <value/> element).
                SortedSet<FormField> fs = new TreeSet<FormField>(new Comparator<FormField>() {
                    public int compare(FormField f1, FormField f2) {
                        return f1.getVariable().compareTo(f2.getVariable());
                    }
                });

                FormField ft = null;

                for (Iterator<FormField> i = extendedInfo.getFields(); i.hasNext();) {
                    FormField f = i.next();
                    if (!f.getVariable().equals("FORM_TYPE")) {
                        fs.add(f);
                    } else {
                        ft = f;
                    }
                }

                // Add FORM_TYPE values
                if (ft != null) {
                    formFieldValuesToCaps(ft.getValues(), sb);
                }

                // 7. 3. For each field other than FORM_TYPE:
                // 1. Append the value of the "var" attribute, followed by the
                // '<' character.
                // 2. Sort values by the XML character data of the <value/>
                // element.
                // 3. For each <value/> element, append the XML character data,
                // followed by the '<' character.
                for (FormField f : fs) {
                    sb.append(f.getVariable());
                    sb.append("<");
                    formFieldValuesToCaps(f.getValues(), sb);
                }
            }
        }
        // 8. Ensure that S is encoded according to the UTF-8 encoding (RFC
        // 3269).
        // 9. Compute the verification string by hashing S using the algorithm
        // specified in the 'hash' attribute (e.g., SHA-1 as defined in RFC
        // 3174).
        // The hashed data MUST be generated with binary output and
        // encoded using Base64 as specified in Section 4 of RFC 4648
        // (note: the Base64 output MUST NOT include whitespace and MUST set
        // padding bits to zero).
        byte[] digest = md.digest(sb.toString().getBytes());
        return Base64.encodeBytes(digest);
    }

    private static void formFieldValuesToCaps(Iterator<String> i, StringBuilder sb) {
        SortedSet<String> fvs = new TreeSet<String>();
        while (i.hasNext()) {
            fvs.add(i.next());
        }
        for (String fv : fvs) {
            sb.append(fv);
            sb.append("<");
        }
    }

    public static class NodeVerHash {
        private String node;
        private String hash;
        private String ver;
        private String nodeVer;

        NodeVerHash(String node, String ver, String hash) {
            this.node = node;
            this.ver = ver;
            this.hash = hash;
            nodeVer = node + "#" + ver;
        }

        public String getNodeVer() {
            return nodeVer;
        }

        public String getNode() {
            return node;
        }

        public String getHash() {
            return hash;
        }

        public String getVer() {
            return ver;
        }
    }
}
