/**
 *
 * Copyright © 2014-2019 Florian Schmaus
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
package org.jivesoftware.smack.provider;

import static org.junit.Assert.assertTrue;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XmlEnvironment;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

public class ProviderManagerTest {

    /**
     * This test should be run in a clean (e.g. forked) VM
     */
    @Test
    public void shouldInitializeSmackTest() throws Exception {
        ProviderManager.addIQProvider("foo", "bar", new TestIQProvider());
        assertTrue(SmackConfiguration.isSmackInitialized());
    }

    public static class TestIQProvider extends IQProvider<IQ> {

        @Override
        public IQ parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) {
            return null;
        }

    }
}
