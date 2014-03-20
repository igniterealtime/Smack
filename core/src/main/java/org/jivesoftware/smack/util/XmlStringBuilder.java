/**
 *
 * Copyright 2014 Florian Schmaus
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
package org.jivesoftware.smack.util;

public class XmlStringBuilder implements Appendable, CharSequence {

    private final StringBuilder sb;

    public XmlStringBuilder() {
        sb = new StringBuilder();
    }

    /**
     * Does nothing if content is null.
     *
     * @param name
     * @param content
     * @return
     */
    public XmlStringBuilder element(String name, String content) {
        if (content == null)
            return this;
        openElement(name);
        escape(content);
        closeElement(name);
        return this;
    }

    public XmlStringBuilder element(String name, Enum<?> content) {
        if (content != null) {
            element(name, content.name());
        }
        return this;
    }

    public XmlStringBuilder halfOpenElement(String name) {
        sb.append('<').append(name);
        return this;
    }

    public XmlStringBuilder openElement(String name) {
        halfOpenElement(name).append('>');
        return this;
    }

    public XmlStringBuilder closeElement(String name) {
        sb.append("</").append(name).append('>');
        return this;
    }

    public XmlStringBuilder emptyElementClose() {
        sb.append("/>");
        return this;
    }

    /**
     * Does nothing if value is null.
     *
     * @param name
     * @param value
     * @return
     */
    public XmlStringBuilder attribute(String name, String value) {
        if (value == null)
            return this;
        sb.append(' ').append(name).append("='");
        escape(value);
        sb.append('\'');
        return this;
    }

    public XmlStringBuilder xmlnsAttribute(String value) {
        attribute("xmlns", value);
        return this;
    }

    public XmlStringBuilder escape(String text) {
        sb.append(StringUtils.escapeForXML(text));
        return this;
    }

    @Override
    public XmlStringBuilder append(CharSequence csq) {
        sb.append(csq);
        return this;
    }

    @Override
    public XmlStringBuilder append(CharSequence csq, int start, int end) {
        sb.append(csq, start, end);
        return this;
    }

    @Override
    public XmlStringBuilder append(char c) {
        sb.append(c);
        return this;
    }

    @Override
    public int length() {
        return sb.length();
    }

    @Override
    public char charAt(int index) {
        return sb.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return sb.subSequence(start, end);
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
