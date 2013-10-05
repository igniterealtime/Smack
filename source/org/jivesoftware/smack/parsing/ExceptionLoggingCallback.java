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
 * Simple parsing exception callback that only logs the encountered parsing exception to stderr.
 * 
 * @author Florian Schmaus
 * 
 */
public class ExceptionLoggingCallback extends ParsingExceptionCallback {

    @Override
    public void handleUnparsablePacket(UnparsablePacket unparsed) throws Exception {
        System.err.print("Smack message parsing exception: " + unparsed.getParsingException().getMessage());
        unparsed.getParsingException().printStackTrace();
        System.err.println("Unparsed content: " + unparsed.getContent());
    }
}
