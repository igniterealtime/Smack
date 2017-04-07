/**
 *
 * Copyright 2014 Vyacheslav Blinov
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

package org.jivesoftware.smackx.debugger.slf4j;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.Stanza;
import org.slf4j.Logger;


class SLF4JLoggingPacketListener implements StanzaListener {
    private final Logger logger;
    private final String prefix;

    public SLF4JLoggingPacketListener(Logger logger, String prefix) {
        this.logger = Validate.notNull(logger);
        this.prefix = Validate.notNull(prefix);
    }

    @Override
    public void processStanza(Stanza packet) {
        if (SLF4JSmackDebugger.printInterpreted.get() && logger.isDebugEnabled()) {
            logger.debug("{}: PKT [{}] '{}'", prefix, packet.getClass().getName(), packet.toXML());
        }
    }
}
