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
package org.jivesoftware.smackx.jingle.provider;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.jingle.transports.jingle_ibb.element.JingleIBBTransport;
import org.jivesoftware.smackx.jingle.transports.jingle_ibb.provider.JingleIBBTransportProvider;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransport;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.provider.JingleS5BTransportProvider;

import org.junit.Test;

/**
 * Tests for the JingleContentProviderManager.
 */
public class JingleContentProviderManagerTest extends SmackTestSuite {

    @Test
    public void transportProviderTest() {
        assertNull(JingleContentProviderManager.getJingleContentTransportProvider(JingleIBBTransport.NAMESPACE_V1));
        assertNull(JingleContentProviderManager.getJingleContentTransportProvider(JingleS5BTransport.NAMESPACE_V1));

        JingleIBBTransportProvider ibbProvider = new JingleIBBTransportProvider();
        JingleContentProviderManager.addJingleContentTransportProvider(JingleIBBTransport.NAMESPACE_V1, ibbProvider);
        assertEquals(ibbProvider, JingleContentProviderManager.getJingleContentTransportProvider(JingleIBBTransport.NAMESPACE_V1));

        assertNull(JingleContentProviderManager.getJingleContentTransportProvider(JingleS5BTransport.NAMESPACE_V1));
        JingleS5BTransportProvider s5bProvider = new JingleS5BTransportProvider();
        JingleContentProviderManager.addJingleContentTransportProvider(JingleS5BTransport.NAMESPACE_V1, s5bProvider);
        assertEquals(s5bProvider, JingleContentProviderManager.getJingleContentTransportProvider(JingleS5BTransport.NAMESPACE_V1));
    }
}
