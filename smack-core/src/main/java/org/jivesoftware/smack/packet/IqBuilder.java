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
package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.id.StandardStanzaIdSource;
import org.jivesoftware.smack.packet.id.StanzaIdSource;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.ToStringUtil;

public final class IqBuilder extends StanzaBuilder<IqBuilder> implements IqView {
    static final IqBuilder EMPTY = new IqBuilder(StandardStanzaIdSource.DEFAULT);

    IQ.Type type = Type.get;

    IqBuilder(StanzaIdSource stanzaIdSource) {
        super(stanzaIdSource);
    }

    IqBuilder(String stanzaId) {
        super(stanzaId);
    }

    @Override
    protected void addStanzaSpecificAttributes(ToStringUtil.Builder builder) {
        builder.addValue("type", type);
    }

    public IqBuilder ofType(IQ.Type type) {
        this.type = Objects.requireNonNull(type);
        return getThis();
    }

    @Override
    public IqBuilder getThis() {
        return this;
    }

    @Override
    public IQ.Type getType() {
        return type;
    }
}
