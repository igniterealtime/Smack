/*
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.avatar;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.util.Pair;
import org.jxmpp.jid.EntityBareJid;

public class MemoryAvatarMetadataStore implements AvatarMetadataStore {

    private final Map<Pair<EntityBareJid, String>, Boolean> availabilityMap = new ConcurrentHashMap<>();

    @Override
    public boolean hasAvatarAvailable(EntityBareJid jid, String itemId) {
        Boolean available = availabilityMap.get(Pair.create(jid, itemId));
        return available != null && available;
    }

    @Override
    public void setAvatarAvailable(EntityBareJid jid, String itemId) {
        availabilityMap.put(Pair.create(jid, itemId), true);
    }

}
