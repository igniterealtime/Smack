/**
 *
 * Copyright 2019 Florian Schmaus
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

import java.net.URISyntaxException;
import java.text.ParseException;

public class SmackParsingException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected SmackParsingException(Exception exception) {
        super(exception);
    }

    public static class SmackTextParseException extends SmackParsingException {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public SmackTextParseException(ParseException parsingException) {
            super(parsingException);
        }
    }

    public static class SmackUriSyntaxParsingException extends SmackParsingException {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public SmackUriSyntaxParsingException(URISyntaxException uriSyntaxException) {
            super(uriSyntaxException);
        }
    }
}
