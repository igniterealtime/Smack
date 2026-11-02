/*
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.omemo.element;

import java.util.List;

import org.jivesoftware.smack.packet.XmlElement;

/**
 * Header element of the message. The header contains information about the sender
 * and the encrypted keys for the recipients, as well as the iv element for AES.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public abstract class OmemoHeaderElement<T extends OmemoKeyElement> implements XmlElement {
    public static final String ELEMENT = "header";

    public static final String ATTR_SID = "sid";
    public static final String ATTR_IV = "iv";

    private final int sid;
    private final byte[] iv;

    public OmemoHeaderElement(int sid, byte[] iv) {
        this.sid = sid;
        this.iv = iv;
    }

    /**
     * Return the deviceId of the sender of the message.
     *
     * @return senders id
     */
    public int getSid() {
        return sid;
    }

    public byte[] getIv() {
        return iv != null ? iv.clone() : null;
    }

    public abstract List<T> getKeys();

    @Override
    public String getElementName() {
        return ELEMENT;
    }
}
