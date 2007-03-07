/**
 * $RCSfile: ,v $
 * $Revision: $
 * $Date:  $
 *
 * Copyright (C) 1999-2005 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software.
 * Use is subject to license terms.
 */
package org.jivesoftware.smackx.workgroup.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class MonitorPacket extends IQ {

    private String sessionID;

    private boolean isMonitor;

    public boolean isMonitor() {
        return isMonitor;
    }

    public void setMonitor(boolean monitor) {
        isMonitor = monitor;
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "monitor";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    public String getElementName() {
        return ELEMENT_NAME;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();

        buf.append("<").append(ELEMENT_NAME).append(" xmlns=");
        buf.append('"');
        buf.append(NAMESPACE);
        buf.append('"');
        buf.append(">");
        if (sessionID != null) {
            buf.append("<makeOwner sessionID=\""+sessionID+"\"></makeOwner>");
        }
        buf.append("</").append(ELEMENT_NAME).append("> ");
        return buf.toString();
    }


    /**
     * Packet extension provider for Monitor Packets.
     */
    public static class InternalProvider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                throw new IllegalStateException("Parser not in proper position, or bad XML.");
            }

            MonitorPacket packet = new MonitorPacket();

            boolean done = false;


            while (!done) {
                int eventType = parser.next();
                if ((eventType == XmlPullParser.START_TAG) && ("isMonitor".equals(parser.getName()))) {
                    String value = parser.nextText();
                    if ("false".equalsIgnoreCase(value)) {
                        packet.setMonitor(false);
                    }
                    else {
                        packet.setMonitor(true);
                    }
                }
                else if (eventType == XmlPullParser.END_TAG && "monitor".equals(parser.getName())) {
                    done = true;
                }
            }

            return packet;
        }
    }
}
