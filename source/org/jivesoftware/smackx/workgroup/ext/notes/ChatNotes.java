/**
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.workgroup.ext.notes;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * IQ packet for retrieving and adding Chat Notes.
 */
public class ChatNotes extends IQ {

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "chat-notes";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";


    private String sessionID;
    private String notes;

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();

        buf.append("<").append(ELEMENT_NAME).append(" xmlns=\"").append(NAMESPACE).append("\">");
        buf.append("<sessionID>").append(getSessionID()).append("</sessionID>");

        if (getNotes() != null) {
            buf.append("<notes>").append(getNotes()).append("</notes>");
        }
        buf.append("</").append(ELEMENT_NAME).append("> ");

        return buf.toString();
    }

    /**
     * An IQProvider for ChatNotes packets.
     *
     * @author Derek DeMoro
     */
    public static class Provider implements IQProvider {

        public Provider() {
            super();
        }

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            ChatNotes chatNotes = new ChatNotes();

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("sessionID")) {
                        chatNotes.setSessionID(parser.nextText());
                    }
                    else if (parser.getName().equals("text")) {
                        String note = parser.nextText();
                        note = note.replaceAll("\\\\n", "\n");
                        chatNotes.setNotes(note);
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals(ELEMENT_NAME)) {
                        done = true;
                    }
                }
            }

            return chatNotes;
        }
    }

    /**
     * Replaces all instances of oldString with newString in string.
     *
     * @param string    the String to search to perform replacements on
     * @param oldString the String that should be replaced by newString
     * @param newString the String that will replace all instances of oldString
     * @return a String will all instances of oldString replaced by newString
     */
    public static final String replace(String string, String oldString, String newString) {
        if (string == null) {
            return null;
        }
        // If the newString is null or zero length, just return the string since there's nothing
        // to replace.
        if (newString == null) {
            return string;
        }
        int i = 0;
        // Make sure that oldString appears at least once before doing any processing.
        if ((i = string.indexOf(oldString, i)) >= 0) {
            // Use char []'s, as they are more efficient to deal with.
            char[] string2 = string.toCharArray();
            char[] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuilder buf = new StringBuilder(string2.length);
            buf.append(string2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            // Replace all remaining instances of oldString with newString.
            while ((i = string.indexOf(oldString, i)) > 0) {
                buf.append(string2, j, i - j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(string2, j, string2.length - j);
            return buf.toString();
        }
        return string;
    }
}



