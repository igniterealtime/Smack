/**
 *
 * Copyright 2003-2006 Jive Software.
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
package org.jivesoftware.smackx.jingleold.nat;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.jingleold.JingleSession;

/**
 * A Jingle Transport Manager implementation to be used on NAT networks with  STUN Service NOT Blocked.
 *
 * @author Thiago Camargo
 */
public class STUNTransportManager extends JingleTransportManager {
    private static final Logger LOGGER = Logger.getLogger(STUNTransportManager.class.getName());

    STUNResolver stunResolver = null;

    public STUNTransportManager() {
        stunResolver = new STUNResolver() {
        };
        try {
            stunResolver.initializeAndWait();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "exception", e);
        }
    }

    protected TransportResolver createResolver(JingleSession session) {
        try {
            stunResolver.resolve(session);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "exception", e);
        }
        return stunResolver;
    }
}
