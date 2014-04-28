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

import java.util.logging.Logger;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.amp.AMPDeliverCondition;
import org.jivesoftware.smackx.amp.AMPExpireAtCondition;
import org.jivesoftware.smackx.amp.AMPMatchResourceCondition;
import org.jivesoftware.smackx.amp.packet.AMPExtension;
import org.xmlpull.v1.XmlPullParser;


public class AMPExtensionProvider implements PacketExtensionProvider {
    private static final Logger LOGGER = Logger.getLogger(AMPExtensionProvider.class.getName());

    /**
     * Creates a new AMPExtensionProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public, no-argument constructor
     */
    public AMPExtensionProvider() {}

    /**
     * Parses a AMPExtension packet (extension sub-packet).
     *
     * @param parser the XML parser, positioned at the starting element of the extension.
     * @return a PacketExtension.
     * @throws Exception if a parsing error occurs.
     */
    @Override
    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        final String from = parser.getAttributeValue(null, "from");
        final String to = parser.getAttributeValue(null, "to");
        final String statusString = parser.getAttributeValue(null, "status");
        AMPExtension.Status status = null;
        if (statusString != null) {
            try {
                status = AMPExtension.Status.valueOf(statusString);
            } catch (IllegalArgumentException ex) {
                LOGGER.severe("Found invalid amp status " + statusString);
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
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
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
                            LOGGER.severe("Found invalid rule action value " + actionString);
                        }
                    }

                    if (action == null || condition == null) {
                        LOGGER.severe("Rule is skipped because either it's action or it's condition is invalid");
                    } else {
                        AMPExtension.Rule rule = new AMPExtension.Rule(action, condition);
                        ampExtension.addRule(rule);
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(AMPExtension.ELEMENT)) {
                    done = true;
                }
            }
        }

        return ampExtension;
    }

    private AMPExtension.Condition createCondition(String name, String value) {
        if (name == null || value == null) {
            LOGGER.severe("Can't create rule condition from null name and/or value");
            return null;
        }


        if (AMPDeliverCondition.NAME.equals(name)) {
            try {
                return new AMPDeliverCondition(AMPDeliverCondition.Value.valueOf(value));
            } catch (IllegalArgumentException ex) {
                LOGGER.severe("Found invalid rule delivery condition value " + value);
                return null;
            }
        } else if (AMPExpireAtCondition.NAME.equals(name)) {
            return new AMPExpireAtCondition(value);
        } else if (AMPMatchResourceCondition.NAME.equals(name)) {
            try {
                return new AMPMatchResourceCondition(AMPMatchResourceCondition.Value.valueOf(value));
            } catch (IllegalArgumentException ex) {
                LOGGER.severe("Found invalid rule match-resource condition value " + value);
                return null;
            }
        } else {
            LOGGER.severe("Found unknown rule condition name " + name);
            return null;
        }
    }
}
