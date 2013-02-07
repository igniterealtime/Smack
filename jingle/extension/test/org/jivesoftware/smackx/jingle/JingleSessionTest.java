/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.jingle.media.JingleMediaManager;
import org.jivesoftware.smackx.jingle.mediaimpl.test.TestMediaManager;
import org.jivesoftware.smackx.jingle.nat.FixedResolver;
import org.jivesoftware.smackx.jingle.nat.FixedTransportManager;

import java.util.ArrayList;
import java.util.List;

public class JingleSessionTest extends SmackTestCase {

    public JingleSessionTest(final String name) {
        super(name);
    }

    public void testEqualsObject() {
        
        FixedResolver tr1 = new FixedResolver("127.0.0.1", 54222);
        FixedTransportManager ftm1 = new FixedTransportManager(tr1);
        TestMediaManager tmm1 = new TestMediaManager(ftm1);
        List<JingleMediaManager> trl1 = new ArrayList<JingleMediaManager>();
        trl1.add(tmm1);
        
        JingleSession js1 = new JingleSession(getConnection(0), "res1", null, "10", trl1);
        JingleSession js2 = new JingleSession(getConnection(1), "res1", null, "10", trl1);
        JingleSession js3 = new JingleSession(getConnection(2), "res2", null, "11", trl1);

        System.out.println(js1.getSid());
        System.out.println(js2.getSid());

        js1.setInitiator("js1");
        js2.setInitiator("js1");
        js1.setSid("10");
        js2.setSid("10");

        assertEquals(js1, js2);
        assertEquals(js2, js1);

        assertFalse(js1.equals(js3));
    }

    public void testGetInstanceFor() {
        String ini1 = "initiator1";
        String sid1 = "sid1";
        String ini2 = "initiator2";
        String sid2 = "sid2";

        FixedResolver tr1 = new FixedResolver("127.0.0.1", 54222);
        FixedTransportManager ftm1 = new FixedTransportManager(tr1);
        TestMediaManager tmm1 = new TestMediaManager(ftm1);
        List<JingleMediaManager> trl1 = new ArrayList<JingleMediaManager>();
        trl1.add(tmm1);
        
        JingleSession js1 = new JingleSession(getConnection(0), ini1, null, sid1, trl1);
        JingleSession js2 = new JingleSession(getConnection(1), ini2, null, sid2, trl1);

        // For a packet, we should be able to get a session that handles that...
        assertNotNull(JingleSession.getInstanceFor(getConnection(0)));
        assertNotNull(JingleSession.getInstanceFor(getConnection(1)));

        assertEquals(JingleSession.getInstanceFor(getConnection(0)), js1);
        assertEquals(JingleSession.getInstanceFor(getConnection(1)), js2);
    }

    protected int getMaxConnections() {
        return 3;
    }
}
