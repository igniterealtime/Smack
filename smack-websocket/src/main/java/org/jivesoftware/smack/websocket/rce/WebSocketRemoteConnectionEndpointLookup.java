/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smack.websocket.rce;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.altconnections.HttpLookupMethod;
import org.jivesoftware.smack.altconnections.HttpLookupMethod.LinkRelation;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpointLookupFailure;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.jid.DomainBareJid;

public final class WebSocketRemoteConnectionEndpointLookup {

    public static Result lookup(DomainBareJid domainBareJid, SecurityMode securityMode) {
        List<RemoteConnectionEndpointLookupFailure> lookupFailures = new ArrayList<>(1);
        List<WebSocketRemoteConnectionEndpoint> discoveredRemoteConnectionEndpoints = new ArrayList<>();

        List<URI> rcUriList = null;
        try {
            // Look for remote connection endpoints by making use of http lookup method described inside XEP-0156.
            rcUriList = HttpLookupMethod.lookup(domainBareJid,
                            LinkRelation.WEBSOCKET);
        } catch (IOException | XmlPullParserException | URISyntaxException e) {
            lookupFailures.add(new RemoteConnectionEndpointLookupFailure.HttpLookupFailure(
                            domainBareJid, e));
            return new Result(discoveredRemoteConnectionEndpoints, lookupFailures);
        }

        if (rcUriList.isEmpty()) {
            throw new IllegalStateException("No endpoints were found inside host-meta");
        }

        // Convert rcUriList to List<WebSocketRemoteConnectionEndpoint>
        Iterator<URI> iterator = rcUriList.iterator();
        List<WebSocketRemoteConnectionEndpoint> rceList = new ArrayList<>();
        while (iterator.hasNext()) {
            rceList.add(new WebSocketRemoteConnectionEndpoint(iterator.next()));
        }

        switch (securityMode) {
            case ifpossible:
                // If security mode equals `if-possible`, give priority to secure endpoints over insecure endpoints.

                // Seprate secure and unsecure endpoints.
                List<WebSocketRemoteConnectionEndpoint> secureEndpointsForSecurityModeIfPossible = new ArrayList<>();
                List<WebSocketRemoteConnectionEndpoint> insecureEndpointsForSecurityModeIfPossible = new ArrayList<>();
                for (WebSocketRemoteConnectionEndpoint uri : rceList) {
                    if (uri.isSecureEndpoint()) {
                        secureEndpointsForSecurityModeIfPossible.add(uri);
                    } else {
                        insecureEndpointsForSecurityModeIfPossible.add(uri);
                    }
                }
                discoveredRemoteConnectionEndpoints = secureEndpointsForSecurityModeIfPossible;
                discoveredRemoteConnectionEndpoints.addAll(insecureEndpointsForSecurityModeIfPossible);
                break;
            case required:
            case disabled:
                /**
                 * If, SecurityMode equals to required, accept wss endpoints (secure endpoints) only or,
                 * if SecurityMode equals to disabled, accept ws endpoints (unsecure endpoints) only.
                 */
                for (WebSocketRemoteConnectionEndpoint uri : rceList) {
                    if ((securityMode.equals(SecurityMode.disabled) && !uri.isSecureEndpoint())
                                    || (securityMode.equals(SecurityMode.required) && uri.isSecureEndpoint())) {
                        discoveredRemoteConnectionEndpoints.add(uri);
                    }
                }
                break;
            default:
        }
        return new Result(discoveredRemoteConnectionEndpoints, lookupFailures);
    }

    public static final class Result {
        public final List<WebSocketRemoteConnectionEndpoint> discoveredRemoteConnectionEndpoints;
        public final List<RemoteConnectionEndpointLookupFailure> lookupFailures;

        public Result(List<WebSocketRemoteConnectionEndpoint> discoveredRemoteConnectionEndpoints,
                        List<RemoteConnectionEndpointLookupFailure> lookupFailures) {
            this.discoveredRemoteConnectionEndpoints = discoveredRemoteConnectionEndpoints;
            this.lookupFailures = lookupFailures;
        }

        public List<WebSocketRemoteConnectionEndpoint> getDiscoveredRemoteConnectionEndpoints() {
            return discoveredRemoteConnectionEndpoints;
        }

        public List<RemoteConnectionEndpointLookupFailure> getLookupFailures() {
            return lookupFailures;
        }
    }
}
