package org.jivesoftware.smackx.ibb;

import org.jivesoftware.smackx.ibb.packet.CloseTest;
import org.jivesoftware.smackx.ibb.packet.DataPacketExtensionTest;
import org.jivesoftware.smackx.ibb.packet.DataTest;
import org.jivesoftware.smackx.ibb.packet.OpenTest;
import org.jivesoftware.smackx.ibb.provider.OpenIQProviderTest;
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
