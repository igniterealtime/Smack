/**
 * Copyright 2013 Florian Schmaus
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * This class provides XMPP "zlib" compression with the help of the Deflater class of the Java API. Note that the method
 * needed is available since Java7, so it will only work with Java7 or higher (hence it's name).
 * 
 * @author Florian Schmaus
 * @see <a
 * href="http://docs.oracle.com/javase/7/docs/api/java/util/zip/Deflater.html#deflate(byte[], int, int, int)">The
 * required deflate() method</a>
 * 
 */
public class Java7ZlibInputOutputStream extends XMPPInputOutputStream {
    private final static Method method;
    private final static boolean supported;
    private final static int compressionLevel = Deflater.DEFAULT_STRATEGY;

    static {
        Method m = null;
        try {
            m = Deflater.class.getMethod("deflate", byte[].class, int.class, int.class, int.class);
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        }
        method = m;
        supported = (method != null);
    }

    public Java7ZlibInputOutputStream() {
        compressionMethod = "zlib";
    }

    @Override
    public boolean isSupported() {
        return supported;
    }

    @Override
    public InputStream getInputStream(InputStream inputStream) {
        return new InflaterInputStream(inputStream, new Inflater(), 512) {
            /**
             * Provide a more InputStream compatible version. A return value of 1 means that it is likely to read one
             * byte without blocking, 0 means that the system is known to block for more input.
             * 
             * @return 0 if no data is available, 1 otherwise
             * @throws IOException
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
        return new DeflaterOutputStream(outputStream, new Deflater(compressionLevel)) {
            public void flush() throws IOException {
                if (!supported) {
                    super.flush();
                    return;
                }
                int count = 0;
                if (!def.needsInput()) {
                    do {
                        count = def.deflate(buf, 0, buf.length);
                        out.write(buf, 0, count);
                    } while (count > 0);
                    out.flush();
                }
                try {
                    do {
                        count = (Integer) method.invoke(def, buf, 0, buf.length, 2);
                        out.write(buf, 0, count);
                    } while (count > 0);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Can't flush");
                } catch (IllegalAccessException e) {
                    throw new IOException("Can't flush");
                } catch (InvocationTargetException e) {
                    throw new IOException("Can't flush");
                }
                super.flush();
            }
        };
    }

}
