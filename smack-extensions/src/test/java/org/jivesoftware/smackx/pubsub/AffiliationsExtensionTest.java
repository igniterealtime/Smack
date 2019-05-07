/**
 *
 * Copyright 2017 Florian Schmaus
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
package org.jivesoftware.smackx.pubsub;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smackx.pubsub.Affiliation.Type;

import org.junit.Test;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.JidTestUtil;
import org.xml.sax.SAXException;

public class AffiliationsExtensionTest {

    @Test
    public void testAffiliationsExtensionToXml() throws SAXException, IOException {
        BareJid affiliatedJid = JidTestUtil.BARE_JID_1;
        Affiliation affiliation = new Affiliation(affiliatedJid, Type.member);
        List<Affiliation> affiliationsList = new ArrayList<>();
        affiliationsList.add(affiliation);

        AffiliationsExtension affiliationsExtension = new AffiliationsExtension(affiliationsList, "testNode");

        CharSequence xml = affiliationsExtension.toXML();

        assertXmlSimilar("<affiliations node='testNode'><affiliation xmlns='http://jabber.org/protocol/pubsub#owner' jid='one@exampleone.org' affiliation='member'/></affiliations>",
                        xml.toString());
    }

}
