/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.ox.util.OpenPgpPubSubUtil;
import org.jivesoftware.smackx.pep.PepManager;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.jxmpp.jid.BareJid;

public abstract class AbstractOpenPgpIntegrationTest extends AbstractSmackIntegrationTest {

    protected final XMPPConnection aliceConnection;
    protected final XMPPConnection bobConnection;
    protected final XMPPConnection chloeConnection;

    protected final BareJid alice;
    protected final BareJid bob;
    protected final BareJid chloe;

    protected final PepManager alicePepManager;
    protected final PepManager bobPepManager;
    protected final PepManager chloePepManager;

    protected AbstractOpenPgpIntegrationTest(SmackIntegrationTestEnvironment<?> environment)
            throws XMPPException.XMPPErrorException, TestNotPossibleException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException {
        super(environment);

        throwIfPubSubNotSupported(conOne);
        throwIfPubSubNotSupported(conTwo);
        throwIfPubSubNotSupported(conThree);

        this.aliceConnection = conOne;
        this.bobConnection = conTwo;
        this.chloeConnection = conThree;

        this.alice = aliceConnection.getUser().asBareJid();
        this.bob = bobConnection.getUser().asBareJid();
        this.chloe = chloeConnection.getUser().asBareJid();

        this.alicePepManager = PepManager.getInstanceFor(aliceConnection);
        this.bobPepManager = PepManager.getInstanceFor(bobConnection);
        this.chloePepManager = PepManager.getInstanceFor(chloeConnection);

        OpenPgpPubSubUtil.deletePubkeysListNode(alicePepManager);
        OpenPgpPubSubUtil.deletePubkeysListNode(bobPepManager);
        OpenPgpPubSubUtil.deletePubkeysListNode(chloePepManager);
    }

    private static void throwIfPubSubNotSupported(XMPPConnection connection)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, TestNotPossibleException {
        if (!PepManager.getInstanceFor(connection).isSupported()) {
            throw new TestNotPossibleException("Server " + connection.getXMPPServiceDomain().toString() +
                    " does not support PEP.");
        }
    }
}
