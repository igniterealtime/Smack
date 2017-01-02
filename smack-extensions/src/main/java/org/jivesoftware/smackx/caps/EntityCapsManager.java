/**
 *
 * Copyright © 2009 Jonas Ådahl, 2011-2014 Florian Schmaus
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
package org.jivesoftware.smackx.caps;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.AbstractPresenceEventListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.filter.PresenceTypeFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.caps.cache.EntityCapsPersistentCache;
import org.jivesoftware.smackx.caps.packet.CapsExtension;
import org.jivesoftware.smackx.disco.AbstractNodeInformationProvider;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo.Feature;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.util.cache.LruCache;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Keeps track of entity capabilities.
 * 
 * @author Florian Schmaus
 * @see <a href="http://www.xmpp.org/extensions/xep-0115.html">XEP-0115: Entity Capabilities</a>
 */
public final class EntityCapsManager extends Manager {
    private static final Logger LOGGER = Logger.getLogger(EntityCapsManager.class.getName());

    public static final String NAMESPACE = CapsExtension.NAMESPACE;
    public static final String ELEMENT = CapsExtension.ELEMENT;

    private static final Map<String, MessageDigest> SUPPORTED_HASHES = new HashMap<String, MessageDigest>();

    /**
     * The default hash. Currently 'sha-1'.
     */
    private static final String DEFAULT_HASH = StringUtils.SHA1;

    private static String DEFAULT_ENTITY_NODE = "http://www.igniterealtime.org/projects/smack";

    protected static EntityCapsPersistentCache persistentCache;

    private static boolean autoEnableEntityCaps = true;

    private static Map<XMPPConnection, EntityCapsManager> instances = new WeakHashMap<>();

    private static final StanzaFilter PRESENCES_WITH_CAPS = new AndFilter(new StanzaTypeFilter(Presence.class), new StanzaExtensionFilter(
                    ELEMENT, NAMESPACE));

    /**
     * Map of "node + '#' + hash" to DiscoverInfo data
     */
    static final LruCache<String, DiscoverInfo> CAPS_CACHE = new LruCache<String, DiscoverInfo>(1000);

