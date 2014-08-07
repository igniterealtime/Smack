/**
 *
 * Copyright 2013 Florian Schmaus
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
package org.jivesoftware.smack.compression;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class XMPPInputOutputStream {

    protected static FlushMethod flushMethod;

    /**
     * Set the used flushed method when compressing data. The default is full flush which may not
     * achieve the best compression ratio, but provides better security against certain attacks.
     * Only use sync flush if you fully understand the implications.
     * 
     * @see <a href="https://blog.thijsalkema.de/blog/2014/08/07/https-attacks-and-xmpp-2-crime-and-breach/">Attacks against XMPP when using compression</a>
     * @param flushMethod
     */
    public static void setFlushMethod(FlushMethod flushMethod) {
        XMPPInputOutputStream.flushMethod = flushMethod;
    }

    protected String compressionMethod;

    public String getCompressionMethod() {
        return compressionMethod;
    }

    public abstract boolean isSupported();

    public abstract InputStream getInputStream(InputStream inputStream) throws Exception;

    public abstract OutputStream getOutputStream(OutputStream outputStream) throws Exception;

    public enum FlushMethod {
        FULL_FLUSH,
        SYNC_FLUSH,
    }
}
