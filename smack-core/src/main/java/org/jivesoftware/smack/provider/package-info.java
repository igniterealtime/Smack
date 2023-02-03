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
 * The Smack provider architecture is a system for plugging in custom XML parsing of staza extensions
 * ({@link org.jivesoftware.smack.packet.ExtensionElement}, {@link org.jivesoftware.smack.packet.IQ} stanzas and
 * {@link org.jivesoftware.smack.packet.Nonza}. Hence, there are the the following providers:
 * <ul>
 * <li>{@link ExtensionElementProvider}</li>
 * <li>{@link IqProvider}</li>
 * <li>{@link NonzaProvider}</li>
 * </ul>
 * For most users, only extension element and IQ providers should be relevant.
 * <h2>Architecture</h2>
 * <p>
 * Providers are registered with the {@link ProviderManager}. XML elements identified by their
 * {@link javax.xml.namespace.QName}, that is, their qualified name consistent of the XML elements name and its
 * namespace. The QName is hence used to map XML elements to their provider Whenever a stanza extension is found in a
 * stanza, parsing will be passed to the correct provider. Each provider is responsible for parsing the XML stream via
 * Smack's {@link org.jivesoftware.smack.xml.XmlPullParser}.
 * </p>
 * <h2>Unknown Extension Elements</h2>
 * <p>
 * If no extension element provider is registered for an element, then Smack will fall back to parse the "unknown"
 * element to a {@link org.jivesoftware.smack.packet.StandardExtensionElement}.
 * </p>
 * <h2>Custom Provider Example</h2>
 * See {@link IqProvider} for examples.
 */
package org.jivesoftware.smack.provider;
