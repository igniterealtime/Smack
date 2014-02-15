package org.jivesoftware.smackx;

import org.jivesoftware.smack.initializer.UrlProviderFileInitializer;

public class WorkgroupProviderInitializer  extends UrlProviderFileInitializer {

    @Override
    protected String getFilePath() {
        return "classpath:org.jivesoftware.smackx/workgroup.providers";
    }
}
