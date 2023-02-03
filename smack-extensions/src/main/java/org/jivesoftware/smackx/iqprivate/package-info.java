/**
 *
 * Copyright 2015 Florian Schmaus
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
 * Smacks implementation of XEP-0049: Private XML Storage.
 * <p>
 * Manages private data, which is a mechanism to allow users to store arbitrary XML data on an XMPP server. Each private
 * data chunk is defined by a element name and XML namespace. Example private data:
 * </p>
 *
 * <pre>
 * <code>
 * &lt;color xmlns=&quot;http://example.com/xmpp/color&quot;&gt;
 *     &lt;favorite&gt;blue&lt;/blue&gt;
 *     &lt;leastFavorite&gt;puce&lt;/leastFavorite&gt;
 * &lt;/color&gt;
 * </code>
 * </pre>
 * @see <a href="https://xmpp.org/extensions/xep-0049.html">XEP-0049: Private XML Storage</a>
 */
package org.jivesoftware.smackx.iqprivate;
