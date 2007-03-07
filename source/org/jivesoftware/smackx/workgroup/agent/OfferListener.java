/**
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

package org.jivesoftware.smackx.workgroup.agent;

/**
 * An interface which all classes interested in hearing about chat offers associated to a particular
 *  AgentSession instance should implement.<br>
 *
 * @author Matt Tucker
 * @author loki der quaeler
 * @see org.jivesoftware.smackx.workgroup.agent.AgentSession
 */
public interface OfferListener {

    /**
     * The implementing class instance will be notified via this when the AgentSession has received
     *  an offer for a chat. The instance will then have the ability to accept, reject, or ignore
     *  the request (resulting in a revocation-by-timeout).
     *
     * @param request the Offer instance embodying the details of the offer
     */
    public void offerReceived (Offer request);

    /**
     * The implementing class instance will be notified via this when the AgentSessino has received
     *  a revocation of a previously extended offer.
     *
     * @param revokedOffer the RevokedOffer instance embodying the details of the revoked offer
     */
    public void offerRevoked (RevokedOffer revokedOffer);

}
