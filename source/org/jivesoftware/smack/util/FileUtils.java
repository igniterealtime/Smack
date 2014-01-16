package org.jivesoftware.smack.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class FileUtils {

    private FileUtils() {
    }
    
    public static InputStream getStreamForUrl(String url, ClassLoader loader) throws MalformedURLException, IOException {
        URI fileUri = URI.create(url);
        
        if (fileUri.getScheme() == null) {
            throw new MalformedURLException("No protocol found in file URL: " + url);
        }
        
        if (fileUri.getScheme().equals("classpath")) {
            // Get an array of class loaders to try loading the providers files from.
            ClassLoader[] classLoaders = getClassLoaders();
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
     * @return an array of ClassLoader instances.
     */
    public static ClassLoader[] getClassLoaders() {
        ClassLoader[] classLoaders = new ClassLoader[2];
        classLoaders[0] = FileUtils.class.getClassLoader();
        classLoaders[1] = Thread.currentThread().getContextClassLoader();
        // Clean up possible null values. Note that #getClassLoader may return a null value.
        List<ClassLoader> loaders = new ArrayList<ClassLoader>();

        for (ClassLoader classLoader : classLoaders) {
            if (classLoader != null) {
                loaders.add(classLoader);
            }
        }
        return loaders.toArray(new ClassLoader[loaders.size()]);
    }

}
