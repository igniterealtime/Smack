/**
 *
 * Copyright 2014-2018 Florian Schmaus
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
package org.jivesoftware.smackx.jiveproperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.util.PacketParserUtils;

import org.jivesoftware.smackx.jiveproperties.packet.JivePropertiesExtension;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JivePropertiesExtensionTest extends SmackTestSuite {

    @BeforeEach
    public void setUp() {
        JivePropertiesManager.setJavaObjectEnabled(true);
    }

    @AfterEach
    public void tearDown() {
        JivePropertiesManager.setJavaObjectEnabled(false);
    }

    @Test
    public void checkProvider() throws Exception {
        // @formatter:off
        String properties = "<message xmlns='jabber:client' from='romeo@example.net/orchard' to='juliet@example.com/balcony'>"
                        + "<body>Neither, fair saint, if either thee dislike.</body>"
                        + "<properties xmlns='http://www.jivesoftware.com/xmlns/xmpp/properties'>"
                        + "<property>"
                        + "<name>FooBar</name>"
                        + "<value type='integer'>42</value>"
                        + "</property>"
                        + "</properties>"
                        + "</message>";
        // @formatter:on

        Message message = PacketParserUtils.parseStanza(properties);
        JivePropertiesExtension jpe = JivePropertiesExtension.from(message);
        assertNotNull(jpe);

        Integer integer = (Integer) jpe.getProperty("FooBar");
        assertNotNull(integer);
        int fourtytwo = integer;
        assertEquals(42, fourtytwo);
    }
}
