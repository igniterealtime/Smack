/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2013 Florian Schmaus.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

/**
 * Base class to receive parsing exceptions.
 * 
 * If this class is used as callback, then Smack will silently ignore the stanza that caused the parsing exception and
 * place the parser after the faulty stanza.
 * 
 * Subclasses may or may not override certain methods of this class. Each of these methods will receive the exception
 * that caused the parsing error and an instance of an Unparsed Packet type. The latter can be used to inspect the
 * stanza that caused the parsing error by using the getContent() (for example {@link UnparsedIQ#getContent()})
 * method.
 * 
 * Smack provides 2 predefined ParsingExceptionCallback's: {@link ExceptionLoggingCallback} and {@link ExceptionThrowingCallback}.
 * 
 * @author Florian Schmaus
 * 
 */
public abstract class ParsingExceptionCallback {

    /**
     * Called when parsing an message stanza caused an exception.
     * 
     * @param stanzaData
     * the raw message stanza data that caused the exception
     * @throws Exception
     */
    public void handleUnparsablePacket(UnparsablePacket stanzaData) throws Exception {
    }
}
