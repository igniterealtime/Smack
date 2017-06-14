/**
 *
 * Copyright 2017 Florian Schmaus
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
package org.jivesoftware.smackx.hints;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.packet.Message;

import org.jivesoftware.smackx.hints.element.MessageProcessingHintType;
import org.jivesoftware.smackx.hints.element.NoCopyHint;
import org.jivesoftware.smackx.hints.element.NoPermanentStoreHint;
import org.jivesoftware.smackx.hints.element.NoStoreHint;
import org.jivesoftware.smackx.hints.element.StoreHint;

public class MessageProcessingHintsManager {

    public static Set<MessageProcessingHintType> getHintsFrom(Message message) {
        Set<MessageProcessingHintType> hints = null;

        boolean noCopyHint = NoCopyHint.hasHint(message);
        if (noCopyHint) {
            hints = new HashSet<>(MessageProcessingHintType.values().length);
            hints.add(MessageProcessingHintType.no_copy);
        }

        boolean noPermanentStoreHint = NoPermanentStoreHint.hasHint(message);
        if (noPermanentStoreHint) {
            if (hints == null) {
                hints = new HashSet<>(MessageProcessingHintType.values().length);
            }
            hints.add(MessageProcessingHintType.no_permanent_store);
        }

        boolean noStoreHint = NoStoreHint.hasHint(message);
        if (noStoreHint) {
            if (hints == null) {
                hints = new HashSet<>(MessageProcessingHintType.values().length);
            }
            hints.add(MessageProcessingHintType.no_store);
        }

        boolean storeHint = StoreHint.hasHint(message);
        if (storeHint) {
            if (hints == null) {
                hints = new HashSet<>(MessageProcessingHintType.values().length);
            }
            hints.add(MessageProcessingHintType.store);
        }

        if (hints == null) {
            return Collections.emptySet();
        }

        return hints;
    }
}
