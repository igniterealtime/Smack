/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.workgroup.packet;

import org.jivesoftware.smackx.workgroup.QueueUser;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Queue details stanza(/packet) extension, which contains details about the users
 * currently in a queue.
 */
public final class QueueDetails implements ExtensionElement {
    private static final Logger LOGGER = Logger.getLogger(QueueDetails.class.getName());

    /**
     * Element name of the stanza(/packet) extension.
     */
    public static final String ELEMENT_NAME = "notify-queue-details";

    /**
     * Namespace of the stanza(/packet) extension.
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/workgroup";

    private static final String DATE_FORMAT = "yyyyMMdd'T'HH:mm:ss";

    private SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    /**
     * The list of users in the queue.
     */
    private final Set<QueueUser> users = new HashSet<>();

    /**
     * Returns the number of users currently in the queue that are waiting to
     * be routed to an agent.
     *
     * @return the number of users in the queue.
     */
    public int getUserCount() {
        return users.size();
    }

    /**
     * Returns the set of users in the queue that are waiting to
     * be routed to an agent (as QueueUser objects).
     *
     * @return a Set for the users waiting in a queue.
     */
    public Set<QueueUser> getUsers() {
        synchronized (users) {
            return users;
        }
    }

    /**
     * Adds a user to the packet.
     *
     * @param user the user.
     */
    private void addUser(QueueUser user) {
        synchronized (users) {
            users.add(user);
        }
    }

    @Override
    public String getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append('<').append(ELEMENT_NAME).append(" xmlns=\"").append(NAMESPACE).append("\">");

        synchronized (users) {
            for (Iterator<QueueUser> i=users.iterator(); i.hasNext(); ) {
                QueueUser user = i.next();
                int position = user.getQueuePosition();
                int timeRemaining = user.getEstimatedRemainingTime();
                Date timestamp = user.getQueueJoinTimestamp();

                buf.append("<user jid=\"").append(user.getUserID()).append("\">");

                if (position != -1) {
                    buf.append("<position>").append(position).append("</position>");
                }

                if (timeRemaining != -1) {
                    buf.append("<time>").append(timeRemaining).append("</time>");
                }

                if (timestamp != null) {
                    buf.append("<join-time>");
                    buf.append(dateFormat.format(timestamp));
                    buf.append("</join-time>");
                }

                buf.append("</user>");
            }
        }
        buf.append("</").append(ELEMENT_NAME).append('>');
        return buf.toString();
    }

    /**
     * Provider class for QueueDetails stanza(/packet) extensions.
     */
    public static class Provider extends ExtensionElementProvider<QueueDetails> {

        @Override
        public QueueDetails parse(XmlPullParser parser,
                        int initialDepth) throws XmlPullParserException,
                        IOException, SmackException {

            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            QueueDetails queueDetails = new QueueDetails();

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_TAG &&
                    "notify-queue-details".equals(parser.getName()))
            {
                eventType = parser.next();
                while ((eventType == XmlPullParser.START_TAG) && "user".equals(parser.getName())) {
                    String uid = null;
                    int position = -1;
                    int time = -1;
                    Date joinTime = null;

                    uid = parser.getAttributeValue("", "jid");

                    if (uid == null) {
                        // throw exception
                    }

                    eventType = parser.next();
                    while ((eventType != XmlPullParser.END_TAG)
                                || (!"user".equals(parser.getName())))
                    {
                        if ("position".equals(parser.getName())) {
                            position = Integer.parseInt(parser.nextText());
                        }
                        else if ("time".equals(parser.getName())) {
                            time = Integer.parseInt(parser.nextText());
                        }
                        else if ("join-time".equals(parser.getName())) {
                            try {
                                joinTime = dateFormat.parse(parser.nextText());
                            } catch (ParseException e) {
                                throw new SmackException(e);
                            }
                        }
                        else if(parser.getName().equals("waitTime")) {
                            Date wait;
                            try {
                                wait = dateFormat.parse(parser.nextText());
                            } catch (ParseException e) {
                                throw new SmackException(e);
                            }
                            LOGGER.fine(wait.toString());
                        }

                        eventType = parser.next();

                        if (eventType != XmlPullParser.END_TAG) {
                            // throw exception
                        }
                    }

                    queueDetails.addUser(new QueueUser(uid, position, time, joinTime));

                    eventType = parser.next();
                }
            }
            return queueDetails;
        }
    }
}
