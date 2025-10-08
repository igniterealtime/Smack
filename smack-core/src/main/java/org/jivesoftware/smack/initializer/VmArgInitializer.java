/*
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
package org.jivesoftware.smack.initializer;

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.provider.ProviderManager;


/**
 * Looks for a provider file location based on the VM argument <i>smack.provider.file</i>.  If it is supplied, its value will
 * be used as a file location for a providers file and loaded into the {@link ProviderManager} on Smack initialization.
 *
 * @author Robin Collier
 *
 */
public class VmArgInitializer extends UrlInitializer {

    protected String getFilePath() {
        return System.getProperty("smack.provider.file");
    }

    @Override
    public List<Exception> initialize() {
        if (getFilePath() != null) {
            super.initialize();
        }
        return Collections.emptyList();
    }
}
