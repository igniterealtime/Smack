/**
 *
 * Copyright 2017 Florian Schmaus
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
package org.jivesoftware.smackx.httpfileupload.element;

import org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager;
import org.jxmpp.jid.DomainBareJid;

public class SlotRequest_V0_2 extends SlotRequest {

    public static final String NAMESPACE = HttpFileUploadManager.NAMESPACE_0_2;

    public SlotRequest_V0_2(DomainBareJid uploadServiceAddress, String filename, long size) {
        this(uploadServiceAddress, filename, size, null);
    }

    /**
     * Create new slot request.
     *
     * @param uploadServiceAddress the XMPP address of the service to request the slot from.
     * @param filename name of file
     * @param size size of file in bytes
     * @param contentType file content type or null
     * @throws IllegalArgumentException if size is less than or equal to zero
     */
    public SlotRequest_V0_2(DomainBareJid uploadServiceAddress, String filename, long size, String contentType) {
        super(uploadServiceAddress, filename, size, contentType, NAMESPACE);
    }
}
