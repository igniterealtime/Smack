package org.jivesoftware.jingleaudio.multi;

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
     * @param payloadType
     * @param remote
     * @param local
     * @return JingleMediaSession
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
