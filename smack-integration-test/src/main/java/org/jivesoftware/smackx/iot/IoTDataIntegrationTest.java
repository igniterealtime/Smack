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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smackx.iot.data.IoTDataManager;
import org.jivesoftware.smackx.iot.data.ThingMomentaryReadOutRequest;
import org.jivesoftware.smackx.iot.data.ThingMomentaryReadOutResult;
import org.jivesoftware.smackx.iot.data.element.IoTDataField;
import org.jivesoftware.smackx.iot.data.element.IoTDataField.IntField;
import org.jivesoftware.smackx.iot.data.element.IoTFieldsExtension;
import org.jivesoftware.smackx.iot.data.element.NodeElement;
import org.jivesoftware.smackx.iot.data.element.TimestampElement;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;

@SpecificationReference(document = "XEP-0347", version = "0.5.1")
public class IoTDataIntegrationTest extends AbstractSmackIntegrationTest {

    private final IoTDataManager iotDataManagerOne;

    private final IoTDataManager iotDataManagerTwo;

    public IoTDataIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        iotDataManagerOne = IoTDataManager.getInstanceFor(conOne);
        iotDataManagerTwo = IoTDataManager.getInstanceFor(conTwo);
    }

    /**
     * Connection one provides a thing, which momentary value is read out by connection two.
     *
     * @throws Exception if an exception occurs.
     * @throws TimeoutException if there was a timeout.
     */
    @SmackIntegrationTest
    public void dataTest() throws Exception {
        final String key = StringUtils.randomString(12);
        final String sn = StringUtils.randomString(12);
        final int value = INSECURE_RANDOM.nextInt();

        Thing dataThing = Thing.builder().setKey(key).setSerialNumber(sn).setMomentaryReadOutRequestHandler(new ThingMomentaryReadOutRequest() {
            @Override
            public void momentaryReadOutRequest(ThingMomentaryReadOutResult callback) {
                IoTDataField.IntField field = new IntField(testRunId, value);
                callback.momentaryReadOut(Collections.singletonList(field));
            }
        }).build();

        iotDataManagerOne.installThing(dataThing);

        List<IoTFieldsExtension> values;
        try {
            IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);

            values = iotDataManagerTwo.requestMomentaryValuesReadOut(conOne.getUser());
        }
        finally {
            iotDataManagerOne.uninstallThing(dataThing);
            IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
        }

        assertEquals(1, values.size(), "An unexpected amount of momentary values was received by " + conOne.getUser());
        IoTFieldsExtension iotFieldsExtension = values.get(0);
        List<NodeElement> nodes = iotFieldsExtension.getNodes();

        assertEquals(1, nodes.size(), "The momentary value received by " + conOne.getUser() + " contains an unexpected amount of nodes.");
        NodeElement node = nodes.get(0);
        List<TimestampElement> timestamps = node.getTimestampElements();

        assertEquals(1, timestamps.size(), "The node received by " + conOne.getUser() + " contains an unexpected amount of timestamps.");
        TimestampElement timestamp = timestamps.get(0);
        List<? extends IoTDataField> fields = timestamp.getDataFields();

        assertEquals(1, fields.size(), "The timestamp received by " + conOne.getUser() + " contains an unexpected amount of data fields.");
        IoTDataField dataField = fields.get(0);
        assertTrue(dataField instanceof IoTDataField.IntField, "The data field received by " + conOne.getUser() + " was expected to be an instance of " + IoTDataField.IntField.class.getSimpleName() + ", but instead, it was " + dataField.getClass().getSimpleName());
        IoTDataField.IntField intDataField = (IoTDataField.IntField) dataField;
        assertEquals(testRunId, intDataField.getName(), "Unexpected name in the data field received by " + conOne.getUser());
        assertEquals(value, intDataField.getValue(), "Unexpected value in the data field received by " + conOne.getUser());
    }
}
