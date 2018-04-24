package org.jivesoftware.smack.initializer;

public final class SmackCoreInitializer extends UrlInitializer {

    @Override
    protected String getProvidersUri() {
        return "classpath:org.jivesoftware.smack/smack.providers";
    }

}
