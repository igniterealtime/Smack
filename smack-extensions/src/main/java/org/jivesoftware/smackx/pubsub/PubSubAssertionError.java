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

import org.jxmpp.jid.BareJid;

public abstract class PubSubAssertionError extends AssertionError {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected PubSubAssertionError(String message) {
        super(message);
    }

    public static class DiscoInfoNodeAssertionError extends PubSubAssertionError {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        DiscoInfoNodeAssertionError(BareJid pubSubService, String nodeId) {
            super("PubSub service '" + pubSubService + "' returned disco info result for node '" + nodeId
                            + "', but it did not contain an Identity of type 'leaf' or 'collection' (and category 'pubsub'), which is not allowed according to XEP-60 5.3.");
        }

    }
}
