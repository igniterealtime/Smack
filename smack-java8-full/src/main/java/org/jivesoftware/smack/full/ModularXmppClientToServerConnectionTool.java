/**
 *
 * Copyright 2018-2020 Florian Schmaus
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
package org.jivesoftware.smack.full;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;

import org.jxmpp.stringprep.XmppStringprepException;

public class ModularXmppClientToServerConnectionTool {

    @SuppressWarnings("DefaultCharset")
    public static void main(String[] args) throws XmppStringprepException, FileNotFoundException {

        final PrintWriter pw;
        final boolean breakStateName;

        switch (args.length) {
        case 0:
            pw = new PrintWriter(System.out);
            breakStateName = false;
            break;
        case 1:
            Path outputFilePath = Paths.get(args[0]);
            File outputFile = outputFilePath.toFile();
            if (outputFile.exists()) {
                outputFile.delete();
            }
            pw = new PrintWriter(outputFile);
            breakStateName = true;
            break;
        default:
            throw new IllegalArgumentException("At most one argument allowed");
        }

        printStateGraph(pw, breakStateName);
        pw.flush();
    }

    public static void printStateGraph(PrintWriter pw, boolean breakStateName) throws XmppStringprepException {
        ModularXmppClientToServerConnectionConfiguration.Builder configurationBuilder = ModularXmppClientToServerConnectionConfiguration.builder()
                        .setUsernameAndPassword("user", "password")
                        .setXmppDomain("example.org");

        ModularXmppClientToServerConnectionConfiguration configuration = configurationBuilder.build();

        configuration.printStateGraphInDotFormat(pw, breakStateName);

        pw.flush();
    }

}
