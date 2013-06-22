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

import org.jivesoftware.smack.packet.IQ;

/**
 * Representation of an unparsed IQ stanza.
 * 
 * @author Florian Schmaus
 *
 */
public class UnparsedIQ extends IQ {
    private final String content;
    private final Exception e;

    public UnparsedIQ(final String content, final Exception e) {
        this.content = content;
        this.e = e;
    }

    /**
     * 
     * @return the exception that caused the parser to fail
     */
    public Exception getException() {
        return e;
    }

    /**
     * Retrieve the raw stanza data
     * 
     * @return the raw stanza data
     */
    public String getContent() {
        return content;
    }

    @Override
    public String getChildElementXML() {
        return null;
    }

}
