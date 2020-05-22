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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.Objects;

public abstract class IqBuilder<IB extends IqBuilder<IB, I>, I extends IQ>
                extends AbstractIqBuilder<IB> {

    protected IqBuilder(IQ other, String stanzaId) {
        super(other, stanzaId);
    }

    protected IqBuilder(AbstractIqBuilder<?> other) {
        super(other);
    }

    protected IqBuilder(XMPPConnection connection) {
        super(connection.getStanzaFactory().getStanzaIdSource());
    }

    protected IqBuilder(String stanzaId) {
        super(stanzaId);
    }

    public IB ofType(IQ.Type type) {
        this.type = Objects.requireNonNull(type);
        return getThis();
    }

    public abstract I build();

}
