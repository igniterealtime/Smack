/**
 *
 * Copyright 2003-2007 Jive Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.vcardtemp.provider;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * vCard provider.
 * 
 * @author Gaston Dombiak
 * @author Derek DeMoro
 * @author Chris Deering
 */

public class VCardProvider extends IQProvider<VCard> {
    private static final Logger LOGGER = Logger.getLogger(VCardProvider.class.getName());

    private enum Adr {
        POSTAL,
        PARCEL,
        DOM,
        INTL,
        PREF,
        POBOX,
        EXTADR,
        STREET,
        LOCALITY,
        REGION,
        PCODE,
        CTRY,
        FF
    }

    private enum Tel {
        VOICE,
        FAX,
        PAGER,
        MSG,
        CELL,
        VIDEO,
        BBS,
        MODEM,
        ISDN,
        PCS,
        PREF
    }

    @Override
    public VCard parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException,
                    SmackException {

        VCard vCard = new VCard();

        try {

            boolean done = false;
            boolean lastEventWasStart = false;
            int depth = 0;
            String name = "";
            String text = "";

            while (!done) {
                int eventType = parser.next();

                if (eventType == XmlPullParser.START_TAG) {

                    lastEventWasStart = true;

                    name = parser.getName();

                    if (name.equals("N")) {
                        parseName(parser, vCard);
                    }
                    else if (name.equals("NICKNAME")) {
                        vCard.setNickName(parser.nextText());
                    }
                    else if (name.equals("FN")) {
                        vCard.setField("FN", parser.nextText());
                    }
                    else if (name.equals("URL")) {
                        vCard.setField("URL", parser.nextText());
                    }
                    else if (name.equals("BDAY")) {
                        vCard.setField("BDAY", parser.nextText());
                    }
                    else if (name.equals("ORG")) {
                        parseOrg(parser, vCard);
                    }
                    else if (name.equals("TITLE")) {
                        vCard.setField("TITLE", parser.nextText());
                    }
                    else if (name.equals("ROLE")) {
                        vCard.setField("ROLE", parser.nextText());
                    }
                    else if (name.equals("TEL")) {
                        parseTel(parser, vCard);
                    }
                    else if (name.equals("ADR")) {
                        parseAddress(parser, vCard);
                    }
                    else if (name.equals("EMAIL")) {
                        parseEmail(parser, vCard);
                    }
                    else if (name.equals("JABBERID")) {
                        vCard.setJabberId(parser.nextText());
                    }
                    else if (name.equals("DESC")) {
                        vCard.setField("DESC", parser.nextText());
                    }
                    else if (name.equals("PHOTO")) {
                        parsePhoto(parser, vCard);
                    }
                    else {
                        text = parser.nextText();
                    }

                    depth++;
                }
                else if (eventType == XmlPullParser.END_TAG) {

                    depth--;

                    if (lastEventWasStart && 0 == depth && !name.isEmpty() && !text.isEmpty()) {

                        vCard.setField(name, text);
                    }

                    name = "";
                    text = "";
                    lastEventWasStart = false;

                    if (parser.getName().equals("vCard")) {
                        done = true;
                    }
                }
            }
        }
        catch (XmlPullParserException e) {
            LOGGER.log(Level.SEVERE, "Exception parsing VCard", e);
        }

        return vCard;

    }

    private void parseAddress(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {

        boolean isWork = true;
        boolean done = false;
        while (!done) {

            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("HOME")) {
                isWork = false;
            }

            else if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();

                for (Adr a : Adr.class.getEnumConstants()) {

                    if (a.name().equals(name)) {

                        if (isWork) {
                            vCard.setAddressFieldWork(name, parser.nextText());
                        }
                        else {
                            vCard.setAddressFieldHome(name, parser.nextText());
                        }
                    }
                }
            }

            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("ADR")) {
                    done = true;
                }
            }
        }
    }

    private void parseTel(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {

        boolean isWork = true;
        String telLabel = "";
        boolean done = false;
        while (!done) {

            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("HOME")) {
                isWork = false;
            }

            else if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();

                if (!telLabel.isEmpty() && "NUMBER".equals(name)) {
                    if (isWork) {
                        vCard.setPhoneWork(telLabel, parser.nextText());
                    }
                    else {
                        vCard.setPhoneHome(telLabel, parser.nextText());
                    }
                }
                else {
                    for (Tel t : Tel.class.getEnumConstants()) {

                        if (t.name().equals(name)) {
                            telLabel = name;
                        }
                    }
                }
            }

            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("TEL")) {
                    done = true;
                }
            }
        }
    }

    private void parseOrg(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {

        boolean done = false;
        while (!done) {

            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("ORGNAME")) {
                vCard.setOrganization(parser.nextText());
            }

            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("ORGUNIT")) {
                vCard.setOrganizationUnit(parser.nextText());
            }

            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("ORG")) {
                    done = true;
                }
            }
        }
    }

    private void parseEmail(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {

        boolean isWork = false;
        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("WORK")) {
                isWork = true;
            }

            else if (eventType == XmlPullParser.START_TAG && parser.getName().equals("USERID")) {
                if (isWork) {
                    vCard.setEmailWork(parser.nextText());
                }
                else {
                    vCard.setEmailHome(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("EMAIL")) {
                    done = true;
                }
            }
        }
    }

    private void parseName(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {

        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("FAMILY")) {
                vCard.setLastName(parser.nextText());
            }

            else if (eventType == XmlPullParser.START_TAG && parser.getName().equals("GIVEN")) {
                vCard.setFirstName(parser.nextText());
            }

            else if (eventType == XmlPullParser.START_TAG && parser.getName().equals("MIDDLE")) {
                vCard.setMiddleName(parser.nextText());
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("N")) {
                    done = true;
                }
            }
        }
    }

    private void parsePhoto(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {

        String binval = null;
        String mimetype = null;

        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("BINVAL")) {
                binval = parser.nextText();
            }

            else if (eventType == XmlPullParser.START_TAG && parser.getName().equals("TYPE")) {
                mimetype = parser.nextText();
            }

            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("PHOTO")) {
                    done = true;
                }
            }
        }

        if (binval == null || mimetype == null)
            return;

        vCard.setAvatar(binval, mimetype);

    }
}
