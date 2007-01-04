package org.jivesoftware.smackx.jingle.nat;

import org.jivesoftware.smack.XMPPException;

/**
 * $RCSfile$
 * $Revision: $
 * $Date: 02/01/2007
 *
 * Copyright 2003-2006 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class ICETransportManager extends JingleTransportManager {

    ICEResolver iceResolver = null;

    public ICETransportManager() {
        iceResolver = new ICEResolver();
        try {
            iceResolver.initializeAndWait();
        } catch (XMPPException e) {
            e.printStackTrace();
        }
    }

    protected TransportResolver createResolver() {
        try {
            iceResolver.resolve();
        } catch (XMPPException e) {
            e.printStackTrace();
        }
        return iceResolver;
    }

}
