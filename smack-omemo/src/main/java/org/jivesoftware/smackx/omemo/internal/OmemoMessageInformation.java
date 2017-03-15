package org.jivesoftware.smackx.omemo.internal;

/**
 * Class that contains information about a decrypted message (eg. which key was used, if it was a carbon...)
 *
 * @param <T_IdKey> IdentityKey class
 * @author Paul Schaub
 */
public class OmemoMessageInformation<T_IdKey> {
    private T_IdKey senderIdentityKey;
    private OmemoDevice senderDevice;
    private CARBON carbon = CARBON.NONE;

    /**
     * Empty constructor
     */
    public OmemoMessageInformation() {
    }

    /**
     * Creates a new OmemoMessageInformation object
     *
     * @param senderIdentityKey identityKey of the sender device
     * @param senderDevice      device that sent the message
     * @param carbon            Carbon type
     */
    public OmemoMessageInformation(T_IdKey senderIdentityKey, OmemoDevice senderDevice, CARBON carbon) {
        this.senderIdentityKey = senderIdentityKey;
        this.senderDevice = senderDevice;
        this.carbon = carbon;
    }

    /**
     * Return the sender devices identityKey
     *
     * @return identityKey
     */
    public T_IdKey getSenderIdentityKey() {
        return senderIdentityKey;
    }

    /**
     * Set the sender devices identityKey
     *
     * @param senderIdentityKey identityKey
     */
    public void setSenderIdentityKey(T_IdKey senderIdentityKey) {
        this.senderIdentityKey = senderIdentityKey;
    }

    /**
     * Return the sender device
     *
     * @return sender device
     */
    public OmemoDevice getSenderDevice() {
        return senderDevice;
    }

    /**
     * Set the sender device
     *
     * @param senderDevice sender device
     */
    public void setSenderDevice(OmemoDevice senderDevice) {
        this.senderDevice = senderDevice;
    }

    /**
     * Return the carbon type
     *
     * @return carbon type
     */
    public CARBON getCarbon() {
        return carbon;
    }

    /**
     * Set the carbon type
     *
     * @param carbon carbon type
     */
    public void setCarbon(CARBON carbon) {
        this.carbon = carbon;
    }

    /**
     * Types of Carbon Messages
     */
    public enum CARBON {
        NONE,   //No carbon
        SENT,   //Sent carbon
        RECV    //Received Carbon
    }

    @Override
    public String toString() {
        return senderDevice.toString() + " " + carbon;
    }
}


