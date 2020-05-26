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
package org.jivesoftware.smack.compression;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XmppInputOutputFilter;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.AuthenticatedButUnboundStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.ResourceBindingStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModule;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.c2s.internal.WalkStateGraphContext;
import org.jivesoftware.smack.compress.packet.Compress;
import org.jivesoftware.smack.compress.packet.Compressed;
import org.jivesoftware.smack.compress.packet.Failure;
import org.jivesoftware.smack.fsm.State;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.fsm.StateTransitionResult;

public class CompressionModule extends ModularXmppClientToServerConnectionModule<CompressionModuleDescriptor> {

    protected CompressionModule(CompressionModuleDescriptor moduleDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        super(moduleDescriptor, connectionInternal);
    }

    public static final class CompressionStateDescriptor extends StateDescriptor {
        private CompressionStateDescriptor() {
            super(CompressionModule.CompressionState.class, 138);
            addPredeccessor(AuthenticatedButUnboundStateDescriptor.class);
            addSuccessor(AuthenticatedButUnboundStateDescriptor.class);
            declarePrecedenceOver(ResourceBindingStateDescriptor.class);
        }

        @Override
        protected CompressionModule.CompressionState constructState(ModularXmppClientToServerConnectionInternal connectionInternal) {
            CompressionModule compressionModule = connectionInternal.connection.getConnectionModuleFor(CompressionModuleDescriptor.class);
            return compressionModule.constructCompressionState(this, connectionInternal);
        }
    }

    private static final class CompressionState extends State {
        private XmppCompressionFactory selectedCompressionFactory;
        private XmppInputOutputFilter usedXmppInputOutputCompressionFitler;

        private CompressionState(StateDescriptor stateDescriptor, ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(stateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.TransitionImpossible isTransitionToPossible(
                        WalkStateGraphContext walkStateGraphContext) {
            final ConnectionConfiguration config = connectionInternal.connection.getConfiguration();
            if (!config.isCompressionEnabled()) {
                return new StateTransitionResult.TransitionImpossibleReason("Stream compression disabled by connection configuration");
            }

            Compress.Feature compressFeature = connectionInternal.connection.getFeature(Compress.Feature.ELEMENT, Compress.NAMESPACE);
            if (compressFeature == null) {
                return new StateTransitionResult.TransitionImpossibleReason("Stream compression not supported or enabled by service");
            }

            selectedCompressionFactory = XmppCompressionManager.getBestFactory(compressFeature);
            if (selectedCompressionFactory == null) {
                return new StateTransitionResult.TransitionImpossibleReason(
                                "No matching compression factory for " + compressFeature.getMethods());
            }

            usedXmppInputOutputCompressionFitler = selectedCompressionFactory.fabricate(config);

            return null;
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext)
                        throws InterruptedException, SmackException, XMPPException {
            final String compressionMethod = selectedCompressionFactory.getCompressionMethod();
            connectionInternal.sendAndWaitForResponse(new Compress(compressionMethod), Compressed.class, Failure.class);

            connectionInternal.addXmppInputOutputFilter(usedXmppInputOutputCompressionFitler);

            connectionInternal.newStreamOpenWaitForFeaturesSequence("server stream features after compression enabled");

            connectionInternal.setCompressionEnabled(true);

            return new CompressionTransitionSuccessResult(compressionMethod);
        }

        @Override
        public void resetState() {
            selectedCompressionFactory = null;
            usedXmppInputOutputCompressionFitler = null;
            connectionInternal.setCompressionEnabled(false);
        }
    }

    public static final class CompressionTransitionSuccessResult extends StateTransitionResult.Success {
        private final String compressionMethod;

        private CompressionTransitionSuccessResult(String compressionMethod) {
            super(compressionMethod + " compression enabled");
            this.compressionMethod = compressionMethod;
        }

        public String getCompressionMethod() {
            return compressionMethod;
        }
    }

    public CompressionState constructCompressionState(CompressionStateDescriptor compressionStateDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new CompressionState(compressionStateDescriptor, connectionInternal);
    }
}
