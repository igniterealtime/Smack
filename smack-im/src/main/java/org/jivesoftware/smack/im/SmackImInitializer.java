/*
 *
 * Copyright © 2015 Florian Schmaus
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
package org.jivesoftware.smack.im;

import org.jivesoftware.smack.initializer.UrlInitializer;

public class SmackImInitializer extends UrlInitializer {

    @Override
    protected String getProvidersUri() {
        return "classpath:org.jivesoftware.smack.im/smackim.providers";
    }

    @Override
    protected String getConfigUri() {
        return "classpath:org.jivesoftware.smack.im/smackim.xml";
    }

}
