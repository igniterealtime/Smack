/**
 *
 * Copyright 2017-2022 Eng Chong Meng
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
package org.jivesoftware.smackx.externalservicediscovery;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jxmpp.jid.DomainBareJid;
import org.threeten.bp.Instant;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A manager for XEP-0215: External Service Discovery.
 *
 * @author Eng Chong Meng
 * @see <a href="https://xmpp.org/extensions/xep-0215.html">XEP-0215: External Service Discovery</a>
 */
public final class ExternalServiceDiscoveryManager extends Manager {
    private static final Logger LOGGER = Logger.getLogger(ExternalServiceDiscoveryManager.class.getName());

    // Create a new ExternalServiceDiscoveryManager on every established connection
    static {
        XMPPConnectionRegistry.addConnectionCreationListener(ExternalServiceDiscoveryManager::getInstanceFor);
    }

    private static final String TURN_SRV_NAME = "turn";
    private static final Map<XMPPConnection, ExternalServiceDiscoveryManager> INSTANCES = new WeakHashMap<>();

    private boolean hasExtService = false;
    private String mServiceExpire = null; // expires='2022-10-30T12:26:55Z'
    private List<ServiceElement> mExtServices = null;

    /**
     * Obtain the ExternalServiceDiscoveryManager responsible for a connection.
     *
     * @param connection the connection object.
     * @return a ExternalServiceDiscoveryManager instance
     */
    public static synchronized ExternalServiceDiscoveryManager getInstanceFor(XMPPConnection connection) {
        ExternalServiceDiscoveryManager extServiceManager = INSTANCES.get(connection);

        if (extServiceManager == null) {
            extServiceManager = new ExternalServiceDiscoveryManager(connection);
            INSTANCES.put(connection, extServiceManager);
        }
        return extServiceManager;
    }

    private ExternalServiceDiscoveryManager(XMPPConnection connection) {
        super(connection);
        AbstractIqRequestHandler iqRequestHandler
                = new AbstractIqRequestHandler(ExternalServices.ELEMENT, ExternalServices.NAMESPACE, IQ.Type.set, IQRequestHandler.Mode.async) {
            @Override
            public IQ handleIQRequest(IQ iqRequest) {
                if (iqRequest instanceof ExternalServiceDiscovery) {
                    handleESDServicePush(((ExternalServiceDiscovery) iqRequest).getServices());
                    return IQ.createResultIQ(iqRequest);
                }
                return IQ.createErrorResponse(iqRequest, StanzaError.Condition.feature_not_implemented);
            }
        };

        connection.addConnectionListener(new ConnectionListener() {
            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                // No need to reset the cache values if the connection is resumed.
                if (resumed) {
                    return;
                }

                // Listen for External Service Discovery IQ Push
                connection.registerIQRequestHandler(iqRequestHandler);

                try {
                    if (discoverExtServices()) {
                        getExtServices();
                    }
                } catch (XMPPErrorException | SmackException.NotConnectedException
                        | SmackException.NoResponseException | InterruptedException e) {
                    LOGGER.log(Level.WARNING, "Error during discovering XEP-0215 external service", e);
                }
            }

            @Override
            public void connectionClosed() {
                connection.unregisterIQRequestHandler(iqRequestHandler);
                mExtServices = null;
            }
        });
    }

    /**
     * Discover external service.
     * Called automatically when connection is authenticated.
     * Note that this is a synchronous call -- Smack must wait for the server response.
     *
     * @return true if external service was discovered
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws SmackException.NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackException.NoResponseException if there was no response from the remote entity.
     */
    public boolean discoverExtServices() throws XMPPErrorException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection());
        List<DiscoverInfo> servicesDiscoverInfo = sdm.findServicesDiscoverInfo(ExternalServices.NAMESPACE, true, true);
        hasExtService = !servicesDiscoverInfo.isEmpty();
        return hasExtService;
    }

    /**
     * Check if ExternalServices was discovered.
     *
     * @return true if ExternalServices was discovered
     */
    public boolean hasExtService() {
        return hasExtService;
    }

    private void handleESDServicePush(List<ServiceElement> services) {
        if (mExtServices == null || services == null || services.isEmpty()) {
            // Force reload until handleServicePush is fully tested
            // mServiceExpire = null;
            return;
        }

        for (ServiceElement service : services) {
            String action = service.getAction();
            service.removeAttribute(ServiceElement.ATTR_ACTION);
            // Timber.d("Ext services: %s", service.toXML());

            switch (action) {
                case "add":
                    mExtServices.add(service);
                    break;

                case "delete":
                case "modified":
                    // mExtServices.removeIf(x -> x.serviceEquals(service));
                    ListIterator<ServiceElement> iter = mExtServices.listIterator();
                    while (iter.hasNext()) {
                        if (iter.next().serviceEquals(service)) {
                            iter.remove();
                        }
                    }

                    if ("modified".equals(action))
                        mExtServices.add(service);
                    break;
            }
        }
    }

    /**
     * Get the discovered services of a current XMPP connection (i.e. serviceName).
     * Saved a copy of the service and its expires time if specified.
     */
    private void getExtServices() {
        DomainBareJid serviceName = connection().getXMPPServiceDomain();
        ExternalServiceDiscovery extServiceDisco = new ExternalServiceDiscovery();
        extServiceDisco.setType(IQ.Type.get);
        extServiceDisco.setTo(serviceName);

        // Discover the entity's external services
        try {
            IQ iq = connection().sendIqRequestAndWaitForResponse(extServiceDisco);
            String expireTE = null;
            if (iq instanceof ExternalServiceDiscovery) {
                List<ServiceElement> services = ((ExternalServiceDiscovery) iq).getServices();
                if (services != null && !services.isEmpty()) {
                    mExtServices = services;

                    // get the service expired time if available
                    for (ServiceElement service : services) {
                        if (TURN_SRV_NAME.equals(service.getType())) {
                            String expireT = service.getExpires();
                            if (service.getExpires() != null) {
                                if (expireTE == null) {
                                    expireTE = expireT;
                                } else if (expireTE.compareTo(expireT) > 0) {
                                    expireTE = expireT;
                                }
                            }
                        }
                    }
                }

                if (expireTE != null) {
                    expireTE = Instant.parse(expireTE).toString();
                } else {
                    expireTE = Instant.now()
                            .plus(3, ChronoUnit.MONTHS)
                            .toString();
                }
            }
            mServiceExpire = expireTE;
        } catch (SmackException.NotConnectedException | InterruptedException | SmackException.NoResponseException | XMPPException.XMPPErrorException e) {
            LOGGER.log(Level.SEVERE, "Could not get external services: " + e, e);
        }
    }

    public List<ServiceElement> getTransportServices(String transport) {
        // Check time expired and reload if necessary
        // Timber.d("UTC now / expire: %s / %s", Instant.now().toString(), (expire == null) ? null : Instant.parse(expire).toString());
        if (mServiceExpire == null || Instant.now().truncatedTo(ChronoUnit.SECONDS).isAfter(Instant.parse(mServiceExpire))) {
            mExtServices = null;
            getExtServices();
        }

        if (mExtServices == null)
            return null;

        if (transport == null) {
            return mExtServices;
        }

        List<ServiceElement> services = new ArrayList<>();
        for (ServiceElement service : mExtServices) {
            if (transport.equals(service.getTransport())) {
                services.add(service);
            }
        }
        return services;
    }
}
