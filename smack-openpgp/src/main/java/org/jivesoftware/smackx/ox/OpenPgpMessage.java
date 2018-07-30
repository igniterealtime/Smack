/**
 *
 * Copyright 2017 Florian Schmaus.
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
package org.jivesoftware.smackx.ox;

import java.io.IOException;
import java.nio.charset.Charset;

import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpContentElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.provider.OpenPgpContentElementProvider;

import org.pgpainless.decryption_verification.OpenPgpMetadata;
import org.xmlpull.v1.XmlPullParserException;

/**
 * This class embodies a decrypted {@link OpenPgpElement}.
 */
public class OpenPgpMessage {

    public enum State {
        /**
         * Represents a {@link SigncryptElement}.
         */
        signcrypt,
        /**
         * Represents a {@link SignElement}.
         */
        sign,
        /**
         * Represents a {@link CryptElement}.
         */
        crypt,
        ;
    }

    private final String element;
    private final State state;
    private final OpenPgpMetadata metadata;

    private OpenPgpContentElement openPgpContentElement;

    /**
     * Constructor.
     *
     * @param content XML representation of the decrypted {@link OpenPgpContentElement}.
     * @param state {@link State} of the {@link OpenPgpContentElement}.
     * @param metadata Metadata about the encryption.
     */
    public OpenPgpMessage(String content, State state, OpenPgpMetadata metadata) {
        this.metadata = Objects.requireNonNull(metadata);
        this.state = Objects.requireNonNull(state);
        this.element = Objects.requireNonNull(content);
    }

    /**
     * Constructor.
     *
     * @param bytes bytes of the XML representation of the decrypted {@link OpenPgpContentElement}.
     * @param state {@link State} of the {@link OpenPgpContentElement}.
     * @param metadata metadata about the encryption.
     */
    public OpenPgpMessage(byte[] bytes, State state, OpenPgpMetadata metadata) {
        this(new String(Objects.requireNonNull(bytes), Charset.forName("UTF-8")), state, metadata);
    }

    /**
     * Return the decrypted {@link OpenPgpContentElement} of this message.
     * To determine, whether the element is a {@link SignElement}, {@link CryptElement} or {@link SigncryptElement},
     * please consult {@link #getState()}.
     *
     * @return {@link OpenPgpContentElement}
     * @throws XmlPullParserException if the parser encounters an error.
     * @throws IOException if the parser encounters an error.
     */
    public OpenPgpContentElement getOpenPgpContentElement() throws XmlPullParserException, IOException {
        ensureOpenPgpContentElementSet();

        return openPgpContentElement;
    }

    private void ensureOpenPgpContentElementSet() throws XmlPullParserException, IOException {
        if (openPgpContentElement != null)
            return;

        openPgpContentElement = OpenPgpContentElementProvider.parseOpenPgpContentElement(element);
        if (openPgpContentElement == null) {
            return;
        }

        // Determine the state of the content element.
        if (openPgpContentElement instanceof SigncryptElement) {
            if (state != State.signcrypt) {
                throw new IllegalStateException("OpenPgpContentElement was signed and encrypted, but is not a SigncryptElement.");
            }
        } else if (openPgpContentElement instanceof SignElement) {
            if (state != State.sign) {
                throw new IllegalStateException("OpenPgpContentElement was signed and unencrypted, but is not a SignElement.");
            }
        } else if (openPgpContentElement instanceof CryptElement) {
            if (state != State.crypt) {
                throw new IllegalStateException("OpenPgpContentElement was unsigned and encrypted, but is not a CryptElement.");
            }
        }
    }

    /**
     * Return the state of the message. This value determines, whether the message was a {@link SignElement},
     * {@link CryptElement} or {@link SigncryptElement}.
     *
     * @return state of the content element.
     * @throws IOException if the parser encounters an error.
     * @throws XmlPullParserException if the parser encounters and error.
     */
    public State getState() throws IOException, XmlPullParserException {
        ensureOpenPgpContentElementSet();
        return state;
    }

    /**
     * Return metadata about the encrypted message.
     *
     * @return metadata
     */
    public OpenPgpMetadata getMetadata() {
        return metadata;
    }
}
