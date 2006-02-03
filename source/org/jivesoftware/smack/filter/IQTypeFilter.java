/**
 * 
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
