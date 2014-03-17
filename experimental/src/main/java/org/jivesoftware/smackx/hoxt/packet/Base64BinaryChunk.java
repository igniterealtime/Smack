/**
 *
 * Copyright 2014 Andriy Tsykholyas
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
package org.jivesoftware.smackx.hoxt.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.hoxt.HOXTManager;

/**
 * Packet extension for base64 binary chunks.<p>
 * This class is immutable.
 *
 * @author Andriy Tsykholyas
 * @see <a href="http://xmpp.org/extensions/xep-0332.html">XEP-0332: HTTP over XMPP transport</a>
 */
public class Base64BinaryChunk implements PacketExtension {

    public static final String ELEMENT_CHUNK = "chunk";
    public static final String ATTRIBUTE_STREAM_ID = "streamId";
    public static final String ATTRIBUTE_LAST = "last";

    private final String streamId;
    private final boolean last;
    private final String text;

    /**
     * Creates the extension.
     *
     * @param text     value of text attribute
     * @param streamId value of streamId attribute
     * @param last     value of last attribute
     */
    public Base64BinaryChunk(String text, String streamId, boolean last) {
        this.text = text;
        this.streamId = streamId;
        this.last = last;
    }

    /**
     * Creates the extension. Last attribute will be initialized with default value (false).
     *
     * @param text     value of text attribute
     * @param streamId value of streamId attribute
     */
    public Base64BinaryChunk(String text, String streamId) {
        this(text, streamId, false);
    }

    /**
     * Returns streamId attribute.
     *
     * @return streamId attribute
     */
    public String getStreamId() {
        return streamId;
    }

    /**
     * Returns last attribute.
     *
     * @return last attribute
     */
    public boolean isLast() {
        return last;
    }

    /**
     * Returns text attribute.
     *
     * @return text attribute
     */
    public String getText() {
        return text;
    }

    @Override
    public String getElementName() {
        return ELEMENT_CHUNK;
    }

    @Override
    public String getNamespace() {
        return HOXTManager.NAMESPACE;
    }

    @Override
    public String toXML() {
        StringBuilder builder = new StringBuilder();
        builder.append("<chunk xmlns='urn:xmpp:http' streamId='");
        builder.append(streamId);
        builder.append("' last='");
        builder.append(Boolean.toString(last));
        builder.append("'>");
        builder.append(text);
        builder.append("</chunk>");
        return builder.toString();
    }
}
