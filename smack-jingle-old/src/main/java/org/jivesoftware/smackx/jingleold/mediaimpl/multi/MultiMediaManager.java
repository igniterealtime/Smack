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
package org.jivesoftware.smackx.jingleold.mediaimpl.multi;

import org.jivesoftware.smackx.jingleold.JingleSession;
import org.jivesoftware.smackx.jingleold.media.JingleMediaManager;
import org.jivesoftware.smackx.jingleold.media.JingleMediaSession;
import org.jivesoftware.smackx.jingleold.media.PayloadType;
import org.jivesoftware.smackx.jingleold.nat.JingleTransportManager;
import org.jivesoftware.smackx.jingleold.nat.TransportCandidate;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements a MultiMediaManager using other JingleMediaManager implementations.
 * It supports every Codecs that JingleMediaManagers added has.
 *
 * @author Thiago Camargo
 */

public class MultiMediaManager extends JingleMediaManager {

    public static final String MEDIA_NAME = "Multi";

    private List<JingleMediaManager> managers = new ArrayList<JingleMediaManager>();

    private PayloadType preferredPayloadType = null;

    public MultiMediaManager(JingleTransportManager transportManager) {
        super(transportManager);
    }

    public void addMediaManager(JingleMediaManager manager) {
        managers.add(manager);
    }

    public void removeMediaManager(JingleMediaManager manager) {
        managers.remove(manager);
    }

    /**
     * Return all supported Payloads for this Manager.
     *
     * @return The Payload List
     */
    @Override
    public List<PayloadType> getPayloads() {
        List<PayloadType> list = new ArrayList<PayloadType>();
        if (preferredPayloadType != null) list.add(preferredPayloadType);
        for (JingleMediaManager manager : managers) {
            for (PayloadType payloadType : manager.getPayloads()) {
                if (!list.contains(payloadType) && !payloadType.equals(preferredPayloadType))
                    list.add(payloadType);
            }
        }
        return list;
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
    public JingleMediaSession createMediaSession(PayloadType payloadType, final TransportCandidate remote, final TransportCandidate local, final JingleSession jingleSession) {
        for (JingleMediaManager manager : managers) {
            if (manager.getPayloads().contains(payloadType)) {
                return manager.createMediaSession(payloadType, remote, local, jingleSession);
            }
        }
        return null;
    }

    @Override
    public PayloadType getPreferredPayloadType() {
        if (preferredPayloadType != null) return preferredPayloadType;
        return super.getPreferredPayloadType();
    }

    public void setPreferredPayloadType(PayloadType preferredPayloadType) {
        this.preferredPayloadType = preferredPayloadType;
    }

    @Override
    public  String getName() {
        return MEDIA_NAME;
    }
}
