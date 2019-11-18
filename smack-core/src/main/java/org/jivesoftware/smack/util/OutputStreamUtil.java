/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smack.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamUtil {

    public static void writeByteSafe(OutputStream outputStream, int i, String message) throws IOException {
        if (i < 0 || i > 0xff) {
            throw new IOException(message + ". The value " + i + " is not within the allowed range for bytes");
        }
        outputStream.write(i);
    }

    public static void writeResetAndFlush(ByteArrayOutputStream byteArrayOutputStream, OutputStream outputStream)
                    throws IOException {
        byteArrayOutputStream.writeTo(outputStream);
        byteArrayOutputStream.reset();
        outputStream.flush();
    }

}
