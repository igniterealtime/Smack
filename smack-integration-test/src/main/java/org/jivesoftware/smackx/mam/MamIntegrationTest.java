/**
 *
 * Copyright 2016 Fernando Ramirez, 2018-2021 Florian Schmaus
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.MessageWithBodiesFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.mam.MamManager.MamQuery;
import org.jivesoftware.smackx.mam.MamManager.MamQueryArgs;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.EntityBareJid;

@SpecificationReference(document = "XEP-0313", version = "0.6.3")
public class MamIntegrationTest extends AbstractSmackIntegrationTest {

    private final MamManager mamManagerConTwo;

    public MamIntegrationTest(SmackIntegrationTestEnvironment environment) throws NoResponseException,
            XMPPErrorException, NotConnectedException, InterruptedException, TestNotPossibleException, NotLoggedInException {
        super(environment);

        mamManagerConTwo = MamManager.getInstanceFor(conTwo);

        if (!mamManagerConTwo.isSupported()) {
            throw new TestNotPossibleException("Message Archive Management (XEP-0313) is not supported by the server.");
        }

        // Make sure MAM is archiving messages.
        try {
            mamManagerConTwo.enableMamForAllMessages();
        } catch (XMPPErrorException e) {
            // Note that we check for feature-not-implemented (and not service-unavailable), as the server understand
            // the MAM namespace, but may not the <prefs/> IQ.
            if (e.getStanzaError().getCondition() != StanzaError.Condition.feature_not_implemented) {
                throw e;
            }

            LOGGER.log(Level.INFO, conTwo.getXMPPServiceDomain() + " doesn't support XEP-0441: Message Archive Management Preferences", e);
        }
    }

    @SmackIntegrationTest
    public void mamTest() throws TimeoutException, Exception {
        EntityBareJid userOne = conOne.getUser().asEntityBareJid();
        EntityBareJid userTwo = conTwo.getUser().asEntityBareJid();

        final String messageBody = "Test MAM message (" + testRunId + ')';
        Message message = conTwo.getStanzaFactory().buildMessageStanza()
                        .to(userTwo)
                        .setBody(messageBody)
                        .build();
        final String messageId = message.getStanzaId();

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

        assertEquals(1, mamQuery.getMessages().size(), conTwo.getUser() + " received an unexpected amount of messages in response to a MAM query.");

        Message mamMessage = mamQuery.getMessages().get(0);

        assertEquals(messageId, mamMessage.getStanzaId(), "The message received by " + conTwo.getUser() + " via a MAM query has an unexpected stanza ID.");
        assertEquals(messageBody, mamMessage.getBody(), "The message received by " + conTwo.getUser() + " via a MAM query has an unexpected body.");
        assertEquals(conOne.getUser(), mamMessage.getFrom(), "The message received by " + conTwo.getUser() + " via a MAM query has an unexpected from-attribute value.");
        assertEquals(userTwo, mamMessage.getTo(), "The message received by " + conTwo.getUser() + " via a MAM query has an unexpected to-attribute value.");
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
            Message message = StanzaBuilder.buildMessage()
                    .to(userTwo)
                    .setBody(messageBody)
                    .build();
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

        assertFalse(mamQuery.isComplete(), "Expected the first MAM response received by " + conTwo.getUser() + " to NOT be complete (but it was).");
        assertEquals(messagesPerPage, mamQuery.getMessageCount(), "Unexpected message count in MAM response received by " + conTwo.getUser());

        List<List<Message>> pages = new ArrayList<>(numPages);
        pages.add(mamQuery.getMessages());

        for (int additionalPageRequestNum = 0; additionalPageRequestNum < numPages - 1; additionalPageRequestNum++) {
            List<Message> page = mamQuery.pageNext(messagesPerPage);

            boolean isLastQuery = additionalPageRequestNum == numPages - 2;
            if (isLastQuery) {
                assertTrue(mamQuery.isComplete(), "Expected the last MAM response received by " + conTwo.getUser() + " to be complete (but it was not).");
            } else {
                assertFalse(mamQuery.isComplete(), "Expected an intermediate MAM response received by " + conTwo.getUser() + " to NOT be complete (but it was).");
            }

            assertEquals(messagesPerPage, page.size(), "Unexpected amount of messages in the MAM response page received by " + conTwo.getUser());

            pages.add(page);
        }

        List<Message> queriedMessages = new ArrayList<>(totalMessages);
        for (List<Message> messages : pages) {
            queriedMessages.addAll(messages);
        }

        assertEquals(outgoingMessages.size(), queriedMessages.size(), "An unexpected total number of messages was received through MAM by " + conTwo.getUser());

        for (int i = 0; i < outgoingMessages.size(); i++) {
            Message outgoingMessage = outgoingMessages.get(i);
            Message queriedMessage = queriedMessages.get(i);

            assertEquals(outgoingMessage.getBody(), queriedMessage.getBody(), "Unexpected message body for message number " + (i + 1) + " as received by " + conTwo.getUser() + " (are messages received out of order?)");
        }
    }
}
