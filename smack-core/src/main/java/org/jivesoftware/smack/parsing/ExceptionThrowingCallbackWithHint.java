/**
 *
 * Copyright 2018-2019 Florian Schmaus.
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
import java.util.logging.Logger;

import org.jivesoftware.smack.UnparseableStanza;

/**
 * Like {@link ExceptionThrowingCallback} but additionally logs a warning message.
 *
 * @author Florian Schmaus
 *
 */
public class ExceptionThrowingCallbackWithHint extends ExceptionThrowingCallback {

    private static final Logger LOGGER = Logger.getLogger(ExceptionThrowingCallbackWithHint.class.getName());

    @Override
    public void handleUnparsableStanza(UnparseableStanza packetData) throws IOException {
        LOGGER.warning("Parsing exception \"" + packetData.getParsingException().getMessage() + "\" encountered."
                        + " This exception will be re-thrown, leading to a disconnect."
                        + " You can change this behavior by setting a different ParsingExceptionCallback using setParsingExceptionCallback()."
                        + " More information an be found in AbstractXMPPConnection's javadoc.");

        super.handleUnparsableStanza(packetData);
    }
}
