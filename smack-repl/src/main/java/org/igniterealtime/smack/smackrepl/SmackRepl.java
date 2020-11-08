/**
 *
 * Copyright 2016-2020 Florian Schmaus
 *
 * This file is part of smack-repl.
 *
 * smack-repl is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */
package org.igniterealtime.smack.smackrepl;

import org.jivesoftware.smack.Smack;
import org.jivesoftware.smack.util.dns.javax.JavaxResolver;

public class SmackRepl {

    public static void init() {
        Smack.ensureInitialized();
        // smack-repl also pulls in smack-resolver-minidns which has higher precedence the smack-resolver-javax but
        // won't work on Java SE platforms. Therefore explicitly setup JavaxResolver.
        JavaxResolver.setup();
        // CHECKSTYLE:OFF
        System.out.println("Smack REPL");
        // CHECKSTYLE:ON
    }

}
