/**
 *
 * Copyright 2013-2019 Florian Schmaus.
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
package org.jivesoftware.smack.parsing;

import java.io.IOException;

import org.jivesoftware.smack.UnparseableStanza;

/**
 * Interface to receive parsing exceptions.
 * <p>
 * If this class is used as callback, then Smack will silently ignore the stanza that caused the parsing exception and
 * place the parser after the faulty stanza.
 * </p>
 * <p>
 * Smack provides 2 predefined ParsingExceptionCallback's: {@link ExceptionLoggingCallback} and {@link ExceptionThrowingCallback}.
 * </p>
 *
 * @author Florian Schmaus
 *
 */
public interface ParsingExceptionCallback {

    /**
     * Called when parsing a stanza caused an exception.
     *
     * @param stanzaData the raw stanza data that caused the exception
     * @throws IOException if an I/O error occurred.
     */
    void handleUnparsableStanza(UnparseableStanza stanzaData) throws IOException;

}
