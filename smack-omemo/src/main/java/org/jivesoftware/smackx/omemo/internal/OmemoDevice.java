/**
 * Copyright the original author or authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.omemo.internal;

import org.jxmpp.jid.BareJid;

/**
 * Class that combines a BareJid and a deviceId.
 *
 * @author Paul Schaub
 */
public class OmemoDevice {
    private final BareJid jid;
    private final int deviceId;

    public OmemoDevice(BareJid jid, int deviceId) {
        this.jid = jid;
        this.deviceId = deviceId;
    }

    /**
     * Return the BareJid of the device owner
     *
     * @return bareJid
     */
    public BareJid getJid() {
        return this.jid;
    }

    /**
     * Return the OMEMO device Id of the device
     *
     * @return deviceId
     */
    public int getDeviceId() {
        return this.deviceId;
    }

    @Override
    public String toString() {
        return jid.toString() + ":" + deviceId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OmemoDevice &&
                ((OmemoDevice) other).getJid().equals(this.getJid()) &&
                ((OmemoDevice) other).getDeviceId() == this.getDeviceId();
    }

    @Override
    public int hashCode() {
        Integer i;
        i = jid.hashCode() + deviceId;
        return i.hashCode();
    }
}
