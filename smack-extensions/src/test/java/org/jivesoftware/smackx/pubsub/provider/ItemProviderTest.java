/**
 *
 * Copyright 2021 Florian Schmaus
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
package org.jivesoftware.smackx.pubsub.provider;

import java.io.IOException;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class ItemProviderTest {

    /**
     * Check that {@link ItemProvider} is able to parse items which have whitespace before their Payload.
     *
     * @param parserKind the used parser Kind
     * @throws XmlPullParserException if an XML pull parser exception occurs.
     * @throws IOException if an IO exception occurs.
     * @throws SmackParsingException if an Smack parsing exception occurs.
     * @see <a href="https://igniterealtime.atlassian.net/jira/software/c/projects/SMACK/issues/SMACK-918">SMACK-918</a>
     */
    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void whitespaceBeforeItemPayload(SmackTestUtil.XmlPullParserKind parserKind) throws XmlPullParserException, IOException, SmackParsingException {
        String item = "<item id='13a3710c-68c3-4da2-8484-d8d9c77af91e' xmlns='http://jabber.org/protocol/pubsub#event'>"
                    + "\n <geoloc xmlns='http://jabber.org/protocol/geoloc' xml:lang='en'/>"
                    + "</item>";
        SmackTestUtil.parse(item, ItemProvider.class, parserKind);
    }

}
