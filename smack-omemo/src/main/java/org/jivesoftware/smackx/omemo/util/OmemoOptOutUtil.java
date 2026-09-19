/*
 *
 * Copyright 2014~2024 Eng Chong Meng
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

package org.jivesoftware.smackx.omemo.util;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.OmemoMessage;
import org.jivesoftware.smackx.omemo.element.OmemoOptOutElement;
import org.jivesoftware.smackx.stanza_content_encryption.element.AffixElement;
import org.jivesoftware.smackx.stanza_content_encryption.element.ContentElement;
import org.jivesoftware.smackx.stanza_content_encryption.element.EnvelopeElement;
import org.jivesoftware.smackx.stanza_content_encryption.element.FromAffixElement;
import org.jivesoftware.smackx.stanza_content_encryption.element.TimestampAffixElement;
import org.jivesoftware.smackx.stanza_content_encryption.element.ToAffixElement;
import org.jivesoftware.smackx.stanza_content_encryption.provider.EnvelopeElementProvider;

import org.jxmpp.jid.BareJid;

public class OmemoOptOutUtil {
    private static TimestampAffixElement affixTimeStamp = null;
    private static FromAffixElement affixFrom = null;
    private static ToAffixElement affixTo = null;
    private static int timeDeltaInMinute = 10;

    @SuppressWarnings("JavaUtilDate")
    public static EnvelopeElement createOmemoOptOut(OmemoManager manager, BareJid recipient, String reason) {
        OmemoOptOutElement optoutElement = new OmemoOptOutElement();
        if (StringUtils.isNotEmpty(reason)) {
            optoutElement.setReason(reason);
        }

        EnvelopeElement envelopeElement = EnvelopeElement.builder()
                .setFrom(manager.getOwnJid())
                .addTo(recipient)
                .setTimestamp(new Date())
                .setRandomPadding()
                .addContentItem(optoutElement)
                .build();

        return envelopeElement;
    }

    public static String parseEnvelopElement(OmemoMessage.Received decryptedMessage, Date date) {
        String reason = null;

        String msgBody = decryptedMessage.getBody();
        if (msgBody.contains(EnvelopeElement.NAMESPACE)) {
            BareJid fromJid = decryptedMessage.getSenderDevice().getJid();
            try {
                XmlPullParser parser = PacketParserUtils.getParserFor(msgBody);
                EnvelopeElementProvider provider = new EnvelopeElementProvider();
                EnvelopeElement envelopElement = provider.parse(parser, parser.getDepth(), null, null);
                reason = getReason(envelopElement, fromJid, date);
            }
            catch (XmlPullParserException | IOException | ParseException | SmackParsingException e) {
                throw new RuntimeException(e);
            }
        }
        return reason;
    }

    private static String getReason(EnvelopeElement envelopElement, BareJid fromJid, Date timeStamp) {
        String reasonText = ValidateOptOutContent(envelopElement, fromJid, timeStamp);
        if (StringUtils.isNullOrEmpty(reasonText)) {
            ContentElement content = envelopElement.getContentElement();
            List<XmlElement> contentElements = content.getItems();

            XmlElement contentElement = (contentElements.size() != 0) ? contentElements.get(0) : null;
            if (contentElement != null) {
                reasonText = ((OmemoOptOutElement) contentElement).getReason();
            }
        }
        return reasonText;
    }

    private static String ValidateOptOutContent(EnvelopeElement envelopElement, BareJid fromJid, Date date) {
        String reason = null;
        String warning = "Warning: Received suspicious omemo chat Opt-out!";

        List<AffixElement> affixElements = envelopElement.getAffixElements();
        extractAffixes(affixElements);

        boolean isValid = affixElements.contains(new FromAffixElement(fromJid));
        if (!isValid) {
            reason = warning + " Invalid sender: " + affixFrom.getJid();
        }

        if (affixTo != null && StringUtils.isNotEmpty(reason)) {
            reason += "Recipient: " + affixTo.getJid();
        }

        LocalDateTime localDateTime1 = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        LocalDateTime localDateTime2 = LocalDateTime.ofInstant(affixTimeStamp.getTimestamp().toInstant(), ZoneId.systemDefault());
        long diffTime = ChronoUnit.MINUTES.between(localDateTime1, localDateTime2);
        if (Math.abs(diffTime) > timeDeltaInMinute) {
            reason = warning + " Time difference (mins): " + diffTime;
        }
        return reason;
    }

    /**
     * This function extracts only important affix elements for app query.
     */
    private static void extractAffixes(List<AffixElement> affixElements) {
        for (AffixElement affixElement : affixElements) {
            if (affixElement instanceof TimestampAffixElement) {
                affixTimeStamp = (TimestampAffixElement) affixElement;
            }
            else if (affixElement instanceof ToAffixElement) {
                affixTo = (ToAffixElement) affixElement;
            }
            else if (affixElement instanceof FromAffixElement) {
                affixFrom = (FromAffixElement) affixElement;
            }
        }
    }
}
