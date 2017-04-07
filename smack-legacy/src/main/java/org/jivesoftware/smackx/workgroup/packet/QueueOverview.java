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

import org.jivesoftware.smackx.workgroup.agent.WorkgroupQueue;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class QueueOverview implements ExtensionElement {

    /**
     * Element name of the stanza(/packet) extension.
     */
    public static String ELEMENT_NAME = "notify-queue";

    /**
     * Namespace of the stanza(/packet) extension.
     */
    public static String NAMESPACE = "http://jabber.org/protocol/workgroup";

    private static final String DATE_FORMAT = "yyyyMMdd'T'HH:mm:ss";
    private SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

    private int averageWaitTime;
    private Date oldestEntry;
    private int userCount;
    private WorkgroupQueue.Status status;

    QueueOverview() {
        this.averageWaitTime = -1;
        this.oldestEntry = null;
        this.userCount = -1;
        this.status = null;
    }

    void setAverageWaitTime(int averageWaitTime) {
        this.averageWaitTime = averageWaitTime;
    }

    public int getAverageWaitTime () {
        return averageWaitTime;
    }

    void setOldestEntry(Date oldestEntry) {
        this.oldestEntry = oldestEntry;
    }

    public Date getOldestEntry() {
        return oldestEntry;
    }

    void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    public int getUserCount() {
        return userCount;
    }

    public WorkgroupQueue.Status getStatus() {
        return status;
    }

    void setStatus(WorkgroupQueue.Status status) {
        this.status = status;
    }

    @Override
    public String getElementName () {
        return ELEMENT_NAME;
    }

    @Override
    public String getNamespace () {
        return NAMESPACE;
    }

    @Override
    public String toXML () {
        StringBuilder buf = new StringBuilder();
        buf.append('<').append(ELEMENT_NAME).append(" xmlns=\"").append(NAMESPACE).append("\">");

        if (userCount != -1) {
            buf.append("<count>").append(userCount).append("</count>");
        }
        if (oldestEntry != null) {
            buf.append("<oldest>").append(dateFormat.format(oldestEntry)).append("</oldest>");
        }
        if (averageWaitTime != -1) {
            buf.append("<time>").append(averageWaitTime).append("</time>");
        }
        if (status != null) {
            buf.append("<status>").append(status).append("</status>");
        }
        buf.append("</").append(ELEMENT_NAME).append('>');

        return buf.toString();
    }

    public static class Provider extends ExtensionElementProvider<QueueOverview> {

        @Override
        public QueueOverview parse(XmlPullParser parser,
                        int initialDepth) throws XmlPullParserException,
                        IOException, SmackException {
            int eventType = parser.getEventType();
            QueueOverview queueOverview = new QueueOverview();            
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

            eventType = parser.next();
            while ((eventType != XmlPullParser.END_TAG)
                         || (!ELEMENT_NAME.equals(parser.getName())))
            {
                if ("count".equals(parser.getName())) {
                    queueOverview.setUserCount(Integer.parseInt(parser.nextText()));
                }
                else if ("time".equals(parser.getName())) {
                    queueOverview.setAverageWaitTime(Integer.parseInt(parser.nextText()));
                }
                else if ("oldest".equals(parser.getName())) {
                    try {
                        queueOverview.setOldestEntry((dateFormat.parse(parser.nextText())));
                    } catch (ParseException e) {
                        throw new SmackException(e);
                    }
                }
                else if ("status".equals(parser.getName())) {
                    queueOverview.setStatus(WorkgroupQueue.Status.fromString(parser.nextText()));
                }

                eventType = parser.next();

                if (eventType != XmlPullParser.END_TAG) {
                    // throw exception
                }
            }

            if (eventType != XmlPullParser.END_TAG) {
                // throw exception
            }

            return queueOverview;
        }
    }
}
