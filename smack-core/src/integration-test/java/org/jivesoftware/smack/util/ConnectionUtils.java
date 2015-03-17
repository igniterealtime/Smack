package org.jivesoftware.smack.util;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPException;

public class ConnectionUtils {

    private ConnectionUtils() {}

    public static void becomeFriends(XMPPConnection con0, XMPPConnection con1) throws XMPPException {
        Roster r0 = con0.getRoster();
        Roster r1 = con1.getRoster();
        r0.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
        r1.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
        r0.createEntry(con1.getUser(), "u2", null);
        r1.createEntry(con0.getUser(), "u1", null);
    }

    public static void letsAllBeFriends(XMPPConnection[] connections) throws XMPPException {
        for (XMPPConnection c1 : connections) {
            for (XMPPConnection c2 : connections) {
                if (c1 == c2)
                    continue;
                becomeFriends(c1, c2);
            }
        }
    }
}
