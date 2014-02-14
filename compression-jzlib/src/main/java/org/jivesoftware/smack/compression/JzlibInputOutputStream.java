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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class provides XMPP "zlib" compression with the help of JZLib. Note that jzlib-1.0.7 must be used (i.e. in the
 * classpath), newer versions won't work!
 * 
 * @author Florian Schmaus
 * @see <a href="http://www.jcraft.com/jzlib/">JZLib</a>
 * 
 */
public class JzlibInputOutputStream extends XMPPInputOutputStream {

    private static Class<?> zoClass = null;
    private static Class<?> ziClass = null;

    static {
        try {
            zoClass = Class.forName("com.jcraft.jzlib.ZOutputStream");
            ziClass = Class.forName("com.jcraft.jzlib.ZInputStream");
        } catch (ClassNotFoundException e) {
        }
    }

    public JzlibInputOutputStream() {
        compressionMethod = "zlib";
    }

    @Override
    public boolean isSupported() {
        return (zoClass != null && ziClass != null);
    }

    @Override
    public InputStream getInputStream(InputStream inputStream) throws SecurityException, NoSuchMethodException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<?> constructor = ziClass.getConstructor(InputStream.class);
        Object in = constructor.newInstance(inputStream);

        Method method = ziClass.getMethod("setFlushMode", Integer.TYPE);
        method.invoke(in, 2);
        return (InputStream) in;
    }

    @Override
    public OutputStream getOutputStream(OutputStream outputStream) throws SecurityException, NoSuchMethodException,
            IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<?> constructor = zoClass.getConstructor(OutputStream.class, Integer.TYPE);
        Object out = constructor.newInstance(outputStream, 9);

        Method method = zoClass.getMethod("setFlushMode", Integer.TYPE);
        method.invoke(out, 2);
        return (OutputStream) out;
    }
}
