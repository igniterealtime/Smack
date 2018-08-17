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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FileUtils {

    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());

    public static InputStream getStreamForClasspathFile(String path, ClassLoader loader) throws IOException {
        // Get an array of class loaders to try loading the providers files from.
        List<ClassLoader> classLoaders = getClassLoaders();
        if (loader != null) {
            classLoaders.add(0, loader);
        }
        for (ClassLoader classLoader : classLoaders) {
            InputStream is = classLoader.getResourceAsStream(path);

            if (is != null) {
                return is;
            }
        }
        throw new IOException("Unable to get '" + path + "' from classpath. Tried ClassLoaders:" + classLoaders);
    }

    public static InputStream getStreamForUri(URI uri, ClassLoader loader) throws IOException {
        String protocol = uri.getScheme();
        if (protocol.equals("classpath")) {
            String path = uri.getSchemeSpecificPart();
            return getStreamForClasspathFile(path, loader);
        }

        URL url = uri.toURL();
        return url.openStream();
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

    public static boolean addLines(String uriString, Set<String> set) throws MalformedURLException, IOException {
        URI uri = URI.create(uriString);
        InputStream is = getStreamForUri(uri, null);
        InputStreamReader sr = new InputStreamReader(is, StringUtils.UTF8);
        BufferedReader br = new BufferedReader(sr);
        try {
            String line;
            while ((line = br.readLine()) != null) {
                set.add(line);
            }
        }
        finally {
            br.close();
        }
        return true;
    }

    /**
     * Reads the contents of a File.
     *
     * @param file
     * @return the content of file or null in case of an error
     * @throws IOException
     */
    @SuppressWarnings("DefaultCharset")
    public static String readFileOrThrow(File file) throws IOException {
        Reader reader = null;
        try {
            reader = new FileReader(file);
            char[] buf = new char[8192];
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
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.FINE, "readFile", e);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "readFile", e);
        }
        return null;
    }

    @SuppressWarnings("DefaultCharset")
    public static void writeFileOrThrow(File file, CharSequence content) throws IOException {
        FileWriter writer = new FileWriter(file, false);
        try {
            writer.write(content.toString());
        } finally {
            writer.close();
        }
    }

    public static boolean writeFile(File file, CharSequence content) {
        try {
            writeFileOrThrow(file, content);
            return true;
        }
        catch (IOException e) {
            LOGGER.log(Level.WARNING, "writeFile", e);
            return false;
        }
    }

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

    public static void maybeDeleteFileOrThrow(File file) throws IOException {
        if (!file.exists()) {
            return;
        }

        boolean successfullyDeleted = file.delete();
        if (!successfullyDeleted) {
            throw new IOException("Could not delete file " + file);
        }
    }
}
