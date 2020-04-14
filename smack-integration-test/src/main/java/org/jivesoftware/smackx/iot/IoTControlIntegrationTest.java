/**
 *
 * Copyright 2016-2020 Florian Schmaus
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
package org.jivesoftware.smackx.iot;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collection;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smackx.iot.control.IoTControlManager;
import org.jivesoftware.smackx.iot.control.ThingControlRequest;
import org.jivesoftware.smackx.iot.control.element.IoTSetResponse;
import org.jivesoftware.smackx.iot.control.element.SetBoolData;
import org.jivesoftware.smackx.iot.control.element.SetData;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.Jid;

public class IoTControlIntegrationTest extends AbstractSmackIntegrationTest {

    private final IoTControlManager IoTControlManagerOne;

    private final IoTControlManager IoTControlManagerTwo;

    public IoTControlIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        IoTControlManagerOne = IoTControlManager.getInstanceFor(conOne);
        IoTControlManagerTwo = IoTControlManager.getInstanceFor(conTwo);
    }

    /**
     * Connection one provides a thing, which is controlled by connection two.
     *
     * @throws Exception if an exception occurs.
     * @throws TimeoutException if there was a timeout.
     */
    @SmackIntegrationTest
    // @SmackSerialIntegrationTest
    public void controlTest() throws Exception {
        final String key = StringUtils.randomString(12);
        final String sn = StringUtils.randomString(12);
        final SimpleResultSyncPoint syncPoint = new SimpleResultSyncPoint();

        Thing controlThing = Thing.builder().setKey(key).setSerialNumber(sn).setControlRequestHandler(new ThingControlRequest() {
            @Override
            public void processRequest(Jid from, Collection<SetData> setData) throws XMPPErrorException {
                if (!from.equals(conTwo.getUser())) {
                    return;
                }
                for (final SetData data : setData) {
                    if (!data.getName().equals(testRunId)) continue;
                    if (!(data instanceof SetBoolData)) continue;
                    SetBoolData boolData = (SetBoolData) data;
                    if (boolData.getBooleanValue()) {
                        syncPoint.signal();
                        break;
                    }
                }
            }
        }).build();

        IoTControlManagerOne.installThing(controlThing);

        try {
            IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);

            SetData data = new SetBoolData(testRunId, true);
            IoTSetResponse response = IoTControlManagerTwo.setUsingIq(conOne.getUser(), data);
            assertNotNull(response);
        }
        finally {
            IoTControlManagerOne.uninstallThing(controlThing);
            IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
        }

        syncPoint.waitForResult(timeout);
    }
}
