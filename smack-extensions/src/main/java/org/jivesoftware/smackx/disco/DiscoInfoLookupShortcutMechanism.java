/**
 *
 * Copyright 2018 Florian Schmaus.
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
package org.jivesoftware.smackx.disco;

import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

import org.jxmpp.jid.Jid;

public abstract class DiscoInfoLookupShortcutMechanism implements Comparable<DiscoInfoLookupShortcutMechanism> {

    private final String name;
    private final int priority;

    protected DiscoInfoLookupShortcutMechanism(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    public final String getName() {
        return name;
    }

    /**
     * Get the priority of this mechanism. Lower values mean higher priority.
     *
     * @return the priority of this mechanism.
     */
    public final int getPriority() {
        return priority;
    }

    public abstract DiscoverInfo getDiscoverInfoByUser(ServiceDiscoveryManager serviceDiscoveryManager, Jid jid);

    @Override
    public final int compareTo(DiscoInfoLookupShortcutMechanism other) {
        // Switch to Integer.compare(int, int) once Smack is on Android 19 or higher.
        Integer ourPriority = getPriority();
        return ourPriority.compareTo(other.getPriority());
    }
}
