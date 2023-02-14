/**
 *
 * Copyright 2015-2022 Florian Schmaus
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
 * Allows {@link org.jivesoftware.smack.StanzaCollector} and {@link org.jivesoftware.smack.StanzaListener} instances to filter for stanzas with particular attributes.
 * <h2>Selected Filter Types</h2>
 * <ul>
 *   <li>{@link StanzaTypeFilter}: filters for stanzas that are a stanza type (Message, Presence, or IQ)</li>
 *   <li>{@link StanzaIdFilter}: filters for stanzas with a particular stanza ID</li>
 *   <li>{@link ToMatchesFilter}: filters for stanzas that are sent to a particular address</li>
 *   <li>{@link FromMatchesFilter}: filters for stanzas that are sent from a particular address</li>
 *   <li>{@link ExtensionElementFilter}: filters for stanzas that have a particular stanza exentsion element</li>
 *   <li>{@link AndFilter}: implements the logical AND operation over two or more filters</li>
 *   <li>{@link OrFilter}: implements the logical OR operation over two or more filters</li>
 *   <li>{@link NotFilter}: implements the logical NOT operation on a filter</li>
 * </ul>
 */
package org.jivesoftware.smack.filter;
