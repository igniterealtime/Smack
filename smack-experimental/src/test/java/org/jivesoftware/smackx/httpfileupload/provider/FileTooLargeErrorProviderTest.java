/**
 *
 * Copyright © 2017 Grigory Fedorov
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
package org.jivesoftware.smackx.httpfileupload.provider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.httpfileupload.element.FileTooLargeError;
import org.jivesoftware.smackx.httpfileupload.element.FileTooLargeError_V0;

import org.junit.Assert;
import org.junit.Test;

public class FileTooLargeErrorProviderTest {

    /**
     * Example 7. Alternative response by the upload service if the file size was too large
     * @see <a href="http://xmpp.org/extensions/xep-0363.html#errors">XEP-0363: HTTP File Upload 5. Error conditions</a>
     */
    private static final String slotErrorFileToLarge_vbase
            = "<iq from='upload.montague.tld' "
            +       "id='step_03' "
            +       "to='romeo@montague.tld/garden' "
            +       "type='error'>"
            +   "<request xmlns='urn:xmpp:http:upload'>"
            +       "<filename>my_juliet.png</filename>"
            +       "<size>23456</size>"
            +   "</request>"
            +   "<error type='modify'>"
            +       "<not-acceptable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas' />"
            +       "<text xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'>File too large. The maximum file size is 20000 bytes</text>"
            +       "<file-too-large xmlns='urn:xmpp:http:upload'>"
            +           "<max-file-size>20000</max-file-size>"
            +       "</file-too-large>"
            +   "</error>"
            + "</iq>";

    private static final String slotErrorFileToLarge_v0
            = "<iq from='upload.montague.tld' " +
            "    id='step_03' " +
            "    to='romeo@montague.tld/garden' " +
            "    type='error'> " +
            "  <request xmlns='urn:xmpp:http:upload:0' " +
            "    filename='my-juliet.jpg' " +
            "    size='23456' " +
            "    content-type='image/jpeg' /> " +
            "  <error type='modify'> " +
            "    <not-acceptable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas' /> " +
            "    <text xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'>File too large. The maximum file size is 20000 bytes</text> " +
            "    <file-too-large xmlns='urn:xmpp:http:upload:0'> " +
            "      <max-file-size>20000</max-file-size> " +
            "    </file-too-large> " +
            "  </error> " +
            "</iq>";

    @Test
    public void checkSlotErrorFileToLargeVBase() throws Exception {
        IQ fileTooLargeErrorIQ = PacketParserUtils.parseStanza(slotErrorFileToLarge_vbase);

        Assert.assertEquals(IQ.Type.error, fileTooLargeErrorIQ.getType());

        FileTooLargeError fileTooLargeError = FileTooLargeError.from(fileTooLargeErrorIQ);
        assertFalse(fileTooLargeError instanceof FileTooLargeError_V0);
        Assert.assertEquals(20000, fileTooLargeError.getMaxFileSize());
    }

    @Test
    public void checkSlotErrorFileToLargeV0() throws Exception {
        IQ fileTooLargeErrorIQ = PacketParserUtils.parseStanza(slotErrorFileToLarge_v0);

        Assert.assertEquals(IQ.Type.error, fileTooLargeErrorIQ.getType());

        FileTooLargeError fileTooLargeError = FileTooLargeError.from(fileTooLargeErrorIQ);
        assertTrue(fileTooLargeError instanceof FileTooLargeError_V0);
        Assert.assertEquals(20000, fileTooLargeError.getMaxFileSize());
    }
}
