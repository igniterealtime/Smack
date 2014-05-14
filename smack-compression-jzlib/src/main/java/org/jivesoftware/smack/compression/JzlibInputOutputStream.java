/**
 *
 * Copyright 2013-2014 Florian Schmaus
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

import com.jcraft.jzlib.*;
import org.jivesoftware.smack.SmackConfiguration;

/**
 * This class provides XMPP "zlib" compression with the help of JZLib.
 * 
 * @author Florian Schmaus
 * @see <a href="http://www.jcraft.com/jzlib/">JZLib</a>
 * 
 */
//@SuppressWarnings("deprecation")
public class JzlibInputOutputStream extends XMPPInputOutputStream {

    static {
        SmackConfiguration.addCompressionHandler(new JzlibInputOutputStream());
    }

    public JzlibInputOutputStream() {
        compressionMethod = "zlib";
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public InputStream getInputStream(InputStream inputStream) throws IOException {
        return new InflaterInputStream(inputStream);
    }

    @Override
    public OutputStream getOutputStream(OutputStream outputStream) throws IOException {
        return new DeflaterOutputStream(outputStream);
    }
}
