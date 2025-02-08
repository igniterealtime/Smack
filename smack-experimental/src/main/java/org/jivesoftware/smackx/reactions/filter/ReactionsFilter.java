/**
 *
 * Copyright 2025 Ismael Nunes Campos
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
package org.jivesoftware.smackx.reactions.filter;

import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;

import org.jivesoftware.smackx.reactions.element.ReactionsElement;

/**
 * Message Reactions filter class.
 *
 * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0444: Message
 *      Reactions</a>
 * @author Ismael Nunes Campos
 *
 */
public final class ReactionsFilter extends StanzaExtensionFilter {

    public static final StanzaFilter INSTANCE = new ReactionsFilter(ReactionsElement.ELEMENT, ReactionsElement.NAMESPACE);

    private ReactionsFilter(String element, String namespace) {
        super(element, namespace);
    }
}
