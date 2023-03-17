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

import org.jivesoftware.smack.packet.id.StanzaIdSource;
import org.jivesoftware.smack.util.ToStringUtil;

public abstract class AbstractIqBuilder<IB extends AbstractIqBuilder<IB>> extends StanzaBuilder<IB> implements IqView {

    protected IQ.Type type = IQ.Type.get;

    AbstractIqBuilder(IQ other, String stanzaId) {
        super(other, stanzaId);
    }

    AbstractIqBuilder(AbstractIqBuilder<?> other) {
        super(other);
        type = other.type;
    }

    AbstractIqBuilder(StanzaIdSource stanzaIdSource) {
        super(stanzaIdSource);
    }

    AbstractIqBuilder(String stanzaId) {
        super(stanzaId);
    }

    public static IqData createResponse(IqView request) {
        return createResponse(request, IQ.ResponseType.result);
    }

    public static IqData createErrorResponse(IqView request) {
        return createResponse(request, IQ.ResponseType.error);
    }

    protected static IqData createResponse(IqView request, IQ.ResponseType responseType) {
        if (!request.isRequestIQ()) {
            throw new IllegalArgumentException("IQ request must be of type 'set' or 'get'. Original IQ: " + request);
        }

        IqData commonResponseIqData = buildResponse(request, s -> {
            return StanzaBuilder.buildIqData(s);
        });
        commonResponseIqData.ofType(responseType.getType());

        return commonResponseIqData;
    }

    @Override
    protected final void addStanzaSpecificAttributes(ToStringUtil.Builder builder) {
        builder.addValue("type", getType());
    }

    @Override
    public final IQ.Type getType() {
        return type;
    }
}
