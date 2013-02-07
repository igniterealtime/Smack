/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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
package org.jivesoftware.smackx.bytestreams.ibb;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;

/**
 * Utility methods to create packets.
 * 
 * @author Henning Staib
 */
public class IBBPacketUtils {

    /**
     * Returns an error IQ.
     * 
     * @param from the senders JID
     * @param to the recipients JID
     * @param xmppError the XMPP error
     * @return an error IQ
     */
    public static IQ createErrorIQ(String from, String to, XMPPError xmppError) {
        IQ errorIQ = new IQ() {

            public String getChildElementXML() {
                return null;
            }

        };
        errorIQ.setType(IQ.Type.ERROR);
        errorIQ.setFrom(from);
        errorIQ.setTo(to);
        errorIQ.setError(xmppError);
        return errorIQ;
    }

    /**
     * Returns a result IQ.
     * 
     * @param from the senders JID
     * @param to the recipients JID
     * @return a result IQ
     */
    public static IQ createResultIQ(String from, String to) {
        IQ result = new IQ() {

            public String getChildElementXML() {
                return null;
            }

        };
        result.setType(IQ.Type.RESULT);
        result.setFrom(from);
        result.setTo(to);
        return result;
    }

}
