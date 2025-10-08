/*
 *
 * Copyright 2019 Florian Schmaus.
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

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil {

    public static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        // CHECKSTYLE:OFF
        throwable.printStackTrace(printWriter);
        // CHECKSTYLE:ON

        printWriter.flush();

        StringBuffer stringBuffer = stringWriter.getBuffer();
        return stringBuffer.toString();
    }

}
