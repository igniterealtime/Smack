package org.jivesoftware.smackx.workgroup.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.*;
import java.text.SimpleDateFormat;

import org.jivesoftware.smackx.workgroup.QueueUser;

/**
 * Queue details packet extension, which contains details about the users
 * currently in a queue.
 */
public class QueueDetails implements PacketExtension {

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "notify-queue-details";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "xmpp:workgroup";

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");

    /**
     * The list of users in the queue.
     */
    private Set users;

    /**
     * Creates a new QueueDetails packet
     */
    private QueueDetails() {
        users = new HashSet();
    }

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
    public Set getUsers() {
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

    public String getElementName() {
        return ELEMENT_NAME;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<").append(ELEMENT_NAME).append(" xmlns=\"").append(NAMESPACE).append("\">");

        synchronized (users) {
            for (Iterator i=users.iterator(); i.hasNext(); ) {
                QueueUser user = (QueueUser)i.next();
                int position = user.getQueuePosition();
                int timeRemaining = user.getEstimatedRemainingTime();
                Date timestamp = user.getQueueJoinTimestamp();

                buf.append("<user jid=\"").append(user.getUserID()).append(">");

                if (position != -1) {
                    buf.append("<position>").append(position).append("</position>");
                }

                if (timeRemaining != -1) {
                    buf.append("<time>").append(timeRemaining).append("</time>");
                }

                if (timestamp != null) {
                    buf.append("<join-time>");
                    buf.append(DATE_FORMATTER.format(timestamp));
                    buf.append("</join-time>");
                }

                buf.append("</user>");
            }
        }
        buf.append("</").append(ELEMENT_NAME).append(">");
        return buf.toString();
    }

    /**
     * Provider class for QueueDetails packet extensions.
     */
    public static class Provider implements PacketExtensionProvider {

        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
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
                                || (! "user".equals(parser.getName())))
                    {
                        if ("position".equals(parser.getName())) {
                            position = Integer.parseInt(parser.nextText());
                        }
                        else if ("time".equals(parser.getName())) {
                            time = Integer.parseInt(parser.nextText());
                        }
                        else if ("join-time".equals(parser.getName())) {
                            joinTime = DATE_FORMATTER.parse(parser.nextText());
                        }
                        else if( parser.getName().equals( "waitTime" ) ) {
                          Date wait = DATE_FORMATTER.parse( parser.nextText() );
                          System.out.println( wait );
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