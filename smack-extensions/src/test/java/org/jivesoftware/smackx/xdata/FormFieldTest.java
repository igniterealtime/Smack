/**
 *
 * Copyright 2020 Florian Schmaus.
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
package org.jivesoftware.smackx.xdata;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.JidTestUtil;

class FormFieldTest {

    @Test
    public void testJidMultiToXml() {
        JidMultiFormField jidMultiFormField = FormField.jidMultiBuilder("myfield")
                        .addValue(JidTestUtil.BARE_JID_1)
                        .addValue(JidTestUtil.BARE_JID_2)
                        .build();

        String expectedXml = "<field xmlns='jabber:x:data' var='myfield' type='jid-multi'><value>one@exampleone.org</value><value>one@exampletwo.org</value></field>";
        CharSequence xml = jidMultiFormField.toXML();
        assertXmlSimilar(expectedXml, xml);
    }

}
