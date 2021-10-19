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
package org.jivesoftware.smackx.jingle.element;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jivesoftware.smack.packet.StreamOpen;

import org.junit.jupiter.api.Test;

public class JingleTest {

    @Test
    public void noRedundantNamespaceTest() {
        Jingle.Builder jingleBuilder = Jingle.builder("test-id");
        jingleBuilder.setSessionId("MySession");
        jingleBuilder.setAction(JingleAction.content_accept);

        JingleContent.Builder jingleContentBuilder = JingleContent.getBuilder();
        jingleContentBuilder.setName("Hello world");
        jingleContentBuilder.setCreator(JingleContent.Creator.initiator);

        jingleBuilder.addJingleContent(jingleContentBuilder.build());
        Jingle iq = jingleBuilder.build();

        String actualXml = iq.toXML(StreamOpen.CLIENT_NAMESPACE).toString();
        String expectedXml
                        = "<iq id='test-id' type='set'>"
                        + "<jingle xmlns='urn:xmpp:jingle:1' action='content-accept' sid='MySession'>"
                        + "<content creator='initiator' name='Hello world'/>"
                        + "</jingle></iq>";
        assertEquals(expectedXml, actualXml);
    }
}
