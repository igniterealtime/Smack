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
package org.jivesoftware.smackx.bytestreams.ibb;

import org.jivesoftware.smackx.bytestreams.ibb.packet.CloseTest;
import org.jivesoftware.smackx.bytestreams.ibb.packet.DataPacketExtensionTest;
import org.jivesoftware.smackx.bytestreams.ibb.packet.DataTest;
import org.jivesoftware.smackx.bytestreams.ibb.packet.OpenTest;
import org.jivesoftware.smackx.bytestreams.ibb.provider.OpenIQProviderTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { CloseTest.class, DataPacketExtensionTest.class, DataTest.class,
                OpenTest.class, OpenIQProviderTest.class, CloseListenerTest.class,
                DataListenerTest.class, InBandBytestreamManagerTest.class,
                InBandBytestreamRequestTest.class,
                InBandBytestreamSessionMessageTest.class,
                InBandBytestreamSessionTest.class, InitiationListenerTest.class })
public class IBBTestsSuite {
    // the class remains completely empty,
    // being used only as a holder for the above annotations
}
