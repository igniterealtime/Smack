/**
 *
 * Copyright 2019 Florian Schmaus.
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
package org.jivesoftware.smackx.bytestreams.socks5;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.jivesoftware.smack.SmackException;

import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.StreamHost;

public abstract class Socks5Exception extends SmackException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected Socks5Exception(String message) {
        super(message);
    }

    public static final class NoSocks5StreamHostsProvided extends Socks5Exception {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        NoSocks5StreamHostsProvided() {
            super("No SOCKS5 stream hosts provided.");
        }
    }

    public static final class CouldNotConnectToAnyProvidedSocks5Host extends Socks5Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        private final Map<StreamHost, Exception> streamHostsExceptions;

        private CouldNotConnectToAnyProvidedSocks5Host(String message, Map<StreamHost, Exception> streamHostsExceptions) {
            super(message);
            this.streamHostsExceptions = Collections.unmodifiableMap(streamHostsExceptions);
        }

        public Map<StreamHost, Exception> getStreamHostsExceptions() {
            return streamHostsExceptions;
        }

        static CouldNotConnectToAnyProvidedSocks5Host construct(Map<StreamHost, Exception> streamHostsExceptions) {
            assert !streamHostsExceptions.isEmpty();

            StringBuilder sb = new StringBuilder(256);
            sb.append("Could not establish socket with any provided SOCKS5 stream host.");
            Iterator<StreamHost> it = streamHostsExceptions.keySet().iterator();
            while (it.hasNext()) {
                StreamHost streamHost = it.next();
                Exception exception = streamHostsExceptions.get(streamHost);

                sb.append(' ').append(streamHost).append(" Exception: '").append(exception).append('\'');
                if (it.hasNext()) {
                    sb.append(',');
                }
            }

            String message = sb.toString();
            return new CouldNotConnectToAnyProvidedSocks5Host(message, streamHostsExceptions);
        }
    }
}
