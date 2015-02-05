/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smack.sm.predicates;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Stanza;

public class ShortcutPredicates implements PacketFilter {

    private final Set<PacketFilter> predicates = new LinkedHashSet<PacketFilter>();

    public ShortcutPredicates() {
    }

    public ShortcutPredicates(Collection<? extends PacketFilter> predicates) {
        this.predicates.addAll(predicates);
    }

    public boolean addPredicate(PacketFilter predicate) {
        return predicates.add(predicate);
    }

    public boolean removePredicate(PacketFilter prediacte) {
        return predicates.remove(prediacte);
    }

    @Override
    public boolean accept(Stanza packet) {
        for (PacketFilter predicate : predicates) {
            if (predicate.accept(packet)) {
                return true;
            }
        }
        return false;
    }
}
