/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-2003 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software.
 * Use is subject to license terms.
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
