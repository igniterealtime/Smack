/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.omemo.exceptions;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class MultipleCryptoFailedException extends CryptoFailedException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final List<CryptoFailedException> cryptoFailedExceptions;

    private MultipleCryptoFailedException(String message, List<CryptoFailedException> cryptoFailedExceptions) {
        super(message);
        this.cryptoFailedExceptions = Collections.unmodifiableList(cryptoFailedExceptions);
        if (cryptoFailedExceptions.isEmpty()) {
            throw new IllegalArgumentException("Exception list must not be empty.");
        }
    }

    public static MultipleCryptoFailedException from(List<CryptoFailedException> cryptoFailedExceptions) {
        StringBuilder sb = new StringBuilder("Multiple CryptoFailedExceptions: ");
        Iterator<CryptoFailedException> it = cryptoFailedExceptions.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        return new MultipleCryptoFailedException(sb.toString(), cryptoFailedExceptions);
    }

    public List<CryptoFailedException> getCryptoFailedExceptions() {
        return cryptoFailedExceptions;
    }
}
