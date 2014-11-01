/**
 *
 * Copyright 2009 Jonas Ådahl.
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

package org.jivesoftware.smack.serverless;


import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.Tuple;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.impl.JmDNSImpl;

import java.net.InetAddress;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

/**
 * Implements a LLService using JmDNS.
 *
 * @author Jonas Ådahl
 */
public class JmDNSService extends LLService implements ServiceListener {
    static JmDNS jmdns = null;
    private ServiceInfo serviceInfo;
    static final String SERVICE_TYPE = "_presence._tcp.local.";

    private JmDNSService(LLPresence presence, LLPresenceDiscoverer presenceDiscoverer) {
        super(presence, presenceDiscoverer);
    }

    /**
     * Instantiate a new JmDNSService and start to listen for connections.
     *
     * @param presence the mDNS presence information that should be used.
     */
    public static LLService create(LLPresence presence) throws XMPPException {
        return create(presence, null);
    }

    /**
     * Instantiate a new JmDNSService and start to listen for connections.
     *
     * @param presence the mDNS presence information that should be used.
     * @param addr the INET Address to use.
     */
    public static LLService create(LLPresence presence, InetAddress addr) throws XMPPException {
        // Start the JmDNS daemon.
        initJmDNS(addr);

        // Start the presence discoverer
        JmDNSPresenceDiscoverer presenceDiscoverer = new JmDNSPresenceDiscoverer();

        // Start the presence service
        JmDNSService service = new JmDNSService(presence, presenceDiscoverer);

        return service;
    }

    @Override
    public void close() throws IOException {
        super.close();
        jmdns.close();
    }

    /**
     * Start the JmDNS daemon.
     */
    private static void initJmDNS(InetAddress addr) throws XMPPException {
        try {
            if (jmdns == null) {
                if (addr == null) {
                    jmdns = JmDNS.create();
                }
                else {
                    jmdns = JmDNS.create(addr);
                }
            }
        }
        catch (IOException ioe) {
            throw new XMPPException.XMPPErrorException("Failed to create a JmDNS instance", new XMPPError(XMPPError.Condition.undefined_condition), ioe);
        }
    }

    protected void updateText() {
        Hashtable<String,String> ht = new Hashtable<String,String>();
        
        for (Tuple<String,String> t : presence.toList()) {
            if (t.a != null && t.b != null) {
                ht.put(t.a, t.b);
            }
        }

        serviceInfo.setText(ht);
    }

    /**
     * Register the DNS-SD service with the daemon.
     */
    protected void registerService() throws XMPPException {
        Hashtable<String,String> ht = new Hashtable<String,String>();
        
        for (Tuple<String,String> t : presence.toList()) {
            if (t.a != null && t.b != null)
                ht.put(t.a, t.b);
        }
        serviceInfo = ServiceInfo.create(SERVICE_TYPE,
                presence.getServiceName(), presence.getPort(), 0, 0, ht);
        jmdns.addServiceListener(SERVICE_TYPE, this);
        try {
            String originalServiceName = serviceInfo.getName();
            jmdns.registerService(serviceInfo);
            String realizedServiceName = getRealizedServiceName(serviceInfo);
            presence.setServiceName(realizedServiceName);

            if (!originalServiceName.equals(realizedServiceName)) {
                serviceNameChanged(realizedServiceName, originalServiceName);
            }
        }
        catch (IOException ioe) {
            throw new XMPPException.XMPPErrorException("Failed to register DNS-SD Service", new XMPPError(XMPPError.Condition.undefined_condition), ioe);
        }
    }

    /**
     * Reregister the DNS-SD service with the daemon.
     *
     * Note: This method does not accommodate changes to the mDNS Service Name!
     * This method may be used to announce changes to the DNS TXT record.
     */
    protected void reannounceService() throws XMPPException {
        try {
            jmdns.unregisterService(serviceInfo);
            jmdns.registerService(serviceInfo);
            // Note that because ServiceInfo objects are tracked
            // within JmDNS by service name, if that value has changed
            // we won't be able to successfully remove the 'old' service.
            // Previously, jmdns exposed the following method:
            //jmdns.reannounceService(serviceInfo);
        }
        catch (IOException ioe) {
            throw new XMPPException.XMPPErrorException("Exception occured when reannouncing mDNS presence.", new XMPPError(XMPPError.Condition.undefined_condition), ioe);
        }
    }

    public void serviceNameChanged(String newName, String oldName) {
        try {
            super.serviceNameChanged(newName, oldName);
        }
        catch (Throwable t) {
            // ignore
        }
    }

    /**
     * Unregister the DNS-SD service, making the client unavailable.
     */
    public void makeUnavailable() {
        jmdns.unregisterService(serviceInfo);
        serviceInfo = null;
    }


    @Override
    public void spam() {
        super.spam();
        System.out.println("Service name: " + serviceInfo.getName());
    }

    /** vv {@link javax.jmdns.ServiceListener} vv **/

    @Override
    public void serviceAdded(ServiceEvent event) {
        // Calling super.serviceNameChanged changes
        // the current local presence to that of the
        // newly added Service.
        // How can we assume that a new Service added
        // corresponds to the local service name changing?
        // This logic is currently executed when a new client joins
        // changing the local presence and confusing our chat logic
        // We could perhaps consider services added at the same host
        // address to be name changes... But that also opens the
        // door to undesired behavior when DHCP leases expire
        // and local addresses are recycled

        // What's wrong with treating new services as new services?
        // From my reading of XEP-0174, I don't see any reason why
        // a client should change their service name.

//        System.out.println("Service added " + event.getName());
//        if (!presence.getServiceName().equals(event.getName())) {
//            super.serviceNameChanged(event.getName(), presence.getServiceName());
//        }
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {

    }

    @Override
    public void serviceResolved(ServiceEvent event) {

    }

    /** ^^ {@link javax.jmdns.ServiceListener} ^^ **/

    /**
     * JmDNS may change the name of a requested service to enforce uniqueness
     * within its DNS cache. This helper method can be called after {@link javax.jmdns.JmDNS#registerService(javax.jmdns.ServiceInfo)}
     * with the passed {@link javax.jmdns.ServiceInfo} to attempt to determine the actual service
     * name registered. e.g: "test@example" may become "test@example (2)"
     *
     * @param requestedInfo the ServiceInfo instance passed to {@link javax.jmdns.JmDNS#registerService(javax.jmdns.ServiceInfo)}
     * @return the unique service name actually being advertised by JmDNS. If no
     *         match found, return requestedInfo.getName()
     */
    private String getRealizedServiceName(ServiceInfo requestedInfo) {
        Map<String, ServiceInfo> map = ((JmDNSImpl) jmdns).getServices();
        // Check if requested service name is used verbatim
        if (map.containsKey(requestedInfo.getKey())) {
            return map.get(requestedInfo.getKey()).getName();
        }

        // The service name was altered... Search registered services
        // e.g test@example.presence._tcp.local would match test@example (2).presence._tcp.local
        for (ServiceInfo info : map.values()) {
            if (info.getName().contains(requestedInfo.getName())
                    && info.getTypeWithSubtype().equals(requestedInfo.getTypeWithSubtype())) {
                return info.getName();
            }
        }

        // No match found! Return expected name
        return requestedInfo.getName();
    }
}
