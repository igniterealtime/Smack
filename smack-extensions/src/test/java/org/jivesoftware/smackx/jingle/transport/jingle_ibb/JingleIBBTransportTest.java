/**
 *
 * Copyright © 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle.transport.jingle_ibb;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.element.JingleIBBTransportElement;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.provider.JingleIBBTransportProvider;

import org.junit.Test;

/**
 * Test JingleIBBTransport provider and element.
 */
public class JingleIBBTransportTest extends SmackTestSuite {

    @Test
    public void parserTest() throws Exception {
        String sid = StringUtils.randomString(24);
        short size = 8192;

        String xml = "<transport xmlns='urn:xmpp:jingle:transports:ibb:1' block-size='8192' sid='" + sid + "'/>";

        JingleIBBTransport transport = new JingleIBBTransport(sid, size);
        assertEquals(xml, transport.getElement().toXML().toString());
        assertEquals(size, (short) transport.getBlockSize());
        assertEquals(sid, transport.getStreamId());

        JingleIBBTransportElement parsed = new JingleIBBTransportProvider()
                .parse(TestUtils.getParser(xml));
        assertEquals(transport.getElement(), parsed);
        assertTrue(transport.getElement().equals(parsed));
        assertEquals(xml, parsed.toXML().toString());

        JingleIBBTransport transport1 = new JingleIBBTransport();
        assertEquals(JingleIBBTransport.DEFAULT_BLOCK_SIZE, transport1.getBlockSize());

        assertFalse(transport.equals(null));

        JingleIBBTransport transport2 = new JingleIBBTransport(transport1.getStreamId(), (short) 256);
        assertEquals((Short) (short) 256, transport2.getBlockSize());
        assertFalse(transport1.equals(transport2));

        transport1.handleSessionAccept(transport2.getElement(), null);
        assertEquals(transport2.getBlockSize(), transport1.getBlockSize());

        JingleIBBTransport transport3 = new JingleIBBTransportAdapter().transportFromElement(transport2.getElement());
        assertEquals(transport2.getBlockSize(), transport3.getBlockSize());
        assertEquals(transport2.getStreamId(), transport3.getStreamId());
    }

    @Test
    public void jingleIBBTransportManagerTest() {
        JingleIBBTransportManager manager = JingleIBBTransportManager.getInstanceFor(new DummyConnection());

        JingleIBBTransport transport1 = (JingleIBBTransport) manager.createTransportForInitiator(null);
        assertEquals(JingleIBBTransport.DEFAULT_BLOCK_SIZE, transport1.getBlockSize());

        JingleIBBTransport transport2 = new JingleIBBTransport("sid", (short) 256);

        JingleIBBTransport transport3 = (JingleIBBTransport) manager.createTransportForResponder(null, transport2);
        assertEquals((Short) (short) 256, transport3.getBlockSize());

        JingleIBBTransport transport4 = new JingleIBBTransport("sod", Short.MAX_VALUE);
        assertEquals((Short) Short.MAX_VALUE, transport4.getBlockSize());

        JingleIBBTransport transport5 = (JingleIBBTransport) manager.createTransportForResponder(null, transport4);
        assertEquals(JingleIBBTransport.MAX_BLOCKSIZE, transport5.getBlockSize());
    }
}
