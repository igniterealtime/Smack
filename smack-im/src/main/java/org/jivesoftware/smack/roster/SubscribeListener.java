/**
 *
 * Copyright 2015-2017 Florian Schmaus
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

package org.jivesoftware.smack.roster;

import org.jivesoftware.smack.packet.Presence;

import org.jxmpp.jid.Jid;


/**
 * Handle incoming requests to subscribe to our presence. The
 * {@link #processSubscribe(Jid, Presence)} method may return a subscribe
 * answer. If no subscribe answer is returned, the next listener will be
 * notified and asked. If no listener returns an answer, then nothing happens.
 *
 */
public interface SubscribeListener {

    enum SubscribeAnswer {
        /**
         * Approve the subscription request.
         */
        Approve,

        /**
         * Approve the subscription request and also request subscription from the other party if not already subscribed.
         * @since 4.2.2
         */
        ApproveAndAlsoRequestIfRequired,

        /**
         * Deny the subscription request.
         */
        Deny,
    }

    /**
     * Handle incoming presence subscription requests.
     *
     * @param from the JID requesting the subscription.
     * @param subscribeRequest the presence stanza used for the request.
     * @return a answer to the request, or <code>null</code>
     */
    SubscribeAnswer processSubscribe(Jid from, Presence subscribeRequest);

}
