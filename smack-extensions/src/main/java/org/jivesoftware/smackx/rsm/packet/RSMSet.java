/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smackx.rsm.packet;

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class RSMSet implements ExtensionElement {

    public static final String ELEMENT = "set";
    public static final String NAMESPACE = "http://jabber.org/protocol/rsm";

    private final String after;
    private final String before;
    private final int count;
    private final int index;
    private final String last;
    private final int max;
    private final String firstString;
    private final int firstIndex;

    public static enum PageDirection {
        before,
        after;
    }

    public RSMSet(int max) {
        this(max, -1);
    }

    public RSMSet(int max, int index) {
        this(null, null, -1, index, null, max, null, -1);
    }

    public RSMSet(String item, PageDirection pageDirection) {
        this(-1, item, pageDirection);
    }

    public RSMSet(int max, String item, PageDirection pageDirection) {
        switch (pageDirection) {
        case before:
            this.before = item;
            this.after = null;
            break;
        case after:
            this.before = null;
            this.after = item;
            break;
        default:
            throw new AssertionError();
        }
        this.count = -1;
        this.index = -1;
        this.last = null;
        this.max = max;
        this.firstString = null;
        this.firstIndex = -1;
    }

    public RSMSet(String after, String before, int count, int index,
                    String last, int max, String firstString, int firstIndex) {
        this.after = after;
        this.before = before;
        this.count = count;
        this.index = index;
        this.last = last;
        this.max = max;
        this.firstString = firstString;
        this.firstIndex = firstIndex;
    }

    public String getAfter() {
        return after;
    }

    public String getBefore() {
        return before;
    }

    public int getCount() {
        return count;
    }

    public int getIndex() {
        return index;
    }

    public String getLast() {
        return last;
    }

    public int getMax() {
        return max;
    }

    public String getFirst() {
        return firstString;
    }

    public int getFirstIndex() {
        return firstIndex;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        xml.optElement("after", after);
        xml.optElement("before", before);
        xml.optIntElement("count", count);
        if (firstString != null) {
            xml.halfOpenElement("first");
            xml.optIntAttribute("index", firstIndex);
            xml.rightAngleBracket();
            xml.append(firstString);
            xml.closeElement("first");
        }
        xml.optIntElement("index", index);
        xml.optElement("last", last);
        xml.optIntElement("max", max);
        xml.closeElement(this);
        return xml;
    }

    public static RSMSet from(Stanza packet) {
        return (RSMSet) packet.getExtension(ELEMENT, NAMESPACE);
    }

    public static RSMSet newAfter(String after) {
        return new RSMSet(after, PageDirection.after);
    }

    public static RSMSet newBefore(String before) {
        return new RSMSet(before, PageDirection.before);
    }
}
