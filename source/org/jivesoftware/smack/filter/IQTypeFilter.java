/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2006 Jive Software.
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
package org.jivesoftware.smack.filter;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;

/**
 * A filter for IQ packet types. Returns true only if the packet is an IQ packet
 * and it matches the type provided in the constructor.
 * 
 * @author Alexander Wenckus
 * 
 */
public class IQTypeFilter implements PacketFilter {

	private IQ.Type type;

	public IQTypeFilter(IQ.Type type) {
		this.type = type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jivesoftware.smack.filter.PacketFilter#accept(org.jivesoftware.smack.packet.Packet)
	 */
	public boolean accept(Packet packet) {
		return (packet instanceof IQ && ((IQ) packet).getType().equals(type));
	}
}
