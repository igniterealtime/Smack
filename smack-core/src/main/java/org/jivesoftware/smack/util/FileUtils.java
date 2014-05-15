/**
 *
 * Copyright the original author or authors
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FileUtils {

    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());

    public static InputStream getStreamForUrl(String url, ClassLoader loader) throws MalformedURLException, IOException {
        URI fileUri = URI.create(url);

        if (fileUri.getScheme() == null) {
            throw new MalformedURLException("No protocol found in file URL: " + url);
        }

        if (fileUri.getScheme().equals("classpath")) {
            // Get an array of class loaders to try loading the providers files from.
            List<ClassLoader> classLoaders = getClassLoaders();
            if (loader != null) {
                classLoaders.add(0, loader);
            }
            for (ClassLoader classLoader : classLoaders) {
                InputStream is = classLoader.getResourceAsStream(fileUri.getSchemeSpecificPart());

                if (is != null) {
                    return is;
                }
            }
        }
        else {
            return fileUri.toURL().openStream();
        }
        return null;
    }

    /**
     * Returns default classloaders.
     *
     * @return a List of ClassLoader instances.
     */
    public static List<ClassLoader> getClassLoaders() {
        ClassLoader[] classLoaders = new ClassLoader[2];
        classLoaders[0] = FileUtils.class.getClassLoader();
        classLoaders[1] = Thread.currentThread().getContextClassLoader();

        // Clean up possible null values. Note that #getClassLoader may return a null value.
        List<ClassLoader> loaders = new ArrayList<ClassLoader>(classLoaders.length);
        for (ClassLoader classLoader : classLoaders) {
            if (classLoader != null) {
                loaders.add(classLoader);
            }
        }
        return loaders;
    }

    public static boolean addLines(String url, Set<String> set) throws MalformedURLException, IOException {
        InputStream is = getStreamForUrl(url, null);
        if (is == null) return false;
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = br.readLine()) != null) {
            set.add(line);
        }
        return true;
    }

    /**
     * Reads the contents of a File
     *
     * @param file
     * @return the content of file or null in case of an error
     * @throws IOException 
     */
    public static String readFileOrThrow(File file) throws FileNotFoundException, IOException {
        Reader reader = null;
        try {
            reader = new FileReader(file);
            char buf[] = new char[8192];
            int len;
            StringBuilder s = new StringBuilder();
            while ((len = reader.read(buf)) >= 0) {
                s.append(buf, 0, len);
            }
            return s.toString();
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public static String readFile(File file) {
        try {
            return readFileOrThrow(file);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "readFile", e);
        }
        return null;
    }

    public static void writeFileOrThrow(File file, String content) throws IOException {
        FileWriter writer = new FileWriter(file, false);
        writer.write(content);
        writer.close();
    }

    public static boolean writeFile(File file, String content) {
        try {
            writeFileOrThrow(file, content);
            return true;
        }
        catch (IOException e) {
            LOGGER.log(Level.WARNING, "writeFile", e);
            return false;
        }
    }
}
