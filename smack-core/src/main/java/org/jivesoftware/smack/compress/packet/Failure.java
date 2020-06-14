/**
 *
 * Copyright 2018-2020 Florian Schmaus
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
package org.jivesoftware.smack.compress.packet;

import java.util.Objects;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class Failure implements Nonza {

    public static final String ELEMENT = "failure";
    public static final String NAMESPACE = Compress.NAMESPACE;
    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    public enum CompressFailureError {
        setup_failed,
        processing_failed,
        unsupported_method,
        ;

        private final String compressFailureError;

        CompressFailureError() {
            compressFailureError = name().replace('_', '-');
        }

        @Override
        public String toString() {
            return compressFailureError;
        }
    }

    private final CompressFailureError compressFailureError;
    private final StanzaError stanzaError;

    public Failure(CompressFailureError compressFailureError) {
        this(compressFailureError, null);
    }

    public Failure(CompressFailureError compressFailureError, StanzaError stanzaError) {
        this.compressFailureError = Objects.requireNonNull(compressFailureError);
        this.stanzaError = stanzaError;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public CompressFailureError getCompressFailureError() {
        return compressFailureError;
    }

    public StanzaError getStanzaError() {
        return stanzaError;
    }

    @Override
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this, enclosingNamespace);
        xml.rightAngleBracket();

        xml.emptyElement(compressFailureError);
        xml.optElement(stanzaError);

        xml.closeElement(this);
        return xml;
    }
}
