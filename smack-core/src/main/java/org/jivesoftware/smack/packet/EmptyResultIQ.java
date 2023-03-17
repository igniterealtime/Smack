/**
 *
 * Copyright © 2014-2023 Florian Schmaus
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
package org.jivesoftware.smack.packet;

public class EmptyResultIQ extends IQ {

    EmptyResultIQ(IqData iqBuilder) {
        super(iqBuilder, null, null);
    }

    // TODO: Deprecate when stanza builder and parsing logic is ready.
    public EmptyResultIQ() {
        super((String) null, null);
        setType(IQ.Type.result);
    }

    public EmptyResultIQ(IQ request) {
        this(AbstractIqBuilder.createResponse(request));
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        // Empty result IQs don't have an child elements
        return null;
    }
}
