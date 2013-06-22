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
 * Smack provides 2 predefined ParsingExceptionCallback's: {@link LogException} and {@link ThrowException}.
 * 
 * @author Florian Schmaus
 * 
 */
public abstract class ParsingExceptionCallback {

    /**
     * Called when parsing an message stanza caused an exception.
     * 
     * @param e
     * the exception thrown while parsing the message stanza
     * @param message
     * the raw message stanza data that caused the exception
     * @throws Exception
     */
    public void messageParsingException(Exception e, UnparsedMessage message) throws Exception {
    }

    /**
     * Called when parsing an IQ stanza caused an exception.
     * 
     * @param e
     * the exception thrown while parsing the iq stanza
     * @param iq
     * the raw iq stanza data that caused the exception
     * @throws Exception
     */
    public void iqParsingException(Exception e, UnparsedIQ iq) throws Exception {
    }

    /**
     * Called when parsing a presence stanza caused an exception.
     * 
     * @param e
     * the exception thrown while parsing the presence stanza
     * @param presence
     * the raw presence stanza data that caused the exception
     * @throws Exception
     */
    public void presenceParsingException(Exception e, UnparsedPresence presence) throws Exception {
    }
}
