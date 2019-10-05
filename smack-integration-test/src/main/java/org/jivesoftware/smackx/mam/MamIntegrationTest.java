/**
 *
 * Copyright 2016 Fernando Ramirez, 2018-2019 Florian Schmaus
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
package org.jivesoftware.smackx.mam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.MessageWithBodiesFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;

import org.jivesoftware.smackx.mam.MamManager.MamQuery;
import org.jivesoftware.smackx.mam.MamManager.MamQueryArgs;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.EntityBareJid;

public class MamIntegrationTest extends AbstractSmackIntegrationTest {

    private final MamManager mamManagerConTwo;

    public MamIntegrationTest(SmackIntegrationTestEnvironment<?> environment) throws NoResponseException,
            XMPPErrorException, NotConnectedException, InterruptedException, TestNotPossibleException, NotLoggedInException {
        super(environment);

        mamManagerConTwo = MamManager.getInstanceFor(conTwo);

        if (!mamManagerConTwo.isSupported()) {
            throw new TestNotPossibleException("Message Archive Management (XEP-0313) is not supported by the server.");
        }

        // Make sure MAM is archiving messages.
        mamManagerConTwo.enableMamForAllMessages();
    }

    @SmackIntegrationTest
    public void mamTest() throws TimeoutException, Exception {
        EntityBareJid userOne = conOne.getUser().asEntityBareJid();
        EntityBareJid userTwo = conTwo.getUser().asEntityBareJid();

        Message message = new Message(userTwo);
        String messageId = message.ensureStanzaIdSet();
        final String messageBody = "Test MAM message (" + testRunId + ')';
        message.setBody(messageBody);

        final SimpleResultSyncPoint messageReceived = new SimpleResultSyncPoint();

        final StanzaListener stanzaListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza stanza) {
                Message message = (Message) stanza;
                if (message.getBody().equals(messageBody)) {
                    messageReceived.signal();
                }
            }
        };
        conTwo.addAsyncStanzaListener(stanzaListener, MessageWithBodiesFilter.INSTANCE);

        try {
            conOne.sendStanza(message);

            messageReceived.waitForResult(timeout);
        } finally {
            conTwo.removeAsyncStanzaListener(stanzaListener);
        }

        MamQueryArgs mamQueryArgs = MamQueryArgs.builder()
            .setResultPageSizeTo(1)
            .limitResultsToJid(userOne)
            .queryLastPage()
            .build();
        MamQuery mamQuery = mamManagerConTwo.queryArchive(mamQueryArgs);

        assertEquals(1, mamQuery.getMessages().size());

        Message mamMessage = mamQuery.getMessages().get(0);

        assertEquals(messageId, mamMessage.getStanzaId());
        assertEquals(messageBody, mamMessage.getBody());
        assertEquals(conOne.getUser(), mamMessage.getFrom());
        assertEquals(userTwo, mamMessage.getTo());
    }

    @SmackIntegrationTest
    public void mamPageTest() throws TimeoutException, Exception {
        final int messagesPerPage = 10;
        final int numPages = 3;
        final int totalMessages = messagesPerPage * numPages;
        final List<Message> outgoingMessages = new ArrayList<>(totalMessages);
        final EntityBareJid userOne = conOne.getUser().asEntityBareJid();
        final EntityBareJid userTwo = conTwo.getUser().asEntityBareJid();
        final SimpleResultSyncPoint allMessagesReceived = new SimpleResultSyncPoint();
        final String lastMessageArchiveUid = mamManagerConTwo.getMessageUidOfLatestMessage();

        for (int i = 0; i < totalMessages; i++) {
            String messageBody = "MAM Page Test " + testRunId + ' ' + (i + 1);
            Message message = new Message(userTwo, messageBody);
            outgoingMessages.add(message);
        }

        final String lastBody = outgoingMessages.get(outgoingMessages.size() - 1).getBody();

        final StanzaListener stanzaListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza stanza) {
                Message message = (Message) stanza;
                if (message.getBody().equals(lastBody)) {
                    allMessagesReceived.signal();
                }
            }
        };
        conTwo.addAsyncStanzaListener(stanzaListener, MessageWithBodiesFilter.INSTANCE);

        try {
            for (Message message : outgoingMessages) {
                conOne.sendStanza(message);
            }

            allMessagesReceived.waitForResult(timeout);
        } finally {
            conTwo.removeAsyncStanzaListener(stanzaListener);
        }

        MamQueryArgs mamQueryArgs = MamQueryArgs.builder()
                .setResultPageSize(messagesPerPage)
                .limitResultsToJid(userOne)
                .afterUid(lastMessageArchiveUid)
                .build();

        MamQuery mamQuery = mamManagerConTwo.queryArchive(mamQueryArgs);

        assertFalse(mamQuery.isComplete());
        assertEquals(messagesPerPage, mamQuery.getMessageCount());

        List<List<Message>> pages = new ArrayList<>(numPages);
        pages.add(mamQuery.getMessages());

        for (int additionalPageRequestNum = 0; additionalPageRequestNum < numPages - 1; additionalPageRequestNum++) {
            List<Message> page = mamQuery.pageNext(messagesPerPage);

            boolean isLastQuery = additionalPageRequestNum == numPages - 2;
            if (isLastQuery) {
                assertTrue(mamQuery.isComplete());
            } else {
                assertFalse(mamQuery.isComplete());
            }

            assertEquals(messagesPerPage, page.size());

            pages.add(page);
        }

        List<Message> queriedMessages = new ArrayList<>(totalMessages);
        for (List<Message> messages : pages) {
            queriedMessages.addAll(messages);
        }

        assertEquals(outgoingMessages.size(), queriedMessages.size());

        for (int i = 0; i < outgoingMessages.size(); i++) {
            Message outgoingMessage = outgoingMessages.get(i);
            Message queriedMessage = queriedMessages.get(i);

            assertEquals(outgoingMessage.getBody(), queriedMessage.getBody());
        }
    }
}
