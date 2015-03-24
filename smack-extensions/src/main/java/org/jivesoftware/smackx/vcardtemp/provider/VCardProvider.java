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

/**
 * vCard provider.
 *
 * @author Gaston Dombiak
 * @author Derek DeMoro
 * @author Chris Deering
 */
public class VCardProvider extends IQProvider<VCard> {

    // @formatter:off
    private static final String[] ADR = new String[] {
        "POSTAL",
        "PARCEL",
        "DOM",
        "INTL",
        "PREF",
        "POBOX",
        "EXTADR",
        "STREET",
        "LOCALITY",
        "REGION",
        "PCODE",
        "CTRY",
        "FF",
    };

    private static final String[] TEL = new String[] {
        "VOICE",
        "FAX",
        "PAGER",
        "MSG",
        "CELL",
        "VIDEO",
        "BBS",
        "MODEM",
        "ISDN",
        "PCS",
        "PREF",
    };
    // @formatter:on

    @Override
    public VCard parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException,
                    SmackException {
        VCard vCard = new VCard();
        String name = null;

        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
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
                break;
            case XmlPullParser.TEXT:
                if (initialDepth + 1 == parser.getDepth()) {
                    vCard.setField(name, parser.getText());
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

        return vCard;
    }

    private static void parseAddress(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {
        final int initialDepth = parser.getDepth();
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
                    for (String adr : ADR) {
                        if (adr.equals(name)) {
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

    private static void parseTel(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {
        final int initialDepth = parser.getDepth();
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
                        for (String tel : TEL) {
                            if (tel.equals(name)) {
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

    private static void parseOrg(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {
        final int initialDepth = parser.getDepth();

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

    private static void parseEmail(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {
        final int initialDepth = parser.getDepth();
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

    private static void parseName(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {
        final int initialDepth = parser.getDepth();

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

    private static void parsePhoto(XmlPullParser parser, VCard vCard) throws XmlPullParserException, IOException {
        final int initialDepth = parser.getDepth();
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

        if (binval == null || mimetype == null) {
            return;
        }

        vCard.setAvatar(binval, mimetype);
    }
}
