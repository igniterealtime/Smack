package org.jivesoftware.smackx.jingle.nat;

import java.net.DatagramPacket;

/**
 * Listener for datagram packets received.
 *
 * @author Thiago Camargo
 */
public interface DatagramListener {

    /**
     * Called when a datagram is received. If the method returns false, the
     * packet MUST NOT be resent from the received Channel.
     *
     * @param datagramPacket the datagram packet received.
     * @return ?
     */
    public boolean datagramReceived(DatagramPacket datagramPacket);

}