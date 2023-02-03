/**
 *
 * Copyright 2018 Paul Schaub
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
/**
 * Smack's API for XEP-0382: Spoiler Messages, that can be used to indicate that
 * the body of a message is a spoiler and should be displayed as such.
 * <h2>Usage</h2>
 * <p>
 * Invoke {@link SpoilerManager#startAnnounceSupport()} to announce support for
 * spoiler messages.
 * </p>
 * <p>
 * Add spoilers to messages via
 * {@link org.jivesoftware.smackx.spoiler.element.SpoilerElement#addSpoiler(Message)},
 * {@link org.jivesoftware.smackx.spoiler.element.SpoilerElement#addSpoiler(Message, String)},
 * or
 * {@link org.jivesoftware.smackx.spoiler.element.SpoilerElement#addSpoiler(Message, String, String)}.
 * To get spoilers use
 * {@link org.jivesoftware.smackx.spoiler.element.SpoilerElement#getSpoilers(Message)}.
 * </p>
 *
 * @see <a href="https://xmpp.org/extensions/xep-0382.html">XEP-0382: Spoiler
 *      Messages</a>
 */
package org.jivesoftware.smackx.spoiler;
