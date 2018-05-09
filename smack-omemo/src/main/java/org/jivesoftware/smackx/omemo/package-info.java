/**
 *
 * Copyright 2017 Paul Schaub
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
 * Classes and interfaces for OMEMO Encryption. This module consists of the
 * XMPP logic and some abstract crypto classes that have to be implemented
 * using concrete crypto libraries (like signal-protocol-java or olm).
 * See smack-omemo-signal for a concrete implementation (GPL licensed).
 *
 * @author Paul Schaub
 * @see <a href="https://conversations.im/xeps/multi-end.html">XEP-0384: OMEMO</a>
 */
package org.jivesoftware.smackx.omemo;
