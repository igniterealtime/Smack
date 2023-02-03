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
 * Smack's API for XEP-0372: References.
 * <p>
 * References are a way to refer to other entities like users, other messages or external data from within a message.
 * </p>
 * <p>
 * Typical use-cases are mentioning other users by name, but referencing to their BareJid, or linking to a sent file.
 * </p>
 * <h2 id="usage">Usage</h2>
 * <p>
 * Mention a user and link to their bare jid.
 * </p>
 *
 * <pre>
 * <code>
 * Message message = new Message(&quot;Alice is a realy nice person.&quot;);
 * BareJid alice = JidCreate.bareFrom(&quot;alice@capulet.lit&quot;);
 * ReferenceManager.addMention(message, 0, 5, alice);
 * </code>
 * </pre>
 */
package org.jivesoftware.smackx.reference;
