/**
 * $RCSfile$
 * $Revision: $
 * $Date: 25/12/2006
 * <p/>
 * Copyright 2003-2006 Jive Software.
 * <p/>
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.smackx.jingle.mediaimpl.multi;

import org.jivesoftware.smackx.jingle.media.JingleMediaManager;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.media.PayloadType;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;

import java.util.*;

/**
 * Implements a MultiMediaManager using other JingleMediaManager implementations.
 * It supports every Codecs that JingleMediaManagers added has.
 *
 * @author Thiago Camargo
 */

public class MultiMediaManager extends JingleMediaManager {

    private List<JingleMediaManager> managers = new ArrayList<JingleMediaManager>();

    public MultiMediaManager() {
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
    public List<PayloadType> getPayloads() {
        List<PayloadType> list = new ArrayList<PayloadType>();
        for (JingleMediaManager manager : managers) {
            for (PayloadType payloadType : manager.getPayloads()) {
                if (!list.contains(payloadType))
                    list.add(payloadType);
            }
        }
        return list;
    }

    /**
     * Returns a new JingleMediaSession
     *
     * @param payloadType payloadType
     * @param remote remote Candidate
     * @param local local Candidate
     * @return JingleMediaSession JingleMediaSession
     */
    public JingleMediaSession createMediaSession(PayloadType payloadType, final TransportCandidate remote, final TransportCandidate local) {
        for (JingleMediaManager manager : managers) {
            if (manager.getPayloads().contains(payloadType)) {
                return manager.createMediaSession(payloadType, remote, local);
            }
        }
        return null;
    }
}
