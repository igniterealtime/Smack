/**
 *
 * Copyright 2017-2022 Paul Schaub
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
package org.jivesoftware.smackx.jingle_filetransfer.component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.jivesoftware.smackx.hashes.HashManager;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChild;
import org.jivesoftware.smackx.jingle_filetransfer.element.Range;

/**
 * Represent a file sent in a file transfer.
 * This can be both LocalFile (available to the client), or RemoteFile (file not yet available).
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class JingleFile extends JingleFileTransferChild {
    public JingleFile(Date date, String desc, HashElement hash, String mediaType, String name, int size) {
        super(date, desc, hash, mediaType, name, size, new Range(0, size));
    }

    public JingleFile(JingleFileTransferChild element) {
        super(element.getDate(), element.getDescription(), element.getHash(), element.getMediaType(),
                element.getName(), element.getSize(), element.getRange());
    }

    public static JingleFile fromFile(File file, String desc, String mediaType, HashManager.ALGORITHM hashAlgorithm) throws NoSuchAlgorithmException, IOException {
        HashElement hash = null;
        if (hashAlgorithm != null) {
            hash = calculateHash(file, hashAlgorithm);
        }
        return new JingleFile(new Date(file.lastModified()), desc, hash, mediaType, file.getName(), (int) file.length());
    }

    public static HashElement calculateHash(File file, HashManager.ALGORITHM algorithm) throws NoSuchAlgorithmException, IOException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File MUST NOT be null and MUST exist.");
        }

        MessageDigest digest = HashManager.getMessageDigest(algorithm);
        if (digest == null) {
            throw new NoSuchAlgorithmException("No algorithm for " + algorithm + " found.");
        }

        FileInputStream fi = new FileInputStream(file);
        DigestInputStream di = new DigestInputStream(fi, digest);

        while (di.available() > 0) {
            di.read();
        }

        byte[] d = di.getMessageDigest().digest();
        return new HashElement(algorithm, d);
    }

    public JingleFileTransferChild getElement() {
        JingleFileTransferChild.Builder builder = JingleFileTransferChild.getBuilder();
        builder.setDate(getDate());
        builder.setDescription(getDescription());
        builder.setHash(getHash());
        builder.setMediaType(getMediaType());
        builder.setName(getName());
        builder.setSize(getSize());
        builder.setRange(getRange());
        return builder.build();
    }
}
