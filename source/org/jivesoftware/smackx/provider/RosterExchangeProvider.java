/*
 * Created on 27/07/2003
 *
 */
package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.packet.*;
import org.xmlpull.v1.XmlPullParser;

/**
 *
 * The RosterExchangeProvider parses RosterExchange packets.
 *
 * @author Gaston Dombiak
 */
public class RosterExchangeProvider implements PacketExtensionProvider {

    /**
     * Creates a new RosterExchangeProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public, no-argument constructor
     */
    public RosterExchangeProvider() {
    }

    /**
     * Parses a RosterExchange packet (extension sub-packet).
     *
     * @param parser the XML parser, positioned at the starting element of the extension.
     * @return a PacketExtension.
     * @throws Exception if a parsing error occurs.
     */
    public PacketExtension parseExtension(XmlPullParser parser)
        throws Exception {

        RosterExchange rosterExchange = new RosterExchange();
        boolean done = false;
        RosterExchange.Item item = null;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("item")) {
                    String jid = parser.getAttributeValue("", "jid");
                    String name = parser.getAttributeValue("", "name");
                    // Create packet.
                    item = new RosterExchange.Item(jid, name);
                }
                if (parser.getName().equals("group")) {
                    String groupName = parser.nextText();
                    item.addGroupName(groupName);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("item")) {
                    rosterExchange.addRosterItem(item);
                }
                if (parser.getName().equals("x")) {
                    done = true;
                }
            }
        }

        return rosterExchange;

    }

}
