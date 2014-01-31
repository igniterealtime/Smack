package org.jivesoftware.smackx.workgroup;

import org.jivesoftware.smack.provider.UrlProviderFileInitializer;

public class WorkgroupProviderInitializer  extends UrlProviderFileInitializer {

    @Override
    protected String getFilePath() {
        return "classpath:META-INF/workgroup.providers";
    }
}
