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
package org.jivesoftware.smackx.provider;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Assert;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.ExtensionProviderInfo;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.IQProviderInfo;
import org.jivesoftware.smack.provider.ProviderFileLoader;
import org.jivesoftware.smack.provider.ProviderLoader;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

@SuppressWarnings("deprecation")
public class ProviderConfigTest {
    
    @Test
    public void addGenericLoaderProvider() {
        ProviderManager.getInstance().addLoader(new ProviderLoader() {
            
            @Override
            public Collection<IQProviderInfo> getIQProviderInfo() {
                ArrayList<IQProviderInfo> l = new ArrayList<IQProviderInfo>(1);
                l.add(new IQProviderInfo("provider", "test:provider",  new TestIQProvider()));
                return l;
            }
            
            @Override
            public Collection<ExtensionProviderInfo> getExtensionProviderInfo() {
                return null;
            }
        });
        
        Assert.assertNotNull(ProviderManager.getInstance().getIQProvider("provider", "test:provider"));
    }
    
    @Ignore // gradle migration
    @Test
    public void addClasspathFileLoaderProvider() throws Exception{
        ProviderManager.getInstance().addLoader(new ProviderFileLoader(FileUtils.getStreamForUrl("classpath:org/jivesoftware/smackx/provider/test.providers", null)));
        Assert.assertNotNull(ProviderManager.getInstance().getIQProvider("provider", "test:file_provider"));
    }

    public static class TestIQProvider implements IQProvider {

        @Override
        public IQ parseIQ(XmlPullParser parser) throws Exception {
            return null;
        }
        
    }
}
