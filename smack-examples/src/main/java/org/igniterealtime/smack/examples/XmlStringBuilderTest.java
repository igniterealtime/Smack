/**
 *
 * Copyright 2023 Florian Schmaus
 *
 * This file is part of smack-examples.
 *
 * smack-examples is free software; you can redistribute it and/or modify
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
package org.igniterealtime.smack.examples;

import java.util.function.Supplier;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class XmlStringBuilderTest {
    static int COUNT_OUTER = 500;
    static int COUNT_INNER = 50;

    public static void main(String[] args) throws Exception {
        test1();
        test2();
        test3();
    }

    public static void test1() throws Exception {
        // CHECKSTYLE:OFF
        System.out.println("Test 1");
        // CHECKSTYLE:ON
        XmlStringBuilder parent = new XmlStringBuilder();
        XmlStringBuilder child = new XmlStringBuilder();
        XmlStringBuilder child2 = new XmlStringBuilder();

        for (int i = 1; i < COUNT_OUTER; i++) {
            XmlStringBuilder cs = new XmlStringBuilder();
            for (int j = 0; j < COUNT_INNER; j++) {
                cs.append("abc");
            }
            child2.append((CharSequence) cs);
        }

        child.append((CharSequence) child2);
        parent.append((CharSequence) child);

        time("test1: parent", () -> "len=" + parent.toString().length());
        time("test1: child", () -> "len=" + child.toString().length());
        time("test1: child2", () -> "len=" + child2.toString().length());
    }

    public static void test2() throws Exception {
        // CHECKSTYLE:OFF
        System.out.println("Test 2: evaluate children first");
        // CHECKSTYLE:ON
        XmlStringBuilder parent = new XmlStringBuilder();
        XmlStringBuilder child = new XmlStringBuilder();
        XmlStringBuilder child2 = new XmlStringBuilder();

        for (int i = 1; i < COUNT_OUTER; i++) {
            XmlStringBuilder cs = new XmlStringBuilder();
            for (int j = 0; j < COUNT_INNER; j++) {
                cs.append("abc");
            }
            child2.append((CharSequence) cs);
        }

        child.append((CharSequence) child2);
        parent.append((CharSequence) child);

        time("test2: child2", () -> "len=" + child2.toString().length());
        time("test2: child", () -> "len=" + child.toString().length());
        time("test2: parent", () -> "len=" + parent.toString().length());
    }

    public static void test3() throws Exception {
        // CHECKSTYLE:OFF
        System.out.println("Test 3: use append(XmlStringBuilder)");
        // CHECKSTYLE:ON
        XmlStringBuilder parent = new XmlStringBuilder();
        XmlStringBuilder child = new XmlStringBuilder();
        XmlStringBuilder child2 = new XmlStringBuilder();

        for (int i = 1; i < COUNT_OUTER; i++) {
            XmlStringBuilder cs = new XmlStringBuilder();
            for (int j = 0; j < COUNT_INNER; j++) {
                cs.append("abc");
            }
            child2.append(cs);
        }

        child.append(child2);
        parent.append(child);

        time("test3: parent", () -> "len=" + parent.toString().length());
        time("test3: child", () -> "len=" + child.toString().length());
        time("test3: child2", () -> "len=" + child2.toString().length());
    }

    static void time(String name, Supplier<String> block) {
        long start = System.currentTimeMillis();
        String result = block.get();
        long end = System.currentTimeMillis();

        // CHECKSTYLE:OFF
        System.out.println(name + " took " + (end - start) + "ms: " + result);
        // CHECKSTYLE:ONy
    }
}
