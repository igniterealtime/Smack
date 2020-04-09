/**
 *
 * Copyright 2013-2020 Florian Schmaus
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * This class provides XMPP "zlib" compression with the help of the Deflater class of the Java API.
 * Note that the method needed for compression with synchronous flush support is available since
 * Java7, so it will only work with Java7 or higher (hence it's name). On Android, the required
 * <code>deflate()</code> method is available on API 19 or higher.
 * <p>
 * See also:
 * <ul>
  * <li><a href="http://docs.oracle.com/javase/7/docs/api/java/util/zip/Deflater.html#deflate(byte[],%20int,%20int,%20int)">The required deflate() method (Java7)</a>
 * <li><a href="http://developer.android.com/reference/java/util/zip/Deflater.html#deflate(byte[],%20int,%20int,%20int)">The required deflate() method (Android)</a>
 * </ul>
 *
 * @author Florian Schmaus
 */
public class Java7ZlibInputOutputStream extends XMPPInputOutputStream {
    private static final int compressionLevel = Deflater.DEFAULT_COMPRESSION;

    public Java7ZlibInputOutputStream() {
        super("zlib");
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public InputStream getInputStream(InputStream inputStream) {
        return new InflaterInputStream(inputStream, new Inflater(), 512) {
            /**
             * Provide a more InputStream compatible version. A return value of 1 means that it is likely to read one
             * byte without blocking, 0 means that the system is known to block for more input.
             *
             * @return 0 if no data is available, 1 otherwise
             * @throws IOException if an I/O error occurred.
             */
            @Override
            public int available() throws IOException {
                /*
                 * aSmack related remark (where KXmlParser is used):
                 * This is one of the funny code blocks. InflaterInputStream.available violates the contract of
                 * InputStream.available, which breaks kXML2.
                 *
                 * I'm not sure who's to blame, oracle/sun for a broken api or the google guys for mixing a sun bug with
                 * a xml reader that can't handle it....
                 *
                 * Anyway, this simple if breaks suns distorted reality, but helps to use the api as intended.
                 */
                if (inf.needsInput()) {
                    return 0;
                }
                return super.available();
            }
        };
    }

    @Override
    public OutputStream getOutputStream(OutputStream outputStream) {
        final int flushMethodInt;
        switch (flushMethod) {
        case SYNC_FLUSH:
            flushMethodInt = Deflater.SYNC_FLUSH;
            break;
        case FULL_FLUSH:
            flushMethodInt = Deflater.FULL_FLUSH;
            break;
        default:
            throw new AssertionError();
        }

        return new DeflaterOutputStream(outputStream, new Deflater(compressionLevel)) {
            @Override
            public void flush() throws IOException {
                int count;
                while ((count = def.deflate(buf, 0, buf.length, flushMethodInt)) > 0) {
                    out.write(buf, 0, count);
                }
                super.flush();
            }
        };
    }

}
