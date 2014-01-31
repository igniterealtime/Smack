package org.jivesoftware.smackx.experimental;

import org.jivesoftware.smack.provider.UrlProviderFileInitializer;

public class ExperimentalProviderInitializer  extends UrlProviderFileInitializer {

    @Override
    protected String getFilePath() {
        return "classpath:META-INF/experimental.providers";
    }
}
