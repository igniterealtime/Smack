/**
 *
 * Copyright 2017 Florian Schmaus
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
package org.jivesoftware.smackx.httpfileupload.element;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager;

public class Slot extends IQ {

    public static final String ELEMENT = "slot";
    public static final String NAMESPACE = HttpFileUploadManager.NAMESPACE;

    private final URL putUrl;
    private final URL getUrl;

    Slot(URL putUrl, URL getUrl, String namespace) {
        super(ELEMENT, namespace);
        setType(Type.result);
        this.putUrl = putUrl;
        this.getUrl = getUrl;
    }

    public Slot(URL putUrl, URL getUrl) {
        this(putUrl, getUrl, NAMESPACE);
    }

    public URL getPutUrl() {
        return putUrl;
    }

    public URL getGetUrl() {
        return getUrl;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(Collections.<String, String>emptyMap());
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();

        xml.element("put", getPutUrl().toString());
        xml.element("get", getGetUrl().toString());

        return xml;
    }
}
