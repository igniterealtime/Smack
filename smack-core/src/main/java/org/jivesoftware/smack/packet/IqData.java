/**
 *
 * Copyright 2019-2020 Florian Schmaus
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

import org.jivesoftware.smack.packet.id.StandardStanzaIdSource;
import org.jivesoftware.smack.packet.id.StanzaIdSource;
import org.jivesoftware.smack.util.Objects;

public final class IqData extends AbstractIqBuilder<IqData> {

    static final IqData EMPTY = new IqData(StandardStanzaIdSource.DEFAULT);

    IqData(StanzaIdSource stanzaIdSource) {
        super(stanzaIdSource);
    }

    IqData(String stanzaId) {
        super(stanzaId);
    }

    public IqData ofType(IQ.Type type) {
        this.type = Objects.requireNonNull(type);
        return getThis();
    }

    @Override
    public IqData getThis() {
        return this;
    }

    @Override
    public Stanza build() {
        throw new UnsupportedOperationException();
    }

    public static IqData createResponseData(IqView request) {
        return createResponse(request, IQ.ResponseType.result);
    }

    public static IqData createErrorResponseDAta(IqView request) {
        return createResponse(request, IQ.ResponseType.error);
    }
}
