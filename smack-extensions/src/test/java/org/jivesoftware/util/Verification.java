/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.util;

import static org.junit.Assert.assertEquals;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;

/**
 * Implement this interface to verify a request/response pair.
 * <p>
 * For convenience there are some useful predefined implementations.
 * 
 * @param <T> class of the request
 * @param <S> class of the response
 * 
 * @author Henning Staib
 */
public interface Verification<T extends Stanza, S extends Stanza> {

    /**
     * Verifies that the "To" field of the request corresponds with the "From" field of
     * the response.
     */
    public static Verification<Stanza, Stanza> correspondingSenderReceiver = new Verification<Stanza, Stanza>() {

        @Override
        public void verify(Stanza request, Stanza response) {
            assertEquals(response.getFrom(), request.getTo());
        }

    };

    /**
     * Verifies that the type of the request is a GET.
     */
    public static Verification<IQ, Stanza> requestTypeGET = new Verification<IQ, Stanza>() {

        @Override
        public void verify(IQ request, Stanza response) {
            assertEquals(IQ.Type.get, request.getType());
        }

    };

    /**
     * Verifies that the type of the request is a SET.
     */
    public static Verification<IQ, Stanza> requestTypeSET = new Verification<IQ, Stanza>() {

        @Override
        public void verify(IQ request, Stanza response) {
            assertEquals(IQ.Type.set, request.getType());
        }

    };

    /**
     * Verifies that the type of the request is a RESULT.
     */
    public static Verification<IQ, Stanza> requestTypeRESULT = new Verification<IQ, Stanza>() {

        @Override
        public void verify(IQ request, Stanza response) {
            assertEquals(IQ.Type.result, request.getType());
        }

    };

    /**
     * Verifies that the type of the request is an ERROR.
     */
    public static Verification<IQ, Stanza> requestTypeERROR = new Verification<IQ, Stanza>() {

        @Override
        public void verify(IQ request, Stanza response) {
            assertEquals(IQ.Type.error, request.getType());
        }

    };

    /**
     * Implement this method to make assertions of the request/response pairs.
     * 
     * @param request the request collected by the mocked XMPP connection
     * @param response the response added to the protocol instance
     */
    public void verify(T request, S response);

}
