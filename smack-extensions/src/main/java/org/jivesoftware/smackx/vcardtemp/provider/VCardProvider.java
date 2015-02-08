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

            String name = null;

            outerloop: while (true) {

                int eventType = parser.next();

                switch (eventType) {

                case XmlPullParser.START_TAG:

                    if (initialDepth + 1 == parser.getDepth()) {
                        
                        name = parser.getName();
    
                        switch (name) {
    
                        case "N":
                            parseName(parser, vCard);
                            break;
                        case "ORG":
                            parseOrg(parser, vCard);
                            break;
                        case "TEL":
                            parseTel(parser, vCard);
                            break;
                        case "ADR":
                            parseAddress(parser, vCard);
                            break;
                        case "EMAIL":
                            parseEmail(parser, vCard);
                            break;
                        case "NICKNAME":
                            vCard.setNickName(parser.nextText());
                            break;
                        case "JABBERID":
                            vCard.setJabberId(parser.nextText());
                            break;
                        case "PHOTO":
                            parsePhoto(parser, vCard);
                            break;
                        default:
                            break;
                        }
                    }
                    break;

                case XmlPullParser.TEXT:

                    if (initialDepth + 1 == parser.getDepth()) {
                        extractTextField(vCard, parser, name);
                    }

                    break;

                case XmlPullParser.END_TAG:

                    if (parser.getDepth() == initialDepth) {

                        break outerloop;
                    }
                    break;
                default:
                    break;
                }
            }
        }
        catch (XmlPullParserException e) {
            LOGGER.log(Level.SEVERE, "Exception parsing VCard", e);
        }

        return vCard;

    }

    private void extractTextField(VCard vCard, XmlPullParser parser, String name) {

        if (name == null || parser.getText() == null) {
            return;
        }

        if (name.length() != 0 && parser.getText().length() != 0) {
            vCard.setField(name, parser.getText());
        }
    }

    private void parseAddress(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {

        int initialDepth = parser.getDepth();
        boolean isWork = true;
        outerloop: while (true) {

            int eventType = parser.next();

            switch (eventType) {

            case XmlPullParser.START_TAG:

                String name = parser.getName();

                if ("HOME".equals(name)) {
                    isWork = false;
                }
                else {
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

                break;

            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                break;
            }
        }
    }

    private void parseTel(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {

        int initialDepth = parser.getDepth();
        boolean isWork = true;
        String telLabel = null;

        outerloop: while (true) {

            int eventType = parser.next();

            switch (eventType) {

            case XmlPullParser.START_TAG:

                String name = parser.getName();

                if ("HOME".equals(name)) {
                    isWork = false;
                }
                else {

                    if (telLabel != null && "NUMBER".equals(name)) {
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
                break;

            case XmlPullParser.END_TAG:

                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }

                break;

            default:
                break;
            }

        }
    }

    private void parseOrg(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {

        int initialDepth = parser.getDepth();

        outerloop: while (true) {

            int eventType = parser.next();

            switch (eventType) {

            case XmlPullParser.START_TAG:

                String name = parser.getName();

                switch (name) {

                case "ORGNAME":
                    vCard.setOrganization(parser.nextText());
                    break;
                case "ORGUNIT":
                    vCard.setOrganizationUnit(parser.nextText());
                    break;
                default:
                    break;
                }

                break;
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;

            default:
                break;
            }
        }
    }

    private void parseEmail(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {

        int initialDepth = parser.getDepth();
        boolean isWork = false;

        outerloop: while (true) {
            int eventType = parser.next();

            switch (eventType) {

            case XmlPullParser.START_TAG:

                String name = parser.getName();

                switch (name) {

                case "WORK":
                    isWork = true;
                    break;
                case "USERID":
                    if (isWork) {
                        vCard.setEmailWork(parser.nextText());
                    }
                    else {
                        vCard.setEmailHome(parser.nextText());
                    }
                    break;
                default:
                    break;
                }

            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }

            default:
                break;
            }
        }
    }

    private void parseName(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {

        int initialDepth = parser.getDepth();

        outerloop: while (true) {
            int eventType = parser.next();

            switch (eventType) {

            case XmlPullParser.START_TAG:

                String name = parser.getName();

                switch (name) {

                case "FAMILY":
                    vCard.setLastName(parser.nextText());
                    break;
                case "GIVEN":
                    vCard.setFirstName(parser.nextText());
                    break;
                case "MIDDLE":
                    vCard.setMiddleName(parser.nextText());
                    break;
                default:
                    break;
                }

                break;
            case XmlPullParser.END_TAG:

                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }

                break;
            default:
                break;

            }
        }
    }

    private void parsePhoto(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {

        int initialDepth = parser.getDepth();
        String binval = null;
        String mimetype = null;

        outerloop: while (true) {
            int eventType = parser.next();

            switch (eventType) {

            case XmlPullParser.START_TAG:

                String name = parser.getName();

                switch (name) {

                case "BINVAL":
                    binval = parser.nextText();
                    break;
                case "TYPE":
                    mimetype = parser.nextText();
                    break;
                default:
                    break;
                }
                break;

            case XmlPullParser.END_TAG:

                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                break;
            }
        }

        if (binval == null || mimetype == null)
            return;

        vCard.setAvatar(binval, mimetype);
    }
}
