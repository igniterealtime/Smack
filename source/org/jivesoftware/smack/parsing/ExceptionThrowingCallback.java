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

import org.jivesoftware.smack.ConnectionListener;

/**
 * Parsing exception callback class that simply throws the encountered parsing exception. This usually leads to an
 * {@link ConnectionListener#connectionClosedOnError(Exception)} disconnect of the connection.
 * 
 * @author Florian Schmaus
 * 
 */
public class ExceptionThrowingCallback extends ParsingExceptionCallback {

    @Override
    public void handleUnparsablePacket(UnparsablePacket packetData) throws Exception {
        throw packetData.getParsingException();
    }
}
