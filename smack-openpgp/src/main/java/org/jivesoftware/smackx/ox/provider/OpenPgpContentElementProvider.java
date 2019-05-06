/**
 *
 * Copyright 2017-2019 Florian Schmaus, 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.provider;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.EncryptedOpenPgpContentElement;
import org.jivesoftware.smackx.ox.element.OpenPgpContentElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

/**
 * Abstract {@link ExtensionElementProvider} implementation for the also abstract {@link OpenPgpContentElement}.
 *
 * @param <O> Specialized subclass of {@link OpenPgpContentElement}.
 */
public abstract class OpenPgpContentElementProvider<O extends OpenPgpContentElement> extends ExtensionElementProvider<O> {

    private static final Logger LOGGER = Logger.getLogger(OpenPgpContentElementProvider.class.getName());

    public static OpenPgpContentElement parseOpenPgpContentElement(String element)
            throws XmlPullParserException, IOException {
        XmlPullParser parser = PacketParserUtils.getParserFor(element);
        return parseOpenPgpContentElement(parser);
    }

    public static OpenPgpContentElement parseOpenPgpContentElement(XmlPullParser parser)
            throws  XmlPullParserException {
        try {
            switch (parser.getName()) {
                case SigncryptElement.ELEMENT:
                    return SigncryptElementProvider.INSTANCE.parse(parser);
                case SignElement.ELEMENT:
                    return SignElementProvider.INSTANCE.parse(parser);
                case CryptElement.ELEMENT:
                    return CryptElementProvider.INSTANCE.parse(parser);
                default: throw new XmlPullParserException("Expected <crypt/>, <sign/> or <signcrypt/> element, " +
                        "but got neither of them.");
            }
        } catch (Exception e) {
            throw new XmlPullParserException(e.getMessage());
        }
    }

    @Override
    public abstract O parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException;

    protected static OpenPgpContentElementData parseOpenPgpContentElementData(XmlPullParser parser, int initialDepth)
            throws XmlPullParserException, IOException, SmackParsingException {
        Set<Jid> to = new HashSet<>();
        Date timestamp = null;
        String rpad = null;
        List<ExtensionElement> payload = new LinkedList<>();

        outerloop: while (true) {
            XmlPullParser.Event tag = parser.next();
            String name = parser.getName();
            switch (tag) {
                case START_ELEMENT:
                    switch (name) {

                        case OpenPgpContentElement.ELEM_TIME:
                            String stamp = parser.getAttributeValue("", OpenPgpContentElement.ATTR_STAMP);
                            timestamp = ParserUtils.getDateFromXep82String(stamp);
                            break;

                        case OpenPgpContentElement.ELEM_TO:
                            String jid = parser.getAttributeValue("", OpenPgpContentElement.ATTR_JID);
                            to.add(JidCreate.bareFrom(jid));
                            break;

                        case EncryptedOpenPgpContentElement.ELEM_RPAD:
                            rpad = parser.nextText();
                            break;

                        case OpenPgpContentElement.ELEM_PAYLOAD:
                            innerloop: while (true) {
                                XmlPullParser.Event ptag = parser.next();
                                String pname = parser.getName();
                                String pns = parser.getNamespace();
                                switch (ptag) {
                                    case START_ELEMENT:
                                        ExtensionElementProvider<ExtensionElement> provider =
                                                ProviderManager.getExtensionProvider(pname, pns);
                                        if (provider == null) {
                                            LOGGER.log(Level.INFO, "No provider found for " + pname + " " + pns);
                                            continue innerloop;
                                        }
                                        payload.add(provider.parse(parser));
                                        break;

                                    case END_ELEMENT:
                                        break innerloop;

                                    default:
                                        // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                                        break;
                                }
                            }
                            break;
                    }
                    break;

                case END_ELEMENT:
                    switch (name) {
                        case CryptElement.ELEMENT:
                        case SigncryptElement.ELEMENT:
                        case SignElement.ELEMENT:
                            break outerloop;
                    }
                    break;

                default:
                    // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                    break;
            }
        }

        return new OpenPgpContentElementData(to, timestamp, rpad, payload);
    }

    protected static final class OpenPgpContentElementData {
        protected final Set<Jid> to;
        protected final Date timestamp;
        protected final String rpad;
        protected final List<ExtensionElement> payload;

        private OpenPgpContentElementData(Set<Jid> to, Date timestamp, String rpad, List<ExtensionElement> payload) {
            this.to = to;
            this.timestamp = timestamp;
            this.rpad = rpad;
            this.payload = payload;
        }
    }
}
