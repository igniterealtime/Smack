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

import java.io.IOException;

import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.vcardtemp.packet.VCard;

/**
 * vCard provider.
 *
 * @author Gaston Dombiak
 * @author Derek DeMoro
 * @author Chris Deering
 */
public class VCardProvider extends IqProvider<VCard> {

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
    public VCard parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
        VCard vCard = new VCard();
        String name = null;

        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
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
            case TEXT_CHARACTERS:
                if (initialDepth + 1 == parser.getDepth()) {
                    String text = parser.getText();
                    vCard.setField(name, text);
                }
                break;
            case END_ELEMENT:
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
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
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
            case END_ELEMENT:
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
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
                String name = parser.getName();
                if ("HOME".equals(name)) {
                    isWork = false;
                }
                else {
                    if ("NUMBER".equals(name)) {
                        if (StringUtils.isNullOrEmpty(telLabel)) {
                            // RFC 2426 § 3.3.1:
                            // "The default type is 'voice'"
                            telLabel = "VOICE";
                        }
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
            case END_ELEMENT:
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
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
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
            case END_ELEMENT:
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
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
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
            case END_ELEMENT:
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
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
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
                case "PREFIX":
                    vCard.setPrefix(parser.nextText());
                    break;
                case "SUFFIX":
                    vCard.setSuffix(parser.nextText());
                    break;
                default:
                    break;
                }
                break;
            case END_ELEMENT:
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
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
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
            case END_ELEMENT:
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
