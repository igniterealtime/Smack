/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
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
package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smackx.packet.*;
import org.xmlpull.v1.XmlPullParser;

/**
 * The MUCUserProvider parses packets with extended presence information about 
 * roles and affiliations.
 *
 * @author Gaston Dombiak
 */
public class MUCUserProvider implements PacketExtensionProvider {

    /**
     * Creates a new MUCUserProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public, no-argument 
     * constructor
     */
    public MUCUserProvider() {
    }

    /**
     * Parses a MUCUser packet (extension sub-packet).
     *
     * @param parser the XML parser, positioned at the starting element of the extension.
     * @return a PacketExtension.
     * @throws Exception if a parsing error occurs.
     */
    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        MUCUser mucUser = new MUCUser();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("invite")) {
                    mucUser.setInvite(parseInvite(parser));
                }
                if (parser.getName().equals("item")) {
                    mucUser.setItem(parseItem(parser));
                }
                if (parser.getName().equals("password")) {
                    mucUser.setPassword(parser.nextText());
                }
                if (parser.getName().equals("status")) {
                    mucUser.setStatus(new MUCUser.Status(parser.getAttributeValue("", "code")));
                }
                if (parser.getName().equals("decline")) {
                    mucUser.setDecline(parseDecline(parser));
                }
                if (parser.getName().equals("destroy")) {
                    mucUser.setDestroy(parseDestroy(parser));
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("x")) {
                    done = true;
                }
            }
        }

        return mucUser;
    }

    private MUCUser.Item parseItem(XmlPullParser parser) throws Exception {
        boolean done = false;
        MUCUser.Item item =
            new MUCUser.Item(
                parser.getAttributeValue("", "affiliation"),
                parser.getAttributeValue("", "role"));
        item.setNick(parser.getAttributeValue("", "nick"));
        item.setJid(parser.getAttributeValue("", "jid"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("actor")) {
                    item.setActor(parser.getAttributeValue("", "jid"));
                }
                if (parser.getName().equals("reason")) {
                    item.setReason(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("item")) {
                    done = true;
                }
            }
        }
        return item;
    }

    private MUCUser.Invite parseInvite(XmlPullParser parser) throws Exception {
        boolean done = false;
        MUCUser.Invite invite = new MUCUser.Invite();
        invite.setFrom(parser.getAttributeValue("", "from"));
        invite.setTo(parser.getAttributeValue("", "to"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("reason")) {
                    invite.setReason(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("invite")) {
                    done = true;
                }
            }
        }
        return invite;
    }

    private MUCUser.Decline parseDecline(XmlPullParser parser) throws Exception {
        boolean done = false;
        MUCUser.Decline decline = new MUCUser.Decline();
        decline.setFrom(parser.getAttributeValue("", "from"));
        decline.setTo(parser.getAttributeValue("", "to"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("reason")) {
                    decline.setReason(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("decline")) {
                    done = true;
                }
            }
        }
        return decline;
    }

    private MUCUser.Destroy parseDestroy(XmlPullParser parser) throws Exception {
        boolean done = false;
        MUCUser.Destroy destroy = new MUCUser.Destroy();
        destroy.setJid(parser.getAttributeValue("", "jid"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("reason")) {
                    destroy.setReason(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("destroy")) {
                    done = true;
                }
            }
        }
        return destroy;
    }
}
