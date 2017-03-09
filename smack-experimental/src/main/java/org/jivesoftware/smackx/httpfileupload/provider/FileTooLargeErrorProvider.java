/**
 *
 * Copyright © 2017 Grigory Fedorov
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
package org.jivesoftware.smackx.httpfileupload.provider;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.httpfileupload.element.FileTooLargeError;
import org.jivesoftware.smackx.httpfileupload.element.FileTooLargeError_V0_2;
import org.xmlpull.v1.XmlPullParser;

/**
 * Provider for File Too Large error extension.
 *
 * @author Grigory Fedorov
 * @see <a href="http://xmpp.org/extensions/xep-0363.html">XEP-0363: HTTP File Upload</a>
 */
public class FileTooLargeErrorProvider extends ExtensionElementProvider<FileTooLargeError> {

    @Override
    public FileTooLargeError parse(XmlPullParser parser, int initialDepth) throws Exception {
        final String namespace = parser.getNamespace();
        Long maxFileSize = null;

        outerloop: while(true) {
            int event = parser.next();

            switch (event) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    switch(name) {
                        case "max-file-size":
                            maxFileSize = Long.valueOf(parser.nextText());
                            break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
            }
        }

        switch (namespace) {
        case FileTooLargeError.NAMESPACE:
            return new FileTooLargeError(maxFileSize);
        case FileTooLargeError_V0_2.NAMESPACE:
            return new FileTooLargeError_V0_2(maxFileSize);
        default:
            throw new AssertionError();
        }
    }
}
