/**
 *
 * Copyright 2018-2020 Florian Schmaus
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

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.tcp.XmppTcpTransportModuleDescriptor;

import org.junit.Test;
import org.jxmpp.stringprep.XmppStringprepException;

public class SmackIntegrationTestXmppConnectionManagerTest {

    @Test
    public void simpleXmppConnectionDescriptorTest() throws ClassNotFoundException, NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            KeyManagementException, NoSuchAlgorithmException, XmppStringprepException, InstantiationException {
        XmppConnectionDescriptor<
            ModularXmppClientToServerConnection,
            ModularXmppClientToServerConnectionConfiguration,
            ModularXmppClientToServerConnectionConfiguration.Builder
        > descriptor = XmppConnectionDescriptor.buildWith(
                        ModularXmppClientToServerConnection.class,
                        ModularXmppClientToServerConnectionConfiguration.class,
                        ModularXmppClientToServerConnectionConfiguration.Builder.class)
            .applyExtraConfguration(b -> b.removeAllModules().addModule(XmppTcpTransportModuleDescriptor.class))
            .build();

        Configuration sinttestConfiguration = Configuration.builder().setService("example.org").build();
        ModularXmppClientToServerConnection connection = descriptor.construct(sinttestConfiguration);

        assertEquals("example.org", connection.getXMPPServiceDomain().toString());
    }
}
