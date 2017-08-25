/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jet.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.component.JingleSecurityBytestreamSession;

/**
 * Wrapper that wraps a the {@link InputStream} and {@link OutputStream} of a {@link BytestreamSession} into a
 * {@link CipherInputStream} and {@link CipherOutputStream}.
 */
public class JetSecurityBytestreamSession extends JingleSecurityBytestreamSession {
    private final Cipher cipher;

    public JetSecurityBytestreamSession(BytestreamSession session, Cipher cipher) {
        super(session);
        this.cipher = cipher;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new CipherInputStream(wrapped.getInputStream(), cipher);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new CipherOutputStream(wrapped.getOutputStream(), cipher);
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }
}
