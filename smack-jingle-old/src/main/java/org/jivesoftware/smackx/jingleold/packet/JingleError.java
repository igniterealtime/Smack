/**
 *
 * Copyright 2003-2005 Jive Software.
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

package org.jivesoftware.smackx.jingleold.packet;

import java.util.Locale;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.jingleold.media.ContentInfo;
import org.xmlpull.v1.XmlPullParser;

public class JingleError implements ExtensionElement {

    public static String NAMESPACE = "urn:xmpp:tmp:jingle:errors";

    public static final JingleError OUT_OF_ORDER = new JingleError("out-of-order");

    public static final JingleError UNKNOWN_SESSION = new JingleError("unknown-session");

    public static final JingleError UNSUPPORTED_CONTENT = new JingleError(
            "unsupported-content");

    public static final JingleError UNSUPPORTED_TRANSPORTS = new JingleError(
            "unsupported-transports");

    // Non standard error messages

    public static final JingleError NO_COMMON_PAYLOAD = new JingleError(
            "unsupported-codecs");

    public static final JingleError NEGOTIATION_ERROR = new JingleError(
            "negotiation-error");

    public static final JingleError MALFORMED_STANZA = new JingleError("malformed-stanza");

    private String message;

    /**
     * Creates a new error with the specified code and message.
     *
     * @param message a message describing the error.
     */
    public JingleError(final String message) {
        this.message = message;
    }

    /**
     * Returns the message describing the error, or null if there is no message.
     *
     * @return the message describing the error, or null if there is no message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the error as XML.
     *
     * @return the error as XML.
     */
    @Override
    public String toXML() {
        StringBuilder buf = new StringBuilder();
        if (message != null) {
            buf.append("<error type=\"cancel\">");
            buf.append('<').append(message).append(" xmlns=\"").append(NAMESPACE).append(
                    "\"/>");
            buf.append("</error>");
        }
        return buf.toString();
    }

    /**
     * Returns a Action instance associated with the String value.
     */
    public static JingleError fromString(String value) {
        if (value != null) {
            value = value.toLowerCase(Locale.US);
            if (value.equals("out-of-order")) {
                return OUT_OF_ORDER;
            } else if (value.equals("unknown-session")) {
                return UNKNOWN_SESSION;
            } else if (value.equals("unsupported-content")) {
                return UNSUPPORTED_CONTENT;
            } else if (value.equals("unsupported-transports")) {
                return UNSUPPORTED_TRANSPORTS;
            } else if (value.equals("unsupported-codecs")) {
                return NO_COMMON_PAYLOAD;
            } else if (value.equals("negotiation-error")) {
                return NEGOTIATION_ERROR;
            } else if (value.equals("malformed-stanza")) {
                return MALFORMED_STANZA;
            }

        }
        return null;
    }

    @Override
    public String toString() {
        return getMessage();
    }

    @Override
    public String getElementName() {
        return message;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public static class Provider extends ExtensionElementProvider<ExtensionElement> {

           private ExtensionElement audioInfo;

           /**
            * Empty constructor.
            */
           public Provider() {
           }

           /**
            * Parse a JingleDescription.Audio extension.
            */
           @Override
           public ExtensionElement parse(XmlPullParser parser, int initialDepth) {
               ExtensionElement result = null;

               if (audioInfo != null) {
                   result = audioInfo;
               } else {
                   String elementName = parser.getName();

                   // Try to get an Audio content info
                   ContentInfo mi = ContentInfo.Audio.fromString(elementName);
                   if (mi != null) {
                       result = new JingleContentInfo.Audio(mi);
                   }
               }
               return result;
           }
    }
}
