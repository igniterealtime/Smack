/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox;

import static junit.framework.TestCase.assertEquals;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.util.Date;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.provider.PublicKeysListElementProvider;

import org.bouncycastle.openpgp.PGPException;
import org.junit.Test;
import org.jxmpp.util.XmppDateTime;
import org.pgpainless.key.OpenPgpV4Fingerprint;
import org.xmlpull.v1.XmlPullParser;

public class PublicKeysListElementTest extends SmackTestSuite {

    @Test
    public void providerTest() throws Exception {
        String expected =
                "<public-keys-list xmlns='urn:xmpp:openpgp:0'>" +
                        "<pubkey-metadata " +
                        "v4-fingerprint='1357B01865B2503C18453D208CAC2A9678548E35' " +
                        "date='2018-03-01T15:26:12.000+00:00'" +
                        "/>" +
                        "<pubkey-metadata " +
                        "v4-fingerprint='67819B343B2AB70DED9320872C6464AF2A8E4C02' " +
                        "date='1953-05-16T12:00:00.000+00:00'" +
                        "/>" +
                        "</public-keys-list>";

        Date date1 = XmppDateTime.parseDate("2018-03-01T15:26:12.000+00:00");
        Date date2 = XmppDateTime.parseDate("1953-05-16T12:00:00.000+00:00");
        PublicKeysListElement.PubkeyMetadataElement child1 =
                new PublicKeysListElement.PubkeyMetadataElement(
                        new OpenPgpV4Fingerprint("1357B01865B2503C18453D208CAC2A9678548E35"), date1);
        PublicKeysListElement.PubkeyMetadataElement child2 =
                new PublicKeysListElement.PubkeyMetadataElement(
                        new OpenPgpV4Fingerprint("67819B343B2AB70DED9320872C6464AF2A8E4C02"), date2);

        PublicKeysListElement element = PublicKeysListElement.builder()
                .addMetadata(child1)
                .addMetadata(child2)
                .build();

        assertXMLEqual(expected, element.toXML(null).toString());

        XmlPullParser parser = TestUtils.getParser(expected);
        PublicKeysListElement parsed = PublicKeysListElementProvider.TEST_INSTANCE.parse(parser);

        assertEquals(element.getMetadata(), parsed.getMetadata());
    }

    @Test
    public void listBuilderRefusesDuplicatesTest() throws PGPException {
        PublicKeysListElement.Builder builder = PublicKeysListElement.builder();
        String fp40 = "49545320414c4c2041424f555420444120484558";
        Date oneDate = new Date(12337883234L);
        Date otherDate = new Date(8888348384L);

        // Check if size of metadata is one after insert.
        builder.addMetadata(new PublicKeysListElement.PubkeyMetadataElement(new OpenPgpV4Fingerprint(fp40), oneDate));
        assertEquals(builder.build().getMetadata().size(), 1);

        // Check if size is still one after inserting element with same fp.
        builder.addMetadata(new PublicKeysListElement.PubkeyMetadataElement(new OpenPgpV4Fingerprint(fp40), otherDate));
        assertEquals(builder.build().getMetadata().size(), 1);
    }
}
