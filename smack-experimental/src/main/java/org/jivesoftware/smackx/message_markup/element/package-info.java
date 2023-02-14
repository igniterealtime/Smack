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
 * Smack's API for XEP-0394: Message Markup, which can be used to style message. Message Markup is an alternative to
 * XHTML-im to style message, which keeps the message body and the markup information strictly separated.
 * <h2>Usage</h2>
 * <p>
 * The most important class is the {@link org.jivesoftware.smackx.message_markup.element.MarkupElement} class, which
 * contains a Builder to construct message markup..
 * </p>
 * <p>
 * To start creating a Message Markup Extension, call
 * {@link org.jivesoftware.smackx.message_markup.element.MarkupElement#getBuilder}. (Almost) all method calls documented
 * below will be made on the builder.
 * </p>
 * <p>
 * Whenever a method call receives a `start` and `end` index, `start` represents the first character, which is affected
 * by the styling, while `end` is the character *after* the last affected character.
 * </p>
 * <h2>Inline styling</h2>
 * <p>
 * Currently there are 3 styles available:
 * <ul>
 * <li>*emphasis*, which should be rendered by a client as *italic*, or **bold**</li>
 * <li>*code*, which should be rendered in `monospace`</li>
 * <li>*deleted*, which should be rendered as ~~strikethrough~~.</li>
 * </ul>
 * <p>
 * Those styles are available by calling `builder.setEmphasis(int start, int end)`, `builder.setDeleted(int start, int
 * end)` and `builder.setCode(int start, int end)`.
 * </p>
 * <p>
 * If you want to apply multiple inline styles to a section, you can do the following:
 * </p>
 *
 * <pre>
 * {@code
 * Set<SpanElement.SpanStyle> spanStyles = new HashSet<>();
 * styles.add(SpanElement.SpanStyle.emphasis);
 * styles.add(SpanElement.SpanStyle.deleted);
 * builder.addSpan(start, end, spanStyles);
 * }
 * </pre>
 * <p>
 * Note, that spans cannot overlap one another.
 * </p>
 * <h2 id="block-level-styling">Block Level Styling</h2>
 * <p>
 * Available block level styles are: * Code blocks, which should be rendered as
 * </p>
 *
 * <pre>
 * <code>blocks
 * of
 * code</code>
 * </pre>
 * <ul>
 * <li>Itemized lists, which should render as
 * <ul>
 * <li>Lists</li>
 * <li>with possibly multiple</li>
 * <li>entries</li>
 * </ul>
 * </li>
 * <li>Block Quotes, which should be rendered by the client &gt; as quotes, which &gt;&gt; also can be nested</li>
 * </ul>
 * <p>
 * To mark a section as code block, call <code>builder.setCodeBlock(start, end)</code>.
 * </p>
 * <p>
 * To create a list, call <code>MarkupElement.Builder.ListBuilder lbuilder = builder.beginList()</code>, which will
 * return a list builder. On this you can call <code>lbuilder.addEntry(start, end)</code> to add an entry.
 * </p>
 * <p>
 * Note: If you add an entry, the start value MUST be equal to the end value of the previous added entry!
 * </p>
 * <p>
 * To end the list, call <code>lbuilder.endList()</code>, which will return the MessageElement builder.
 * </p>
 * <p>
 * To create a block quote, call <code>builder.setBlockQuote(start, end)</code>.
 * </p>
 * <p>
 * Note that block level elements MUST NOT overlap each other boundaries, but may be fully contained (nested) within
 * each other.
 * </p>
 * @see <a href="http://xmpp.org/extensions/xep-0394.html">XEP-0394: Message Markup</a>
 */
package org.jivesoftware.smackx.message_markup.element;
