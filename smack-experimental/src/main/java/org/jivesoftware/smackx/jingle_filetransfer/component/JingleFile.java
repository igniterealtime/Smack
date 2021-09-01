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
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChildElement;

/**
 * Represent a file sent in a file transfer. This class contains metadata of the transferred file.
 */
public class JingleFile {

    private String name, description, mediaType;
    private long size;
    private Date date;
    private HashElement hashElement;

    public static JingleFile fromFile(File file, String description, String mediaType, HashManager.ALGORITHM hashAlgorithm) throws NoSuchAlgorithmException, IOException {

        HashElement hashElement = null;
        if (hashAlgorithm != null) {
            hashElement = calculateHash(file, hashAlgorithm);
        }

        return new JingleFile(file.getName(), description, file.length(), mediaType, new Date(file.lastModified()), hashElement);
    }

    public JingleFile(String name, String description, long size, String mediaType, Date date, HashElement hashElement) {
        this.name = name;
        this.description = description;
        this.size = size;
        this.mediaType = mediaType;
        this.date = date;
        this.hashElement = hashElement;
    }

    public JingleFile(JingleFileTransferChildElement element) {
        this.name = element.getName();
        this.description = element.getDescription();
        this.size = element.getSize();
        this.mediaType = element.getMediaType();
        this.date = element.getDate();
        this.hashElement = element.getHash();
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

    public JingleFileTransferChildElement getElement() {
        JingleFileTransferChildElement.Builder builder = JingleFileTransferChildElement.getBuilder();
        builder.setDate(getDate());
        builder.setSize(getSize());
        builder.setName(getName());
        builder.setDescription(getDescription());
        builder.setMediaType(getMediaType());
        builder.setHash(getHashElement());

        return builder.build();
    }

    public Date getDate() {
        return date;
    }

    public long getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getMediaType() {
        return mediaType;
    }

    public HashElement getHashElement() {
        return hashElement;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setHashElement(HashElement hashElement) {
        this.hashElement = hashElement;
    }
}
