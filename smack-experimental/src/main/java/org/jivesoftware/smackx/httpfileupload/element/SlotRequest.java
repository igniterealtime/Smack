/**
 *
 * Copyright © 2017 Grigory Fedorov
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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager;
import org.jxmpp.jid.DomainBareJid;

/**
 * Upload slot request.

 * @author Grigory Fedorov
 * @see <a href="http://xmpp.org/extensions/xep-0363.html">XEP-0363: HTTP File Upload</a>
 */
public class SlotRequest extends IQ {
    public static final String ELEMENT = "request";
    public static final String NAMESPACE = HttpFileUploadManager.NAMESPACE;

    private final String filename;
    private final long size;
    private final String contentType;

    public SlotRequest(DomainBareJid uploadServiceAddress, String filename, long size) {
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
    public SlotRequest(DomainBareJid uploadServiceAddress, String filename, long size, String contentType) {
        this(uploadServiceAddress, filename, size, contentType, NAMESPACE);
    }

    protected SlotRequest(DomainBareJid uploadServiceAddress, String filename, long size, String contentType, String namespace) {
        super(ELEMENT, namespace);

        if (size <= 0) {
            throw new IllegalArgumentException("File fileSize must be greater than zero.");
        }

        this.filename = filename;
        this.size = size;
        this.contentType = contentType;

        setType(Type.get);
        setTo(uploadServiceAddress);
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.element("filename", filename);
        xml.element("size", String.valueOf(size));
        xml.optElement("content-type", contentType);
        return xml;
    }
}
