/**
 *
 * Copyright 2017 Florian Schmaus, 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {

    public static FileOutputStream prepareFileOutputStream(File file) throws IOException {
        if (!file.exists()) {

            // Create parent directory
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IOException("Cannot create directory " + parent.getAbsolutePath());
            }

            // Create file
            if (!file.createNewFile()) {
                throw new IOException("Cannot create file " + file.getAbsolutePath());
            }
        }

        if (file.isDirectory()) {
            throw new AssertionError("File " + file.getAbsolutePath() + " is not a file!");
        }

        return new FileOutputStream(file);
    }

    public static FileInputStream prepareFileInputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isFile()) {
                return new FileInputStream(file);
            } else {
                throw new IOException("File " + file.getAbsolutePath() + " is not a file!");
            }
        } else {
            throw new FileNotFoundException("File " + file.getAbsolutePath() + " not found.");
        }
    }
}
