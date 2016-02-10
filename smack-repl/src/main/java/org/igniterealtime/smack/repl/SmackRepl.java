/**
 *
 * Copyright 2016 Florian Schmaus
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
package org.igniterealtime.smack.repl;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.util.dns.javax.JavaxResolver;

public class SmackRepl {

    public static void init() {
        SmackConfiguration.getVersion();
        // smack-repl also pulls in smack-resolver-minidns which has higher precedence the smack-resolver-javax but
        // won't work on Java SE platforms. Therefore explicitly setup JavaxResolver.
        JavaxResolver.setup();
        // CHECKSTYLE:OFF
        System.out.println("Smack REPL");
        // CHECKSTYLE:ON
    }

}
