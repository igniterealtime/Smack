/**
 *
 * Copyright 2018-2019 Florian Schmaus
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
package org.igniterealtime.smack.inttest;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.tcp.XmppNioTcpConnection;

import org.junit.Test;
import org.jxmpp.stringprep.XmppStringprepException;

public class SmackIntegrationTestXmppConnectionManagerTest {

    @Test
    public void simpleXmppConnectionDescriptorTest() throws ClassNotFoundException, NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            KeyManagementException, NoSuchAlgorithmException, XmppStringprepException, InstantiationException {
        XmppConnectionDescriptor<XmppNioTcpConnection, XMPPTCPConnectionConfiguration, XMPPTCPConnectionConfiguration.Builder> descriptor
        = new XmppConnectionDescriptor<>(XmppNioTcpConnection.class, XMPPTCPConnectionConfiguration.class);

        Configuration sinttestConfiguration = Configuration.builder().setService("example.org").build();
        XmppNioTcpConnection connection = descriptor.construct(sinttestConfiguration);

        assertEquals("example.org", connection.getXMPPServiceDomain().toString());
    }
}
