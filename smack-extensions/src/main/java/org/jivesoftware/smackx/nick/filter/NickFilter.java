/**
 *
 * Copyright 2018 Miguel Hincapie.
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
package org.jivesoftware.smackx.nick.filter;

import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smackx.nick.packet.Nick;

/**
 * Used with NickManager.
 *
 * @author Miguel Hincapie 2018
 * @see <a href="http://xmpp.org/extensions/xep-0172.html">XEP-0172: User Nickname</a>
 */
public final class NickFilter extends StanzaExtensionFilter {

    public static final StanzaFilter INSTANCE = new NickFilter(Nick.NAMESPACE);

    private NickFilter(String namespace) {
        super(namespace);
    }
}
