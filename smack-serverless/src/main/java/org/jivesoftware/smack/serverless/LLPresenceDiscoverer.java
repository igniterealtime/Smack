/**
 *
 * Copyright 2009 Jonas Ådahl.
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

package org.jivesoftware.smack.serverless;


import java.util.Set;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Link-local presence discoverer. XEP-0174 describes how to use mDNS/DNS-SD.
 * This class in an abstract representation of the basic functionality of
 * handling presences discovering.
 */
public abstract class LLPresenceDiscoverer {
    // Listeners to be notified about changes.
    protected Set<LLPresenceListener> listeners = new CopyOnWriteArraySet<LLPresenceListener>();
    // Map of service name -> Link-local presence
    private Map<String,LLPresence> presences = new ConcurrentHashMap<String,LLPresence>();

    /**
     * Add listener which will be notified when new presences are discovered,
     * presence information changed or presences goes offline.
     * @param listener the listener to be notified.
     */
    public void addPresenceListener(LLPresenceListener listener) {
        listeners.add(listener);
        for (LLPresence presence : presences.values())
            listener.presenceNew(presence);
    }

    /**
     * Remove presence listener.
     * @param listener listener to be removed.
     */
    public void removePresenceListener(LLPresenceListener listener) {
        listeners.remove(listener);
    }

    /**
     * Return a collection of presences known.
     * @return all known presences.
     */
    public Collection<LLPresence> getPresences() {
        return presences.values();
    }

    /**
     * Return the presence with the specified service name.
     * 
     * @param name service name of the presence.
     * @return the presence information with the given service name.
     */
    public LLPresence getPresence(String name) {
        return presences.get(name);
    }

    /**
     * Used by the class extending this one to tell when new
     * presence is added.
     * 
     * @param name service name of the presence.
     */
    protected void presenceAdded(String name) {
        presences.put(name, null);
    }

    /**
     * Used by the class extending this one to tell when new
     * presence information is added.
     *
     * @param name service name of the presence.
     * @param presence presence information.
     */
    protected void presenceInfoAdded(String name, LLPresence presence) {
        presences.put(name, presence);
        for (LLPresenceListener l : listeners)
            l.presenceNew(presence);
    }

    /** 
     * Used by the class extending this one to tell when a presence
     * goes offline.
     *
     * @param name service name of the presence going offline.
     */
    protected void presenceRemoved(String name) {
        LLPresence presence = presences.remove(name);
        for (LLPresenceListener l : listeners)
            l.presenceRemove(presence);
    }
}
