package org.jivesoftware.smackx.ibb;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;

/**
 * Utility methods to create packets.
 * 
 * @author Henning Staib
 */
public class IBBPacketUtils {

    /**
     * Returns an error IQ.
     * 
     * @param from the senders JID
     * @param to the recipients JID
     * @param xmppError the XMPP error
     * @return an error IQ
     */
    public static IQ createErrorIQ(String from, String to, XMPPError xmppError) {
        IQ errorIQ = new IQ() {

            public String getChildElementXML() {
                return null;
            }

        };
        errorIQ.setType(IQ.Type.ERROR);
        errorIQ.setFrom(from);
        errorIQ.setTo(to);
        errorIQ.setError(xmppError);
        return errorIQ;
    }

    /**
     * Returns a result IQ.
     * 
     * @param from the senders JID
     * @param to the recipients JID
     * @return a result IQ
     */
    public static IQ createResultIQ(String from, String to) {
        IQ result = new IQ() {

            public String getChildElementXML() {
                return null;
            }

        };
        result.setType(IQ.Type.RESULT);
        result.setFrom(from);
        result.setTo(to);
        return result;
    }

}