    /**
     * Map of Full JID -&gt; DiscoverInfo/null. In case of c2s connection the
     * key is formed as user@server/resource (resource is required) In case of
     * link-local connection the key is formed as user@host (no resource) In
     * case of a server or component the key is formed as domain
     */
    static final LruCache<Jid, NodeVerHash> JID_TO_NODEVER_CACHE = new LruCache<>(10000);

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });

        try {
            MessageDigest sha1MessageDigest = MessageDigest.getInstance(DEFAULT_HASH);
            SUPPORTED_HASHES.put(DEFAULT_HASH, sha1MessageDigest);
        } catch (NoSuchAlgorithmException e) {
            // Ignore
        }
    }

    /**
     * Set the default entity node that will be used for new EntityCapsManagers.
     *
     * @param entityNode
     */
    public static void setDefaultEntityNode(String entityNode) {
        DEFAULT_ENTITY_NODE = entityNode;
    }

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
        CAPS_CACHE.put(nodeVer, info);

        if (persistentCache != null)
            persistentCache.addDiscoverInfoByNodePersistent(nodeVer, info);
    }

    /**
     * Get the Node version (node#ver) of a JID. Returns a String or null if
     * EntiyCapsManager does not have any information.
     * 
     * @param jid
     *            the user (Full JID)
     * @return the node version (node#ver) or null
     */
    public static String getNodeVersionByJid(Jid jid) {
        NodeVerHash nvh = JID_TO_NODEVER_CACHE.lookup(jid);
        if (nvh != null) {
            return nvh.nodeVer;
        } else {
            return null;
        }
    }

    public static NodeVerHash getNodeVerHashByJid(Jid jid) {
        return JID_TO_NODEVER_CACHE.lookup(jid);
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
    public static DiscoverInfo getDiscoverInfoByUser(Jid user) {
        NodeVerHash nvh = JID_TO_NODEVER_CACHE.lookup(user);
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
        DiscoverInfo info = CAPS_CACHE.lookup(nodeVer);

        // If it was not in CAPS_CACHE, try to retrieve the information from persistentCache
        if (info == null && persistentCache != null) {
            info = persistentCache.lookup(nodeVer);
            // Promote the information to CAPS_CACHE if one was found
            if (info != null) {
                CAPS_CACHE.put(nodeVer, info);
            }
        }

        // If we were able to retrieve information from one of the caches, copy it before returning
        if (info != null)
            info = new DiscoverInfo(info);

        return info;
    }

    /**
     * Set the persistent cache implementation.
     * 
     * @param cache
     */
    public static void setPersistentCache(EntityCapsPersistentCache cache) {
        persistentCache = cache;
    }

    /**
     * Sets the maximum cache sizes.
     *
     * @param maxJidToNodeVerSize
     * @param maxCapsCacheSize
     */
    public static void setMaxsCacheSizes(int maxJidToNodeVerSize, int maxCapsCacheSize) {
        JID_TO_NODEVER_CACHE.setMaxCacheSize(maxJidToNodeVerSize);
        CAPS_CACHE.setMaxCacheSize(maxCapsCacheSize);
    }

    /**
     * Clears the memory cache.
     */
    public static void clearMemoryCache() {
        JID_TO_NODEVER_CACHE.clear();
        CAPS_CACHE.clear();
    }

    private static void addCapsExtensionInfo(Jid from, CapsExtension capsExtension) {
        String capsExtensionHash = capsExtension.getHash();
        String hashInUppercase = capsExtensionHash.toUpperCase(Locale.US);
        // SUPPORTED_HASHES uses the format of MessageDigest, which is uppercase, e.g. "SHA-1" instead of "sha-1"
        if (!SUPPORTED_HASHES.containsKey(hashInUppercase))
            return;
        String hash = capsExtensionHash.toLowerCase(Locale.US);

        String node = capsExtension.getNode();
        String ver = capsExtension.getVer();

        JID_TO_NODEVER_CACHE.put(from, new NodeVerHash(node, ver, hash));
    }

    private final Queue<CapsVersionAndHash> lastLocalCapsVersions = new ConcurrentLinkedQueue<>();

    private final ServiceDiscoveryManager sdm;

    private boolean entityCapsEnabled;
    private CapsVersionAndHash currentCapsVersion;
    private volatile Presence presenceSend;

    /**
     * The entity node String used by this EntityCapsManager instance.
     */
    private String entityNode = DEFAULT_ENTITY_NODE;

    private EntityCapsManager(XMPPConnection connection) {
        super(connection);
        this.sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        instances.put(connection, this);

        connection.addConnectionListener(new AbstractConnectionListener() {
            @Override
            public void connected(XMPPConnection connection) {
                // It's not clear when a server would report the caps stream
                // feature, so we try to process it after we are connected and
                // once after we are authenticated.
                processCapsStreamFeatureIfAvailable(connection);
            }
            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                // It's not clear when a server would report the caps stream
                // feature, so we try to process it after we are connected and
                // once after we are authenticated.
                processCapsStreamFeatureIfAvailable(connection);

                // Reset presenceSend when the connection was not resumed
                if (!resumed) {
                    presenceSend = null;
                }
            }
            private void processCapsStreamFeatureIfAvailable(XMPPConnection connection) {
                CapsExtension capsExtension = connection.getFeature(
                                CapsExtension.ELEMENT, CapsExtension.NAMESPACE);
                if (capsExtension == null) {
                    return;
                }
                DomainBareJid from = connection.getXMPPServiceDomain();
                addCapsExtensionInfo(from, capsExtension);
            }
        });

        // This calculates the local entity caps version
        updateLocalEntityCaps();

        if (autoEnableEntityCaps)
            enableEntityCaps();

        connection.addAsyncStanzaListener(new StanzaListener() {
            // Listen for remote presence stanzas with the caps extension
            // If we receive such a stanza, record the JID and nodeVer
            @Override
            public void processPacket(Stanza packet) {
                if (!entityCapsEnabled())
                    return;

                CapsExtension capsExtension = CapsExtension.from(packet);
                Jid from = packet.getFrom();
                addCapsExtensionInfo(from, capsExtension);
            }

        }, PRESENCES_WITH_CAPS);

        Roster.getInstanceFor(connection).addPresenceEventListener(new AbstractPresenceEventListener() {
            @Override
            public void presenceUnavailable(FullJid from, Presence presence) {
                JID_TO_NODEVER_CACHE.remove(from);
            }
        });

        connection.addPacketSendingListener(new StanzaListener() {
            @Override
            public void processPacket(Stanza packet) {
                presenceSend = (Presence) packet;
            }
        }, PresenceTypeFilter.OUTGOING_PRESENCE_BROADCAST);

        // Intercept presence packages and add caps data when intended.
        // XEP-0115 specifies that a client SHOULD include entity capabilities
        // with every presence notification it sends.
        StanzaListener packetInterceptor = new StanzaListener() {
            public void processPacket(Stanza packet) {
                if (!entityCapsEnabled) {
                    // Be sure to not send stanzas with the caps extension if it's not enabled
                    packet.removeExtension(CapsExtension.ELEMENT, CapsExtension.NAMESPACE);
                    return;
                }
                CapsVersionAndHash capsVersionAndHash = getCapsVersionAndHash();
                CapsExtension caps = new CapsExtension(entityNode, capsVersionAndHash.version, capsVersionAndHash.hash);
                packet.overrideExtension(caps);
            }
        };
        connection.addPacketInterceptor(packetInterceptor, PresenceTypeFilter.AVAILABLE);
        // It's important to do this as last action. Since it changes the
        // behavior of the SDM in some ways
        sdm.setEntityCapsManager(this);
    }

    public static synchronized EntityCapsManager getInstanceFor(XMPPConnection connection) {
        if (SUPPORTED_HASHES.size() <= 0)
            throw new IllegalStateException("No supported hashes for EntityCapsManager");

        EntityCapsManager entityCapsManager = instances.get(connection);

        if (entityCapsManager == null) {
            entityCapsManager = new EntityCapsManager(connection);
        }

        return entityCapsManager;
    }

    public synchronized void enableEntityCaps() {
        // Add Entity Capabilities (XEP-0115) feature node.
        sdm.addFeature(NAMESPACE);
        updateLocalEntityCaps();
        entityCapsEnabled = true;
    }

    public synchronized void disableEntityCaps() {
        entityCapsEnabled = false;
        sdm.removeFeature(NAMESPACE);
    }

    public boolean entityCapsEnabled() {
        return entityCapsEnabled;
    }

    public void setEntityNode(String entityNode) {
        this.entityNode = entityNode;
        updateLocalEntityCaps();
    }

    /**
     * Remove a record telling what entity caps node a user has.
     * 
     * @param user
     *            the user (Full JID)
     */
    public static void removeUserCapsNode(String user) {
        JID_TO_NODEVER_CACHE.remove(user);
    }

    /**
     * Get our own caps version. The version depends on the enabled features. A
     * caps version looks like '66/0NaeaBKkwk85efJTGmU47vXI='
     * 
     * @return our own caps version
     */
    public CapsVersionAndHash getCapsVersionAndHash() {
        return currentCapsVersion;
    }

    /**
     * Returns the local entity's NodeVer (e.g.
     * "http://www.igniterealtime.org/projects/smack/#66/0NaeaBKkwk85efJTGmU47vXI=
     * )
     * 
     * @return the local NodeVer
     */
    public String getLocalNodeVer() {
        CapsVersionAndHash capsVersionAndHash = getCapsVersionAndHash();
        if (capsVersionAndHash == null) {
            return null;
        }
        return entityNode + '#' + capsVersionAndHash.version;
    }

    /**
     * Returns true if Entity Caps are supported by a given JID.
     * 
     * @param jid
     * @return true if the entity supports Entity Capabilities.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public boolean areEntityCapsSupported(Jid jid) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return sdm.supportsFeature(jid, NAMESPACE);
    }

    /**
     * Returns true if Entity Caps are supported by the local service/server.
     * 
     * @return true if the user's server supports Entity Capabilities.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public boolean areEntityCapsSupportedByServer() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        return areEntityCapsSupported(connection().getXMPPServiceDomain());
    }

    /**
     * Updates the local user Entity Caps information with the data provided
     *
     * If we are connected and there was already a presence send, another
     * presence is send to inform others about your new Entity Caps node string.
     *
     */
    public void updateLocalEntityCaps() {
        XMPPConnection connection = connection();

        DiscoverInfo discoverInfo = new DiscoverInfo();
        discoverInfo.setType(IQ.Type.result);
        sdm.addDiscoverInfoTo(discoverInfo);

        // getLocalNodeVer() will return a result only after currentCapsVersion is set. Therefore
        // set it first and then call getLocalNodeVer()
        currentCapsVersion = generateVerificationString(discoverInfo);
        final String localNodeVer = getLocalNodeVer();
        discoverInfo.setNode(localNodeVer);
        addDiscoverInfoByNode(localNodeVer, discoverInfo);
        if (lastLocalCapsVersions.size() > 10) {
            CapsVersionAndHash oldCapsVersion = lastLocalCapsVersions.poll();
            sdm.removeNodeInformationProvider(entityNode + '#' + oldCapsVersion.version);
        }
        lastLocalCapsVersions.add(currentCapsVersion);

        if (connection != null)
            JID_TO_NODEVER_CACHE.put(connection.getUser(), new NodeVerHash(entityNode, currentCapsVersion));

        final List<Identity> identities = new LinkedList<Identity>(ServiceDiscoveryManager.getInstanceFor(connection).getIdentities());
        sdm.setNodeInformationProvider(localNodeVer, new AbstractNodeInformationProvider() {
            List<String> features = sdm.getFeatures();
            List<ExtensionElement> packetExtensions = sdm.getExtendedInfoAsList();
            @Override
            public List<String> getNodeFeatures() {
                return features;
            }
            @Override
            public List<Identity> getNodeIdentities() {
                return identities;
            }
            @Override
            public List<ExtensionElement> getNodePacketExtensions() {
                return packetExtensions;
            }
        });

        // Re-send the last sent presence, and let the stanza interceptor
        // add a <c/> node to it.
        // See http://xmpp.org/extensions/xep-0115.html#advertise
        // We only send a presence packet if there was already one send
        // to respect ConnectionConfiguration.isSendPresence()
        if (connection != null && connection.isAuthenticated() && presenceSend != null) {
            try {
                connection.sendStanza(presenceSend.cloneWithNewId());
            }
            catch (InterruptedException | NotConnectedException e) {
                LOGGER.log(Level.WARNING, "Could could not update presence with caps info", e);
            }
        }
    }

    /**
     * Verify DisoverInfo and Caps Node as defined in XEP-0115 5.4 Processing
     * Method.
     * 
     * @see <a href="http://xmpp.org/extensions/xep-0115.html#ver-proc">XEP-0115
     *      5.4 Processing Method</a>
     * 
     * @param ver
     * @param hash
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

        String calculatedVer = generateVerificationString(info, hash).version;

        if (!ver.equals(calculatedVer))
            return false;

        return true;
    }

    /**
     * 
     * @param info
     * @return true if the stanza(/packet) extensions is ill-formed
     */
    protected static boolean verifyPacketExtensions(DiscoverInfo info) {
        List<FormField> foundFormTypes = new LinkedList<FormField>();
        for (ExtensionElement pe : info.getExtensions()) {
            if (pe.getNamespace().equals(DataForm.NAMESPACE)) {
                DataForm df = (DataForm) pe;
                for (FormField f : df.getFields()) {
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

    protected static CapsVersionAndHash generateVerificationString(DiscoverInfo discoverInfo) {
        return generateVerificationString(discoverInfo, null);
    }

    /**
     * Generates a XEP-115 Verification String
     * 
     * @see <a href="http://xmpp.org/extensions/xep-0115.html#ver">XEP-115
     *      Verification String</a>
     * 
     * @param discoverInfo
     * @param hash
     *            the used hash function, if null, default hash will be used
     * @return The generated verification String or null if the hash is not
     *         supported
     */
    protected static CapsVersionAndHash generateVerificationString(DiscoverInfo discoverInfo, String hash) {
        if (hash == null) {
            hash = DEFAULT_HASH;
        }
        // SUPPORTED_HASHES uses the format of MessageDigest, which is uppercase, e.g. "SHA-1" instead of "sha-1"
        MessageDigest md = SUPPORTED_HASHES.get(hash.toUpperCase(Locale.US));
        if (md == null)
            return null;
        // Then transform the hash to lowercase, as this value will be put on the wire within the caps element's hash
        // attribute. I'm not sure if the standard is case insensitive here, but let's assume that even it is, there could
        // be "broken" implementation in the wild, so we *always* transform to lowercase.
        hash = hash.toLowerCase(Locale.US);

        DataForm extendedInfo =  DataForm.from(discoverInfo);

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

        for (DiscoverInfo.Identity i : discoverInfo.getIdentities())
            sortedIdentities.add(i);

        // 3. For each identity, append the 'category/type/lang/name' to S,
        // followed by the '<' character.
        for (DiscoverInfo.Identity identity : sortedIdentities) {
            sb.append(identity.getCategory());
            sb.append('/');
            sb.append(identity.getType());
            sb.append('/');
            sb.append(identity.getLanguage() == null ? "" : identity.getLanguage());
            sb.append('/');
            sb.append(identity.getName() == null ? "" : identity.getName());
            sb.append('<');
        }

        // 4. Sort the supported service discovery features.
        SortedSet<String> features = new TreeSet<String>();
        for (Feature f : discoverInfo.getFeatures())
            features.add(f.getVar());

        // 5. For each feature, append the feature to S, followed by the '<'
        // character
        for (String f : features) {
            sb.append(f);
            sb.append('<');
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

                for (FormField f : extendedInfo.getFields()) {
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
                    sb.append('<');
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
        byte[] digest;
        synchronized(md) {
            digest = md.digest(sb.toString().getBytes());
        }
        String version = Base64.encodeToString(digest);
        return new CapsVersionAndHash(version, hash);
    }

    private static void formFieldValuesToCaps(List<String> i, StringBuilder sb) {
        SortedSet<String> fvs = new TreeSet<String>();
        for (String s : i) {
            fvs.add(s);
        }
        for (String fv : fvs) {
            sb.append(fv);
            sb.append('<');
        }
    }

    public static class NodeVerHash {
        private String node;
        private String hash;
        private String ver;
        private String nodeVer;

        NodeVerHash(String node, CapsVersionAndHash capsVersionAndHash) {
            this(node, capsVersionAndHash.version, capsVersionAndHash.hash);
        }

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
