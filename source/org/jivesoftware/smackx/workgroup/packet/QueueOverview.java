/**
* $RCSfile$
* $Revision$
* $Date$
*
* Copyright (C) 2002-2004 Jive Software. All rights reserved.
* ====================================================================
* The Jive Software License (based on Apache Software License, Version 1.1)
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution,
*    if any, must include the following acknowledgment:
*       "This product includes software developed by
*        Jive Software (http://www.jivesoftware.com)."
*    Alternately, this acknowledgment may appear in the software itself,
*    if and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Smack" and "Jive Software" must not be used to
*    endorse or promote products derived from this software without
*    prior written permission. For written permission, please
*    contact webmaster@jivesoftware.com.
*
* 5. Products derived from this software may not be called "Smack",
*    nor may "Smack" appear in their name, without prior written
*    permission of Jive Software.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
*/

package org.jivesoftware.smackx.workgroup.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.Date;
import java.text.SimpleDateFormat;

import org.jivesoftware.smackx.workgroup.agent.WorkgroupQueue;

public class QueueOverview implements PacketExtension {

    /**
     * Element name of the packet extension.
     */
    public static String ELEMENT_NAME = "notify-queue";

    /**
     * Namespace of the packet extension.
     */
    public static String NAMESPACE = "xmpp:workgroup";

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");

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

    public String getElementName () {
        return ELEMENT_NAME;
    }

    public String getNamespace () {
        return NAMESPACE;
    }

    public String toXML () {
        StringBuffer buf = new StringBuffer();
        buf.append("<").append(ELEMENT_NAME).append(" xmlns=\"").append(NAMESPACE).append("\">");

        if (userCount != -1) {
            buf.append("<count>").append(userCount).append("</count>");
        }
        if (oldestEntry != null) {
            buf.append("<oldest>").append(DATE_FORMATTER.format(oldestEntry)).append("</oldest>");
        }
        if (averageWaitTime != -1) {
            buf.append("<time>").append(averageWaitTime).append("</time>");
        }
        if (status != null) {
            buf.append("<status>").append(status).append("</status>");
        }
        buf.append("</").append(ELEMENT_NAME).append(">");

        return buf.toString();
    }

    public static class Provider implements PacketExtensionProvider {

        public PacketExtension parseExtension (XmlPullParser parser) throws Exception {
            int eventType = parser.getEventType();
            QueueOverview queueOverview = new QueueOverview();

            if (eventType != XmlPullParser.START_TAG) {
                // throw exception
            }

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
                    queueOverview.setOldestEntry((DATE_FORMATTER.parse(parser.nextText())));
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