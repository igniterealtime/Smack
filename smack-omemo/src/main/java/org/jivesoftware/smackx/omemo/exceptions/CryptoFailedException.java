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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception gets thrown when some cryptographic function failed.
 *
 * @author Paul Schaub
 */
public class CryptoFailedException extends Exception {

    private static final long serialVersionUID = 3466888654338119924L;

    private final ArrayList<Exception> exceptions = new ArrayList<>();

    public CryptoFailedException(String message) {
        super(message);
    }

    public CryptoFailedException(Exception e) {
        super(e);
        exceptions.add(e);
    }

    public List<Exception> getExceptions() {
        return Collections.unmodifiableList(exceptions);
    }
}
