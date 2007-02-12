/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smack.filter;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Message;

/**
 * Filters for message packets with a particular thread value.
 *
 * @author Matt Tucker
 */
public class ThreadFilter implements PacketFilter {

    private String thread;

    /**
     * Creates a new thread filter using the specified thread value.
     *
     * @param thread the thread value to filter for.
     */
    public ThreadFilter(String thread) {
        if (thread == null) {
            throw new IllegalArgumentException("Thread cannot be null.");
        }
        this.thread = thread;
    }

    public boolean accept(Packet packet) {
        return packet instanceof Message && thread.equals(((Message) packet).getThread());
    }
}
