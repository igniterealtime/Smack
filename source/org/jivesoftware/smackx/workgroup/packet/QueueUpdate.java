package org.jivesoftware.smackx.workgroup.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * An IQ packet that encapsulates both types of workgroup queue
 * status notifications -- position updates, and estimated time
 * left in the queue updates.
 */
public class QueueUpdate extends IQ {

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "queue-status";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "xmpp:workgroup";

    private int position;
    private int remainingTime;

    public QueueUpdate(int position, int remainingTime) {
        this.position = position;
        this.remainingTime = remainingTime;
    }

    /**
     * Returns the user's position in the workgroup queue, or -1 if the
     * value isn't set on this packet.
     *
     * @return the position in the workgroup queue.
     */
    public int getPosition() {
        return this.position;
    }

    /**
     * Returns the user's estimated time left in the workgroup queue, or
     * -1 if the value isn't set on this packet.
     *
     * @return the estimated time left in the workgroup queue.
     */
    public int getRemaingTime() {
        return remainingTime;
    }

    public String getChildElementXML () {
        StringBuffer buf = new StringBuffer();
        buf.append("<queue-status xmlns=\"xmpp:workgroup\">");
        if (position != -1) {
            buf.append("<queue-position>").append(position).append("</queue-position>");
        }
        else if (remainingTime != -1) {
            buf.append("<queue-time>").append(remainingTime).append("</queue-time>");
        }
        buf.append("</queue-status>");
        return buf.toString();
    }

    public static class Provider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            boolean done = false;
            int position = -1;
            int timeRemaining = -1;
            while (!done) {
                parser.next();
                String elementName = parser.getName();
                if (parser.getEventType() == XmlPullParser.START_TAG && "position".equals(elementName)) {
                    try {
                        position = Integer.parseInt(parser.nextText());
                    }
                    catch (NumberFormatException nfe) { }
                }
                else if (parser.getEventType() == XmlPullParser.START_TAG && "time".equals(elementName)) {
                    try {
                        timeRemaining = Integer.parseInt(parser.nextText());
                    }
                    catch (NumberFormatException nfe) { }
                }
                else if (parser.getEventType() == XmlPullParser.END_TAG && "queue-status".equals(elementName)) {
                    done = true;
                }
            }
            return new QueueUpdate(position, timeRemaining);
        }
    }
}