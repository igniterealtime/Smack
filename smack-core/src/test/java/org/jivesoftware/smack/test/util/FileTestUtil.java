/**
 *
 * Copyright © 2018 Paul Schaub
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
package org.jivesoftware.smack.test.util;

import java.io.File;
import java.util.Stack;

public class FileTestUtil {

    /**
     * Returns a {@link File} pointing to a temporary directory. On unix like systems this might be {@code /tmp}
     * for example.
     * If {@code suffix} is not null, the returned file points to {@code <temp>/suffix}.
     *
     * @param suffix optional path suffix
     * @return temp directory
     */
    public static File getTempDir(String suffix) {
        String temp = System.getProperty("java.io.tmpdir");
        if (temp == null) {
            temp = "tmp";
        }

        if (suffix == null) {
            return new File(temp);
        } else {
            return new File(temp, suffix);
        }
    }

    /**
     * Recursively delete a directory and its contents.
     *
     * @param root root directory
     */
    public static void deleteDirectory(File root) {
        if (!root.exists()) {
            return;
        }
        File[] currList;
        Stack<File> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            if (stack.lastElement().isDirectory()) {
                currList = stack.lastElement().listFiles();
                if (currList != null && currList.length > 0) {
                    for (File curr : currList) {
                        stack.push(curr);
                    }
                } else {
                    stack.pop().delete();
                }
            } else {
                stack.pop().delete();
            }
        }
    }
}
