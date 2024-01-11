/**
 *
 * Copyright 2017-2022 Paul Schaub
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
package org.jivesoftware.smackx.jingle.transports.jingle_s5b;

import org.jivesoftware.smackx.jingle.exception.FailedTransportException;

/**
 * Represents a S5B Transport Exception.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class S5BTransportException extends FailedTransportException {

    protected static final long serialVersionUID = 1L;

    private S5BTransportException(Throwable throwable) {
        super(throwable);
    }

    public static class CandidateError extends S5BTransportException {
        protected static final long serialVersionUID = 1L;

        CandidateError(Throwable throwable) {
            super(throwable);
        }
    }

    public static class ProxyError extends S5BTransportException {
        protected static final long serialVersionUID = 1L;

        ProxyError(Throwable throwable) {
            super(throwable);
        }
    }
}
