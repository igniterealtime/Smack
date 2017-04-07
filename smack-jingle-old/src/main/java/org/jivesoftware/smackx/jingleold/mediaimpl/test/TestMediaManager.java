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
package org.jivesoftware.smackx.jingleold.mediaimpl.test;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smackx.jingleold.JingleSession;
import org.jivesoftware.smackx.jingleold.media.JingleMediaManager;
import org.jivesoftware.smackx.jingleold.media.JingleMediaSession;
import org.jivesoftware.smackx.jingleold.media.PayloadType;
import org.jivesoftware.smackx.jingleold.nat.JingleTransportManager;
import org.jivesoftware.smackx.jingleold.nat.TransportCandidate;

/**
 * Implements a MediaManager for test purposes.
 *
 * @author Thiago Camargo
 */

public class TestMediaManager extends JingleMediaManager {

    public static final String MEDIA_NAME = "TestMedia";

    private List<PayloadType> payloads = new ArrayList<PayloadType>();

    private PayloadType preferredPayloadType = null;

    public TestMediaManager(JingleTransportManager transportManager) {
        super(transportManager);
    }

    /**
    * Return all supported Payloads for this Manager.
    *
    * @return The Payload List
    */
    @Override
    public List<PayloadType> getPayloads() {
        return payloads;
    }

    public void setPayloads(List<PayloadType> payloads) {
        this.payloads.addAll(payloads);
    }

    /**
     * Returns a new JingleMediaSession.
     *
     * @param payloadType payloadType
     * @param remote      remote Candidate
     * @param local       local Candidate
     * @return JingleMediaSession JingleMediaSession
     */
    @Override
    public JingleMediaSession createMediaSession(PayloadType payloadType, final TransportCandidate remote,
            final TransportCandidate local, final JingleSession jingleSession) {
        TestMediaSession session = null;

        session = new TestMediaSession(payloadType, remote, local, "", jingleSession);

        return session;
    }

    @Override
    public PayloadType getPreferredPayloadType() {
        if (preferredPayloadType != null)
            return preferredPayloadType;
        return super.getPreferredPayloadType();
    }

    public void setPreferredPayloadType(PayloadType preferredPayloadType) {
        this.preferredPayloadType = preferredPayloadType;
    }

    @Override
    public String getName() {
        return MEDIA_NAME;
    }
}
