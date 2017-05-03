/**
 *
 * Copyright 2017 Florian Schmaus
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
package org.jivesoftware.smackx.pubsub;

import org.jivesoftware.smack.SmackException;
import org.jxmpp.jid.BareJid;

public abstract class PubSubException extends SmackException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static class NotALeafNodeException extends PubSubException {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        private final String nodeId;
        private final BareJid pubSubService;

        NotALeafNodeException(String nodeId, BareJid pubSubService) {
            this.nodeId = nodeId;
            this.pubSubService = pubSubService;
        }

        public String getNodeId() {
            return nodeId;
        }

        public BareJid getPubSubService() {
            return pubSubService;
        }

    }
}
