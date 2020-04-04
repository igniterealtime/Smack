/**
 *
 * Copyright 2020 Florian Schmaus
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
 * The Smack XMPP client library.

 * <h2>Modular XMPP c2s connection</h2>
 * <p>
 * The graph below shows the current states of the modular xmpp client connection. Only some states are final states,
 * most states are intermediate states in order to reach a final state.
 * </p>
 * <img src="doc-files/ModularXmppClientToServerConnectionStateGraph.png" alt="The state graph of XmppNioTcpConnection">
 */
package org.jivesoftware.smack.full;
