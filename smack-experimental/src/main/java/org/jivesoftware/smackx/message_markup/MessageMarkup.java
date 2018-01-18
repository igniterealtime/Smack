/**
 *
 * Copyright © 2018 Paul Schaub
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
package org.jivesoftware.smackx.message_markup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smackx.message_markup.element.BlockQuoteElement;
import org.jivesoftware.smackx.message_markup.element.CodeBlockElement;
import org.jivesoftware.smackx.message_markup.element.ListElement;
import org.jivesoftware.smackx.message_markup.element.MarkupElement;
import org.jivesoftware.smackx.message_markup.element.SpanElement;

public final class MessageMarkup {

    private final List<SpanElement> spans = new ArrayList<>();
    private final List<BlockQuoteElement> quotes = new ArrayList<>();
    private final List<CodeBlockElement> codes = new ArrayList<>();
    private final List<ListElement> lists = new ArrayList<>();

    private MessageMarkup() {

    }

    /**
     * Return a new Builder for Message Markup elements.
     * @return builder.
     */
    public static MessageMarkup getBuilder() {
        return new MessageMarkup();
    }

    /**
     * Mark a section of a message as deleted.
     *
     * @param start start index
     * @param end end index
     * @return builder
     */
    public MessageMarkup setDeleted(int start, int end) {
        return addSpan(start, end, Collections.singleton(SpanElement.SpanStyle.deleted));
    }

    /**
     * Mark a section of a message as emphasized.
     *
     * @param start start index
     * @param end end index
     * @return builder
     */
    public MessageMarkup setEmphasis(int start, int end) {
        return addSpan(start, end, Collections.singleton(SpanElement.SpanStyle.emphasis));
    }

    /**
     * Mark a section of a message as inline code.
     *
     * @param start start index
     * @param end end index
     * @return builder
     */
    public MessageMarkup setCode(int start, int end) {
        return addSpan(start, end, Collections.singleton(SpanElement.SpanStyle.code));
    }

    /**
     * Add a span element.
     *
     * @param start start index
     * @param end end index
     * @param styles list of text styles for that span
     * @return builder
     */
    public MessageMarkup addSpan(int start, int end, Set<SpanElement.SpanStyle> styles) {
        verifyStartEnd(start, end);

        for (SpanElement other : spans) {
            if ((start >= other.getStart() && start <= other.getEnd()) ||
                    (end >= other.getStart() && end <= other.getEnd())) {
                throw new IllegalArgumentException("Spans MUST NOT overlap each other.");
            }
        }

        spans.add(new SpanElement(start, end, styles));
        return this;
    }

    /**
     * Mark a section of a message as block quote.
     *
     * @param start start index
     * @param end end index
     * @return builder
     */
    public MessageMarkup setBlockQuote(int start, int end) {
        verifyStartEnd(start, end);

        for (BlockQuoteElement other : quotes) {
            // 1 if out, 0 if on, -1 if in
            Integer s = start;
            Integer e = end;
            int startPos = s.compareTo(other.getStart()) * s.compareTo(other.getEnd());
            int endPos = e.compareTo(other.getStart()) * e.compareTo(other.getEnd());
            int allowed = startPos * endPos;

            if (allowed < 1) {
                throw new IllegalArgumentException("BlockQuotes MUST NOT overlap each others boundaries");
            }
        }

        quotes.add(new BlockQuoteElement(start, end));
        return this;
    }

    /**
     * Mark a section of a message as a code block.
     *
     * @param start start index
     * @param end end index
     * @return builder
     */
    public MessageMarkup setCodeBlock(int start, int end) {
        verifyStartEnd(start, end);

        codes.add(new CodeBlockElement(start, end));
        return this;
    }

    /**
     * Begin a list.
     *
     * @return list builder
     */
    public MessageMarkupListBuilder beginList() {
        return new MessageMarkupListBuilder(this);
    }

    public static final class MessageMarkupListBuilder {
        private final MessageMarkup markup;
        private final ArrayList<ListElement.ListEntryElement> entries = new ArrayList<>();
        private int end = -1;

        private MessageMarkupListBuilder(MessageMarkup markup) {
            this.markup = markup;
        }

        /**
         * Add an entry to the list.
         * The start index of an entry must correspond to the end index of the previous entry
         * (if a previous entry exists.)
         *
         * @param start start index
         * @param end end index
         * @return list builder
         */
        public MessageMarkupListBuilder addEntry(int start, int end) {
            verifyStartEnd(start, end);

            ListElement.ListEntryElement last = entries.size() == 0 ? null : entries.get(entries.size() - 1);
            // Entries themselves do not store end values, that's why we store the last entries end value in this.end
            if (last != null && start != this.end) {
                throw new IllegalArgumentException("Next entries start must be equal to last entries end (" + this.end + ").");
            }
            entries.add(new ListElement.ListEntryElement(start));
            this.end = end;

            return this;
        }

        /**
         * End the list.
         *
         * @return builder
         */
        public MessageMarkup endList() {
            if (entries.size() > 0) {
                ListElement.ListEntryElement first = entries.get(0);
                ListElement list = new ListElement(first.getStart(), end, entries);
                markup.lists.add(list);
            }

            return markup;
        }
    }

    /**
     * Build a Message Markup element.
     *
     * @return extension element
     */
    public MarkupElement build() {
        List<MarkupElement.MarkupChildElement> children = new ArrayList<>();
        children.addAll(spans);
        children.addAll(quotes);
        children.addAll(codes);
        children.addAll(lists);
        return new MarkupElement(children);
    }

    private static void verifyStartEnd(int start, int end) {
        if (start >= end || start < 0) {
            throw new IllegalArgumentException("Start value (" + start + ") MUST be greater equal than 0 " +
                    "and MUST be smaller than end value (" + end + ").");
        }
    }
}
