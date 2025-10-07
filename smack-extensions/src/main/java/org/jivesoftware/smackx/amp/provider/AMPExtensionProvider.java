/**
 *
 * Copyright 2014 Vyacheslav Blinov
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
package org.jivesoftware.smackx.amp.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.amp.AMPDeliverCondition;
import org.jivesoftware.smackx.amp.AMPExpireAtCondition;
import org.jivesoftware.smackx.amp.AMPMatchResourceCondition;
import org.jivesoftware.smackx.amp.packet.AMPExtension;

import org.jxmpp.JxmppContext;


public class AMPExtensionProvider extends ExtensionElementProvider<AMPExtension> {

    /**
     * Parses a AMPExtension stanza (extension sub-packet).
     *
     * @param parser the XML parser, positioned at the starting element of the extension.
     * @return a PacketExtension.
     * @throws IOException if an I/O error occurred.
     * @throws XmlPullParserException if an error in the XML parser occurred.
     * @throws SmackParsingException if the Smack parser (provider) encountered invalid input.
     */
    @Override
    public AMPExtension parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment, JxmppContext jxmppContext)
                    throws XmlPullParserException, IOException, SmackParsingException {
        final String from = parser.getAttributeValue(null, "from");
        final String to = parser.getAttributeValue(null, "to");
        final String statusString = parser.getAttributeValue(null, "status");
        AMPExtension.Status status = null;
        if (statusString != null) {
            try {
                status = AMPExtension.Status.valueOf(statusString);
            } catch (IllegalArgumentException ex) {
                throw new SmackParsingException("Invalid AMP status: " + statusString, ex);
            }
        }

        AMPExtension ampExtension = new AMPExtension(from, to, status);

        String perHopValue = parser.getAttributeValue(null, "per-hop");
        if (perHopValue != null) {
            boolean perHop = Boolean.parseBoolean(perHopValue);
            ampExtension.setPerHop(perHop);
        }

        boolean done = false;
        while (!done) {
            XmlPullParser.Event eventType = parser.next();
            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                if (parser.getName().equals(AMPExtension.Rule.ELEMENT)) {
                    String actionString = parser.getAttributeValue(null, AMPExtension.Action.ATTRIBUTE_NAME);
                    String conditionName = parser.getAttributeValue(null, AMPExtension.Condition.ATTRIBUTE_NAME);
                    String conditionValue = parser.getAttributeValue(null, "value");

                    AMPExtension.Condition condition = createCondition(conditionName, conditionValue);
                    AMPExtension.Action action = null;
                    if (actionString != null) {
                        try {
                            action = AMPExtension.Action.valueOf(actionString);
                        } catch (IllegalArgumentException ex) {
                            throw new SmackParsingException("Found invalid rule action value " + actionString, ex);
                        }
                    }

                    AMPExtension.Rule rule = new AMPExtension.Rule(action, condition);
                    ampExtension.addRule(rule);
                }
            } else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                if (parser.getName().equals(AMPExtension.ELEMENT)) {
                    done = true;
                }
            }
        }

        return ampExtension;
    }

    private static AMPExtension.Condition createCondition(String name, String value) throws SmackParsingException {
        if (StringUtils.isNullOrEmpty(name)) {
            throw new SmackParsingException("Can't have a condition without a name");
        }
        if (StringUtils.isNullOrEmpty(value)) {
            throw new SmackParsingException("Can't have a condition " + name + " without value");
        }

        if (AMPDeliverCondition.NAME.equals(name)) {
            try {
                return new AMPDeliverCondition(AMPDeliverCondition.Value.valueOf(value));
            } catch (IllegalArgumentException ex) {
                throw new SmackParsingException("Found invalid rule delivery condition value " + value, ex);
            }
        } else if (AMPExpireAtCondition.NAME.equals(name)) {
            return new AMPExpireAtCondition(value);
        } else if (AMPMatchResourceCondition.NAME.equals(name)) {
            try {
                return new AMPMatchResourceCondition(AMPMatchResourceCondition.Value.valueOf(value));
            } catch (IllegalArgumentException ex) {
                throw new SmackParsingException("Found invalid rule match-resource condition value " + value, ex);
            }
        } else {
            throw new SmackParsingException("Found unknown rule condition name " + name);
        }
    }
}
