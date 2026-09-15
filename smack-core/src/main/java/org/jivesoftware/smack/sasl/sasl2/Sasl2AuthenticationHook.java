/*
 *
 * Copyright 2026 Florian Schmaus
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
package org.jivesoftware.smack.sasl.sasl2;

import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.fsm.LoginContext;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.packet.Sasl2Feature;
import org.jivesoftware.smack.sasl.sasl2.Sasl2Authentication.Sasl2AuthenticationResult;

/**
 * Hook interface allowing connection modules and extensions to participate in the SASL2 (XEP-0388)
 * authentication lifecycle.
 */
public interface Sasl2AuthenticationHook {

    /**
     * Provide a skip reason if the candidate SASL mechanism cannot or should not be selected.
     *
     * @param mechanism the candidate SASL mechanism.
     * @param sasl2Feature the announced SASL2 stream feature.
     * @param configuration the connection configuration.
     * @return null if the mechanism is eligible, or a non-null string describing why it should be skipped.
     */
    default String getSkipReason(SASLMechanism mechanism, Sasl2Feature sasl2Feature, ConnectionConfiguration configuration) {
        return null;
    }

    /**
     * Prepare or initialize the selected mechanism instance before authentication text is generated.
     * For example, provide credentials or tokens to token-based mechanisms.
     *
     * @param mechanism the selected SASL mechanism instance.
     * @param sasl2Feature the announced SASL2 stream feature.
     * @throws SmackException in case of an error.
     */
    default void prepareSelectedMechanism(SASLMechanism mechanism, Sasl2Feature sasl2Feature) throws SmackException {
    }

    /**
     * Add child elements to the {@code <authenticate/>} nonza.
     *
     * @param sasl2Feature the announced SASL2 stream feature.
     * @param loginContext the current login context.
     * @param extensions mutable list of extensions for the {@code <authenticate/>} nonza.
     * @throws SmackException in case of an error.
     */
    default void addAuthenticateExtensions(Sasl2Feature sasl2Feature, LoginContext loginContext, List<XmlElement> extensions)
                    throws SmackException {
    }

    /**
     * Called when SASL2 authentication succeeds.
     *
     * @param result the result of SASL2 authentication.
     * @param authenticateExtensions the extensions that were included in the {@code <authenticate/>} request.
     * @throws SmackException in case of an error.
     */
    default void onSasl2Success(Sasl2AuthenticationResult result, Collection<? extends XmlElement> authenticateExtensions)
                    throws SmackException {
    }

    /**
     * Called when SASL2 authentication fails with a {@link SASLErrorException}.
     * Allows a hook to handle the failure (e.g. invalidate tokens) and request a graceful fallback / retry.
     *
     * @param failure the SASL error exception.
     * @param failedMechanism the mechanism that was attempted.
     * @param sasl2Feature the announced SASL2 stream feature.
     * @param attemptedExtensions the extensions that were included in the failed {@code <authenticate/>} request.
     * @return a {@link Sasl2Fallback} describing fallback extensions and mechanism filter, or null if this hook does not handle fallback.
     */
    default Sasl2Fallback onSasl2Failure(SASLErrorException failure, SASLMechanism failedMechanism,
                                         Sasl2Feature sasl2Feature, Collection<? extends XmlElement> attemptedExtensions) {
        return null;
    }
}
