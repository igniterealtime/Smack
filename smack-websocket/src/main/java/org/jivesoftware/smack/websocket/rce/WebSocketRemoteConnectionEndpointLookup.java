/**
 *
 * Copyright 2020 Aditya Borikar, Florian Schmaus
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
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.altconnections.HttpLookupMethod;
import org.jivesoftware.smack.altconnections.HttpLookupMethod.LinkRelation;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpointLookupFailure;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.jid.DomainBareJid;

public final class WebSocketRemoteConnectionEndpointLookup {

    public static Result lookup(DomainBareJid domainBareJid) {
        List<RemoteConnectionEndpointLookupFailure> lookupFailures = new ArrayList<>(1);

        List<URI> rcUriList = null;
        try {
            // Look for remote connection endpoints by making use of http lookup method described inside XEP-0156.
            rcUriList = HttpLookupMethod.lookup(domainBareJid,
                            LinkRelation.WEBSOCKET);
        } catch (IOException | XmlPullParserException | URISyntaxException e) {
            lookupFailures.add(new RemoteConnectionEndpointLookupFailure.HttpLookupFailure(
                            domainBareJid, e));
            return new Result(lookupFailures);
        }

        List<SecureWebSocketRemoteConnectionEndpoint> discoveredSecureEndpoints = new ArrayList<>(rcUriList.size());
        List<InsecureWebSocketRemoteConnectionEndpoint> discoveredInsecureEndpoints = new ArrayList<>(rcUriList.size());

        for (URI webSocketUri : rcUriList) {
            WebSocketRemoteConnectionEndpoint wsRce = WebSocketRemoteConnectionEndpoint.from(webSocketUri);
            if (wsRce instanceof SecureWebSocketRemoteConnectionEndpoint) {
                SecureWebSocketRemoteConnectionEndpoint secureWsRce = (SecureWebSocketRemoteConnectionEndpoint) wsRce;
                discoveredSecureEndpoints.add(secureWsRce);
            } else if (wsRce instanceof InsecureWebSocketRemoteConnectionEndpoint) {
                InsecureWebSocketRemoteConnectionEndpoint insecureWsRce = (InsecureWebSocketRemoteConnectionEndpoint) wsRce;
                discoveredInsecureEndpoints.add(insecureWsRce);
            } else {
                // WebSocketRemoteConnectionEndpoint.from() must return an instance which type is one of the above.
                throw new AssertionError();
            }
        }

        return new Result(discoveredSecureEndpoints, discoveredInsecureEndpoints, lookupFailures);
    }

    public static final class Result {
        public final List<SecureWebSocketRemoteConnectionEndpoint> discoveredSecureEndpoints;
        public final List<InsecureWebSocketRemoteConnectionEndpoint> discoveredInsecureEndpoints;
        public final List<RemoteConnectionEndpointLookupFailure> lookupFailures;

        public Result() {
            this(Collections.emptyList());
        }

        public Result(List<RemoteConnectionEndpointLookupFailure> lookupFailures) {
            // The list of endpoints needs to be mutable, because maybe a user supplied endpoint will be added to it.
            // Hence we do not use Collections.emptyList() as argument for the discovered endpoints.
            this(new ArrayList<>(1), new ArrayList<>(1), lookupFailures);
        }

        public Result(List<SecureWebSocketRemoteConnectionEndpoint> discoveredSecureEndpoints,
                        List<InsecureWebSocketRemoteConnectionEndpoint> discoveredInsecureEndpoints,
                        List<RemoteConnectionEndpointLookupFailure> lookupFailures) {
            this.discoveredSecureEndpoints = discoveredSecureEndpoints;
            this.discoveredInsecureEndpoints = discoveredInsecureEndpoints;
            this.lookupFailures = lookupFailures;
        }

        public boolean isEmpty() {
            return discoveredSecureEndpoints.isEmpty() && discoveredInsecureEndpoints.isEmpty();
        }

        public int discoveredEndpointCount() {
            return discoveredSecureEndpoints.size() + discoveredInsecureEndpoints.size();
        }

        // TODO: Remove the following methods since the fields are already public? Or make the fields private and use
        // the methods? I tend to remove the methods, as their method name is pretty long. But OTOH the fields reference
        // mutable datastructes, which is uncommon to be public.
        public List<SecureWebSocketRemoteConnectionEndpoint> getDiscoveredSecureRemoteConnectionEndpoints() {
            return discoveredSecureEndpoints;
        }

        public List<InsecureWebSocketRemoteConnectionEndpoint> getDiscoveredInsecureRemoteConnectionEndpoints() {
            return discoveredInsecureEndpoints;
        }

        public List<RemoteConnectionEndpointLookupFailure> getLookupFailures() {
            return lookupFailures;
        }
    }
}
