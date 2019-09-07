/**
 *
 * Copyright © 2014-2019 Florian Schmaus
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

package org.jivesoftware.smack.packet;

/**
 * A Nonza, i.e everything that is <b>not a stanza</b> as defined
 * RFC 6120 8. Stanzas are {@link Message}, {@link Presence} and {@link IQ}.
 * Everything else should sublcass this class instead of {@link Stanza}.
 * <p>
 * It is important to cleanly distinguish between stanzas and non-stanzas. For
 * example plain stream elements don't count into the stanza count of XEP-198
 * Stream Management.
 * </p>
 *
 * @author Florian Schmaus
 * @see <a href="http://xmpp.org/extensions/xep-0360.html">XEP-0360: Nonzas (are not Stanzas)</a>
 */
public interface Nonza extends TopLevelStreamElement {

}
