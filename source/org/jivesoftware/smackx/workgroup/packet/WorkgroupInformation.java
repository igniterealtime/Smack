package org.jivesoftware.smackx.workgroup.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;


/**
 * A packet extension that contains information about the user and agent in a
 * workgroup chat. The packet extension is attached to group chat invitations.
 */
public class WorkgroupInformation implements PacketExtension {

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "workgroup";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "xmpp:workgroup";

    private String userID;
    private String agentID;

    protected WorkgroupInformation(String userID, String agentID) {
        this.userID = userID;
        this.agentID = agentID;
    }

    public String getUserID() {
        return userID;
    }

    public String getAgentID() {
        return agentID;
    }

    public String getElementName() {
        return ELEMENT_NAME;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();

        buf.append('<').append(ELEMENT_NAME);
        buf.append(" user=\"").append(userID).append("\"");
        buf.append(" agent=\"").append(agentID);
        buf.append("\" xmlns=\"").append(NAMESPACE).append("\" />");

        return buf.toString();
    }

    public static class Provider implements PacketExtensionProvider {

        /**
         * PacketExtensionProvider implementation
         */
        public PacketExtension parseExtension (XmlPullParser parser)
            throws Exception {
            String user = parser.getAttributeValue("", "user");
            String agent = parser.getAttributeValue("", "agent");

            // since this is a start and end tag, and we arrive on the start, this should guarantee
            //      we leave on the end
            parser.next();

            return new WorkgroupInformation(user, agent);
        }
    }
}