/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle_filetransfer;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.hashes.HashManager;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.ChecksumElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChildElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.Range;
import org.jivesoftware.smackx.jingle_filetransfer.provider.ChecksumProvider;

import org.junit.Test;

/**
 * Created by vanitas on 12.07.17.
 */
public class ChecksumTest extends SmackTestSuite {

    @Test
    public void parserTest() throws Exception {
        HashElement hash = new HashElement(HashManager.ALGORITHM.SHA_256, "f4OxZX/x/FO5LcGBSKHWXfwtSx+j1ncoSt3SABJtkGk=");
        JingleFileTransferChildElement file = new JingleFileTransferChildElement(null, null, hash, null, null, -1, null);
        ChecksumElement checksum = new ChecksumElement(JingleContentElement.Creator.initiator, "name", file);

        String xml = "<checksum xmlns='urn:xmpp:jingle:apps:file-transfer:5' creator='initiator' name='name'>" +
                "<file>" +
                "<hash xmlns='urn:xmpp:hashes:2' algo='sha-256'>f4OxZX/x/FO5LcGBSKHWXfwtSx+j1ncoSt3SABJtkGk=</hash>" +
                "</file>" +
                "</checksum>";

        assertXMLEqual(xml, checksum.toXML().toString());
        assertXMLEqual(xml, new ChecksumProvider().parse(TestUtils.getParser(xml)).toXML().toString());

        Range range = new Range(12L,34L);
        file = new JingleFileTransferChildElement(null, null, hash, null, null, -1, range);
        checksum = new ChecksumElement(JingleContentElement.Creator.initiator, "name", file);

        xml = "<checksum xmlns='urn:xmpp:jingle:apps:file-transfer:5' creator='initiator' name='name'>" +
                "<file>" +
                "<range offset='12' length='34'/>" +
                "<hash xmlns='urn:xmpp:hashes:2' algo='sha-256'>f4OxZX/x/FO5LcGBSKHWXfwtSx+j1ncoSt3SABJtkGk=</hash>" +
                "</file>" +
                "</checksum>";
        assertXMLEqual(xml, checksum.toXML().toString());
        assertXMLEqual(xml, new ChecksumProvider().parse(TestUtils.getParser(xml)).toXML().toString());

    }
}
