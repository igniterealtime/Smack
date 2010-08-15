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
