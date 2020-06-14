/**
 *
 * Copyright (C) 2007 Jive Software.
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
package org.jivesoftware.smack.packet;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

public class PresenceTest {
    @Test
    public void setPresenceTypeTest() throws IOException, SAXException {
        Presence.Type type = Presence.Type.unavailable;
        Presence.Type type2 = Presence.Type.subscribe;

        StringBuilder controlBuilder = new StringBuilder();
        controlBuilder.append("<presence")
                .append(" type=\"")
                .append(type)
                .append("\">")
                .append("</presence>");
        String control = controlBuilder.toString();

        Presence presenceTypeInConstructor = StanzaBuilder.buildPresence()
                        .ofType(type)
                        .build();
        assertEquals(type, presenceTypeInConstructor.getType());
        assertXmlSimilar(control, presenceTypeInConstructor.toXML(StreamOpen.CLIENT_NAMESPACE).toString());

        controlBuilder = new StringBuilder();
        controlBuilder.append("<presence")
                .append(" type=\"")
                .append(type2)
                .append("\">")
                .append("</presence>");
        control = controlBuilder.toString();

        PresenceBuilder presenceTypeSet = getNewPresence();
        presenceTypeSet.ofType(type2);
        assertEquals(type2, presenceTypeSet.getType());
        assertXmlSimilar(control, presenceTypeSet.build().toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void setNullPresenceTypeTest() {
        assertThrows(IllegalArgumentException.class, () ->
        getNewPresence().ofType(null)
        );
    }

    @Test
    public void isPresenceAvailableTest() {
        PresenceBuilder presence = getNewPresence();
        presence.ofType(Presence.Type.available);
        assertTrue(presence.build().isAvailable());

        presence.ofType(Presence.Type.unavailable);
        assertFalse(presence.build().isAvailable());
    }

    @Test
    public void setPresenceStatusTest() throws IOException, SAXException {
        final String status = "This is a test of the emergency broadcast system.";

        StringBuilder controlBuilder = new StringBuilder();
        controlBuilder.append("<presence>")
                .append("<status>")
                .append(status)
                .append("</status>")
                .append("</presence>");
        String control = controlBuilder.toString();

        PresenceBuilder presence = getNewPresence();
        presence.setStatus(status);

        assertEquals(status, presence.getStatus());
        assertXmlSimilar(control, presence.build().toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void setPresencePriorityTest() throws IOException, SAXException {
        final int priority = 10;

        StringBuilder controlBuilder = new StringBuilder();
        controlBuilder.append("<presence>")
                .append("<priority>")
                .append(priority)
                .append("</priority>")
                .append("</presence>");
        String control = controlBuilder.toString();

        PresenceBuilder presence = getNewPresence();
        presence.setPriority(priority);

        assertEquals(priority, presence.getPriority());
        assertXmlSimilar(control, presence.build().toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void setIllegalPriorityTest() {
        assertThrows(IllegalArgumentException.class, () ->
        getNewPresence().setPriority(Integer.MIN_VALUE)
        );
    }

    @Test
    public void setPresenceModeTest() throws IOException, SAXException {
        Presence.Mode mode1 = Presence.Mode.dnd;
                final int priority = 10;
        final String status = "This is a test of the emergency broadcast system.";
        Presence.Mode mode2 = Presence.Mode.chat;

        StringBuilder controlBuilder = new StringBuilder();
        controlBuilder.append("<presence>")
                .append("<status>")
                .append(status)
                .append("</status>")
                .append("<priority>")
                .append(priority)
                .append("</priority>")
                .append("<show>")
                .append(mode1)
                .append("</show>")
                .append("</presence>");
        String control = controlBuilder.toString();

        Presence presenceBuildWithBuilder = StanzaBuilder.buildPresence()
                        .ofType(Presence.Type.available)
                        .setStatus(status)
                        .setPriority(priority)
                        .setMode(mode1)
                        .build();
        assertEquals(mode1, presenceBuildWithBuilder.getMode());
        assertXmlSimilar(control, presenceBuildWithBuilder.toXML(StreamOpen.CLIENT_NAMESPACE).toString());

        controlBuilder = new StringBuilder();
        controlBuilder.append("<presence>")
                .append("<show>")
                .append(mode2)
                .append("</show>")
                .append("</presence>");
       control = controlBuilder.toString();

        PresenceBuilder presenceModeSet = getNewPresence();
        presenceModeSet.setMode(mode2);
        assertEquals(mode2, presenceModeSet.getMode());
        assertXmlSimilar(control, presenceModeSet.build().toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void isModeAwayTest() {
        PresenceBuilder presence = getNewPresence();
        presence.setMode(Presence.Mode.away);
        assertTrue(presence.build().isAway());

        presence.setMode(Presence.Mode.chat);
        assertFalse(presence.build().isAway());
    }

    @Test
    public void presenceXmlLangTest() throws IOException, SAXException {
        final String lang = "sp";

        StringBuilder controlBuilder = new StringBuilder();
        controlBuilder.append("<presence")
                .append(" xml:lang=\"")
                .append(lang)
                .append("\">")
                .append("</presence>");
        String control = controlBuilder.toString();

        PresenceBuilder presence = getNewPresence();
        presence.setLanguage(lang);

        assertXmlSimilar(control, presence.build().toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    private static PresenceBuilder getNewPresence() {
        PresenceBuilder presence = StanzaBuilder.buildPresence().ofType(Presence.Type.available);
        return presence;
    }
}
