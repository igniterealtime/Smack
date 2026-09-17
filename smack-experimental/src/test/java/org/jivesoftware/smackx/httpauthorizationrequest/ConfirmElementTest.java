/*
 *
 * Copyright 2020 Eng Chong Meng
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
package org.jivesoftware.smackx.httpauthorizationrequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jivesoftware.smack.test.util.TestUtils;

import org.jivesoftware.smackx.httpauthorizationrequest.element.ConfirmElement;
import org.jivesoftware.smackx.httpauthorizationrequest.provider.ConfirmExtProvider;

import org.junit.jupiter.api.Test;

/**
 * Test serialization and parsing of ConfirmElement.
 */
public class ConfirmElementTest {

    @Test
    public void confirmElementSerializationTest() throws Exception {
        String id = "a7374jnjlalasdf82";
        String method = "GET";
        String url = "https://files.shakespeare.lit:9345/missive.html";

        ConfirmElement element = new ConfirmElement(id, method, url);
        String expected =
                "<confirm xmlns='http://jabber.org/protocol/http-auth'" +
                        " id='a7374jnjlalasdf82'" +
                        " method='GET'" +
                        " url='https://files.shakespeare.lit:9345/missive.html'/>";

        String actual = element.toXML().toString();
        assertEquals(expected, actual, "Serialized xml of ConfirmElement must match.");

        ConfirmElement parsed = new ConfirmExtProvider().parse(TestUtils.getParser(actual));
        assertEquals(actual, parsed.toXML().toString(), "Parsed ConfirmElement must equal the original.");
    }
}
