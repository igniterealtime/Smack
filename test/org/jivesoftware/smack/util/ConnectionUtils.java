package org.jivesoftware.smack.util;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPException;

public class ConnectionUtils {

    public static void becomeFriends(Connection con0, Connection con1) throws XMPPException {
        Roster r0 = con0.getRoster();
        Roster r1 = con1.getRoster();
        r0.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
        r1.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
        r0.createEntry(con1.getUser(), "u2", null);
        r1.createEntry(con0.getUser(), "u1", null);
    }

    public static void letsAllBeFriends(Connection[] connections) throws XMPPException {
        for (Connection c1 : connections) {
            for (Connection c2 : connections) {
                if (c1 == c2)
                    continue;
                becomeFriends(c1, c2);
            }
        }
    }
}
