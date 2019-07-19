/**
 *
 * Copyright 2018 Florian Schmaus
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
package org.jivesoftware.smack;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

import org.jivesoftware.smack.SmackReactor.ChannelSelectedCallback;
import org.jivesoftware.smack.fsm.AbstractXmppStateMachineConnection;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.fsm.StateDescriptorGraph.GraphVertex;

public abstract class AbstractXmppNioConnection extends AbstractXmppStateMachineConnection {

    protected AbstractXmppNioConnection(ConnectionConfiguration configuration, GraphVertex<StateDescriptor> initialStateDescriptorVertex) {
        super(configuration, initialStateDescriptorVertex);
    }

    protected SelectionKey registerWithSelector(SelectableChannel channel, int ops, ChannelSelectedCallback callback)
            throws ClosedChannelException {
        return SMACK_REACTOR.registerWithSelector(channel, ops, callback);
    }

    /**
     * Set the interest Ops of a SelectionKey. Since Java's NIO interestOps(int) can block at any time, we use a queue
     * to perform the actual operation in the reactor where we can perform this operation non-blocking.
     *
     * @param selectionKey
     * @param interestOps
     */
    protected void setInterestOps(SelectionKey selectionKey, int interestOps) {
        SMACK_REACTOR.setInterestOps(selectionKey, interestOps);
    }

}
