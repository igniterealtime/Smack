/**
 *
 * Copyright 2014 Georg Lukas, 2021 Florian Schmaus.
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
package org.jivesoftware.smackx.iqversion;

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.Smack;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError.Condition;
import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.iqversion.packet.Version;

import org.jxmpp.jid.Jid;

/**
 * A Version Manager that automatically responds to version IQs with a predetermined reply.
 *
 * <p>
 * The VersionManager takes care of handling incoming version request IQs, according to
 * XEP-0092 (Software Version). You can configure the version reply for a given connection
 * by running the following code:
 * </p>
 *
 * <pre>
 * Version MY_VERSION = new Version("My Little XMPP Application", "v1.23", "OS/2 32-bit");
 * VersionManager.getInstanceFor(mConnection).setVersion(MY_VERSION);
 * </pre>
 *
 * @author Georg Lukas
 */
public final class VersionManager extends Manager {
    private static final Map<XMPPConnection, VersionManager> INSTANCES = new WeakHashMap<>();

    private static VersionInformation defaultVersion;

    private VersionInformation ourVersion = defaultVersion;

    public static void setDefaultVersion(String name, String version) {
        setDefaultVersion(name, version, null);
    }

    public static void setDefaultVersion(String name, String version, String os) {
        defaultVersion = generateVersionFrom(name, version, os);
    }

    private static boolean autoAppendSmackVersion = true;

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                VersionManager.getInstanceFor(connection);
            }
        });
    }

    private VersionManager(final XMPPConnection connection) {
        super(connection);

        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(Version.NAMESPACE);

        connection.registerIQRequestHandler(new AbstractIqRequestHandler(Version.ELEMENT, Version.NAMESPACE, IQ.Type.get,
                        Mode.async) {
            @Override
            public IQ handleIQRequest(IQ iqRequest) {
                if (ourVersion == null) {
                    return IQ.createErrorResponse(iqRequest, Condition.not_acceptable);
                }

                Version versionRequest = (Version) iqRequest;
                Version versionResponse = Version.builder(versionRequest)
                                .setName(ourVersion.name)
                                .setVersion(ourVersion.version)
                                .setOs(ourVersion.os)
                                .build();
                return versionResponse;
            }
        });
    }

    public static synchronized VersionManager getInstanceFor(XMPPConnection connection) {
        VersionManager versionManager = INSTANCES.get(connection);

        if (versionManager == null) {
            versionManager = new VersionManager(connection);
            INSTANCES.put(connection, versionManager);
        }

        return versionManager;
    }

    public static void setAutoAppendSmackVersion(boolean autoAppendSmackVersion) {
        VersionManager.autoAppendSmackVersion = autoAppendSmackVersion;
    }

    public void setVersion(String name, String version) {
        setVersion(name, version, null);
    }

    public void setVersion(String name, String version, String os) {
        ourVersion = generateVersionFrom(name, version, os);
    }

    public void unsetVersion() {
        ourVersion = null;
    }

    public boolean isSupported(Jid jid) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(jid,
                        Version.NAMESPACE);
    }

    /**
     * Request version information from a given JID.
     *
     * @param jid TODO javadoc me please
     * @return the version information or {@code null} if not supported by JID
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public Version getVersion(Jid jid) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException {
        if (!isSupported(jid)) {
            return null;
        }
        XMPPConnection connection = connection();
        Version version = Version.builder(connection).to(jid).build();
        return connection().sendIqRequestAndWaitForResponse(version);
    }

    private static VersionInformation generateVersionFrom(String name, String version, String os) {
        if (autoAppendSmackVersion) {
            name += " (Smack " + Smack.getVersion() + ')';
        }
        return new VersionInformation(name, version, os);
    }

    private static final class VersionInformation {
        private final String name;
        private final String version;
        private final String os;

        private VersionInformation(String name, String version, String os) {
            this.name = StringUtils.requireNotNullNorEmpty(name, "Must provide a name");
            this.version = StringUtils.requireNotNullNorEmpty(version, "Must provide a version");
            this.os = os;
        }
    }
}
