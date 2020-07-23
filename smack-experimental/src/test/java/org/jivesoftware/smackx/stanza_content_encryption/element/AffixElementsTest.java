/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.stanza_content_encryption.element;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.util.XmppDateTime;

public class AffixElementsTest {

    public static final EntityBareJid JID_HOUSTON = JidCreate.entityBareFromOrThrowUnchecked("missioncontrol@houston.nasa.gov");
    public static final EntityBareJid JID_OPPORTUNITY = JidCreate.entityBareFromOrThrowUnchecked("opportunity@mars.planet");

    /**
     * Test serialization of 'to' affix element.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0420.html#example-1">XEP-420 Example 1</a>
     */
    @Test
    public void testToAffixElement() {
        ToAffixElement to = new ToAffixElement(JID_HOUSTON);
        String expectedXml = "<to jid='missioncontrol@houston.nasa.gov'/>";

        assertXmlSimilar(expectedXml, to.toXML());
        assertEquals(JID_HOUSTON, to.getJid());
    }

    @Test
    public void testToAffixElementEquals() {
        ToAffixElement to1 = new ToAffixElement(JID_HOUSTON);
        ToAffixElement to2 = new ToAffixElement(JID_HOUSTON);

        assertEquals(to1, to2);
        assertEquals(to1, to1);
        assertEquals(to1.hashCode(), to2.hashCode());
        assertFalse(to1.equals(null));
    }

    @Test
    public void toElementNullArgThrows() {
        assertThrows(IllegalArgumentException.class, () -> new ToAffixElement(null));
    }

    /**
     * Test serialization of 'from' affix element.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0420.html#example-1">XEP-420 Example 1</a>
     */
    @Test
    public void testFromAffixElement() {
        FromAffixElement from = new FromAffixElement(JID_OPPORTUNITY);
        String expectedXml = "<from jid='opportunity@mars.planet'/>";

        assertXmlSimilar(expectedXml, from.toXML());
        assertEquals(JID_OPPORTUNITY, from.getJid());
    }

    @Test
    public void testFromAffixElementEquals() {
        FromAffixElement from1 = new FromAffixElement(JID_HOUSTON);
        FromAffixElement from2 = new FromAffixElement(JID_HOUSTON);

        assertEquals(from1, from2);
        assertEquals(from1, from1);
        assertEquals(from1.hashCode(), from2.hashCode());
        assertFalse(from1.equals(null));
    }

    @Test
    public void fromElementNullArgThrows() {
        assertThrows(IllegalArgumentException.class, () -> new FromAffixElement(null));
    }

    @Test
    public void testTimestampAffixElement() throws ParseException {
        Date date = XmppDateTime.parseDate("2004-01-25T05:05:00.000+00:00");
        TimestampAffixElement timestamp = new TimestampAffixElement(date);
        String expectedXml = "<time stamp='2004-01-25T05:05:00.000+00:00'/>";

        assertXmlSimilar(expectedXml, timestamp.toXML());
        assertEquals(date, timestamp.getTimestamp());
    }

    @Test
    public void timestampElementNullArgThrows() {
        assertThrows(IllegalArgumentException.class, () -> new TimestampAffixElement(null));
    }

    @Test
    public void testTimestampElementEquals() throws ParseException {
        TimestampAffixElement t1 = new TimestampAffixElement(XmppDateTime.parseDate("2004-01-25T05:05:00.000+00:00"));
        TimestampAffixElement t2 = new TimestampAffixElement(t1.getTimestamp());

        assertEquals(t1, t2);
        assertEquals(t1, t1);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertFalse(t1.equals(null));
    }

    @Test
    public void testRandomPaddingElement() {
        RandomPaddingAffixElement rpad = new RandomPaddingAffixElement();

        assertNotNull(rpad.getPadding());
        assertTrue(rpad.getPadding().length() < 200);
    }

    @Test
    public void testRandomPaddingEquals() {
        RandomPaddingAffixElement rpad1 = new RandomPaddingAffixElement();
        RandomPaddingAffixElement rpad2 = new RandomPaddingAffixElement(rpad1.getPadding());

        assertEquals(rpad1, rpad2);
        assertEquals(rpad1, rpad1);
        assertEquals(rpad1.hashCode(), rpad2.hashCode());
        assertFalse(rpad1.equals(null));
    }

    @Test
    public void testRandomPaddingSerialization() {
        RandomPaddingAffixElement rpad = new RandomPaddingAffixElement();
        String expectedXml = "<rpad>" + rpad.getPadding() + "</rpad>";

        assertXmlSimilar(expectedXml, rpad.toXML());
    }

    @Test
    public void rpadElementNullArgThrows() {
        assertThrows(IllegalArgumentException.class, () -> new RandomPaddingAffixElement(null));
    }
}
