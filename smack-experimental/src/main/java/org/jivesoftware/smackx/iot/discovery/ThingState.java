/**
 *
 * Copyright 2016 Florian Schmaus
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
package org.jivesoftware.smackx.iot.discovery;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jivesoftware.smack.util.Async;

import org.jivesoftware.smackx.iot.element.NodeInfo;

import org.jxmpp.jid.BareJid;

public class ThingState {

    private final NodeInfo nodeInfo;

    private BareJid registry;
    private BareJid owner;
    private boolean removed;

    private final List<ThingStateChangeListener> listeners = new CopyOnWriteArrayList<>();

    ThingState(NodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    void setRegistry(BareJid registry) {
        this.registry = registry;
    }

    void setUnregistered() {
        this.registry = null;
    }

    void setOwner(final BareJid owner) {
        this.owner = owner;
        Async.go(new Runnable() {
            @Override
            public void run() {
                for (ThingStateChangeListener thingStateChangeListener : listeners) {
                    thingStateChangeListener.owned(owner);
                }
            }
        });
    }

    void setUnowned() {
        this.owner = null;
    }

    void setRemoved() {
        removed = true;
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    public BareJid getRegistry() {
        return registry;
    }

    public BareJid getOwner() {
        return owner;
    }

    public boolean isOwned() {
        return owner != null;
    }

    public boolean isRemoved() {
        return removed;
    }

    public boolean setThingStateChangeListener(ThingStateChangeListener thingStateChangeListener) {
        return listeners.add(thingStateChangeListener);
    }

    public boolean removeThingStateChangeListener(ThingStateChangeListener thingStateChangeListener) {
        return listeners.remove(thingStateChangeListener);
    }
}
