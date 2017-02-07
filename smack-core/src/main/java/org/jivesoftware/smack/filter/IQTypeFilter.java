/**
 *
 * Copyright 2003-2006 Jive Software.
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
package org.jivesoftware.smack.filter;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.util.Objects;

/**
 * A filter for IQ stanza(/packet) types. Returns true only if the stanza(/packet) is an IQ packet
 * and it matches the type provided in the constructor.
 * 
 * @author Alexander Wenckus
 * 
 */
public final class IQTypeFilter extends FlexibleStanzaTypeFilter<IQ> {

    public static final StanzaFilter GET = new IQTypeFilter(Type.get);
    public static final StanzaFilter SET = new IQTypeFilter(Type.set);
    public static final StanzaFilter RESULT = new IQTypeFilter(Type.result);
    public static final StanzaFilter ERROR = new IQTypeFilter(Type.error);
    public static final StanzaFilter GET_OR_SET = new OrFilter(GET, SET);

    private final IQ.Type type;

    private IQTypeFilter(IQ.Type type) {
        super(IQ.class);
        this.type = Objects.requireNonNull(type, "Type must not be null");
    }

    @Override
    protected boolean acceptSpecific(IQ iq) {
        return iq.getType() == type;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": type=" + type;
    }
}
