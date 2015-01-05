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

/**
 * A filter for IQ packet types. Returns true only if the packet is an IQ packet
 * and it matches the type provided in the constructor.
 * 
 * @author Alexander Wenckus
 * 
 */
public class IQTypeFilter extends FlexiblePacketTypeFilter<IQ> {
    
    public static final PacketFilter GET = new IQTypeFilter(Type.get);
    public static final PacketFilter SET = new IQTypeFilter(Type.set);
    public static final PacketFilter RESULT = new IQTypeFilter(Type.result);
    public static final PacketFilter ERROR = new IQTypeFilter(Type.error);
    public static final PacketFilter GET_OR_SET = new OrFilter(GET, SET);

	private final IQ.Type type;

	private IQTypeFilter(IQ.Type type) {
        super(IQ.class);
		this.type = type;
	}

    @Override
    protected boolean acceptSpecific(IQ iq) {
        return iq.getType() == type;
    }
}
