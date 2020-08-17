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

import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;

public class ForMatchingPredicateOrAfterXStanzas implements StanzaFilter {

    private final StanzaFilter predicate;
    private final AfterXStanzas afterXStanzas;

    public ForMatchingPredicateOrAfterXStanzas(StanzaFilter predicate, int count) {
        this.predicate = predicate;
        this.afterXStanzas = new AfterXStanzas(count);
    }

    @Override
    public boolean accept(Stanza packet) {
        if (predicate.accept(packet)) {
            afterXStanzas.resetCounter();
            return true;
        }
        return afterXStanzas.accept(packet);
    }
}
