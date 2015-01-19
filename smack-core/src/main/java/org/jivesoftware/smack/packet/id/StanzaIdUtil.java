/**
 *
 * Copyright 2003-2007 Jive Software, 2015 Florian Schmaus
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
package org.jivesoftware.smack.packet.id;

import java.util.concurrent.atomic.AtomicLong;

import org.jivesoftware.smack.util.StringUtils;

public class StanzaIdUtil {

    /**
     * A prefix helps to make sure that ID's are unique across multiple instances.
     */
    private static final String PREFIX = StringUtils.randomString(5) + "-";

    /**
     * Keeps track of the current increment, which is appended to the prefix to
     * forum a unique ID.
     */
    private static final AtomicLong ID = new AtomicLong();

    public static String newStanzaId() {
        return PREFIX + Long.toString(ID.incrementAndGet());
    }
}
