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

import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.Objects;

import org.jxmpp.jid.EntityBareJid;

public class MemoryAvatarMetadataStore implements AvatarMetadataStore {

    private Map<Tuple<EntityBareJid, String>, Boolean> availabilityMap = new ConcurrentHashMap<>();

    @Override
    public boolean hasAvatarAvailable(EntityBareJid jid, String itemId) {
        Boolean available = availabilityMap.get(new Tuple<>(jid, itemId));
        return available != null && available;
    }

    @Override
    public void setAvatarAvailable(EntityBareJid jid, String itemId) {
        availabilityMap.put(new Tuple<>(jid, itemId), Boolean.TRUE);
    }

    private static class Tuple<A, B> {
        private final A first;
        private final B second;

        Tuple(A first, B second) {
            this.first = first;
            this.second = second;
        }

        public A getFirst() {
            return first;
        }

        public B getSecond() {
            return second;
        }

        @Override
        public int hashCode() {
            return HashCode.builder()
                    .append(first)
                    .append(second)
                    .build();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Tuple)) {
                return false;
            }

            @SuppressWarnings("unchecked") Tuple<A, B> other = (Tuple<A, B>) obj;
            return Objects.equals(getFirst(), other.getFirst())
                    && Objects.equals(getSecond(), other.getSecond());
        }
    }
}
