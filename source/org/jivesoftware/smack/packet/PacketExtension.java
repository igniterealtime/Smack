package org.jivesoftware.smack.packet;

/**
 *
 * @author Matt Tucker
 */
public interface PacketExtension {

    public String getElementName();

    public String getNamespace();

    public String toXML();
}
