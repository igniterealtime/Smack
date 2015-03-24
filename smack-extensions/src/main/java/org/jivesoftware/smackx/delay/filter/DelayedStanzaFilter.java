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
package org.jivesoftware.smackx.delay.filter;

import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.delay.DelayInformationManager;

/**
 * Filters stanza with delay information, ie. stanzas that got delayed for some reason
 */
public class DelayedStanzaFilter implements StanzaFilter {

    public static final StanzaFilter INSTANCE = new DelayedStanzaFilter();

    /**
     * Filters stanzas that got not delayed, ie. have no delayed information
     */
    public static final StanzaFilter NOT_DELAYED_STANZA = new NotFilter(INSTANCE);

    private DelayedStanzaFilter() {
    }

    @Override
    public boolean accept(Stanza packet) {
        return DelayInformationManager.isDelayedStanza(packet);
    }

}
