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
package org.jivesoftware.smack.fsm;

import java.io.IOException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.c2s.internal.WalkStateGraphContext;

/**
 * Note that this is an non-static inner class of XmppClientToServerConnection so that states can inspect and modify
 * the connection.
 */
public abstract class State {

    protected final StateDescriptor stateDescriptor;

    protected final ModularXmppClientToServerConnectionInternal connectionInternal;

    protected State(StateDescriptor stateDescriptor, ModularXmppClientToServerConnectionInternal connectionInternal) {
        this.stateDescriptor = stateDescriptor;
        this.connectionInternal = connectionInternal;
    }

    /**
     * Check if the state should be activated.
     *
     * @param walkStateGraphContext the context of the current state graph walk.
     * @return <code>null</code> if the state should be activated.
     * @throws SmackException in case a Smack exception occurs.
     */
    public StateTransitionResult.TransitionImpossible isTransitionToPossible(WalkStateGraphContext walkStateGraphContext)
                    throws SmackException {
        return null;
    }

    public abstract StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext)
                    throws IOException, SmackException, InterruptedException, XMPPException;

    public StateDescriptor getStateDescriptor() {
        return stateDescriptor;
    }

    public void resetState() {
    }

    @Override
    public String toString() {
        return "State " + stateDescriptor + ' ' + connectionInternal.connection;
    }

    protected final void ensureNotOnOurWayToAuthenticatedAndResourceBound(
                    WalkStateGraphContext walkStateGraphContext) {
        if (walkStateGraphContext.isFinalStateAuthenticatedAndResourceBound()) {
            throw new IllegalStateException(
                            "Smack should never attempt to reach the authenticated and resource bound state over "
                                            + this
                                            + ". This is probably a programming error within Smack, please report it to the develoeprs.");
        }
    }

}
