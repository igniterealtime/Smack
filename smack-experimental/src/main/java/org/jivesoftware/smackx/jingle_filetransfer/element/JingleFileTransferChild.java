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
package org.jivesoftware.smackx.jingle_filetransfer.element;

import java.io.File;
import java.util.Date;

import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionChildElement;

/**
 * Content of type File.
 */
public class JingleFileTransferChild extends JingleContentDescriptionChildElement {
    public static final String ELEMENT = "file";
    public static final String ELEM_DATE = "date";
    public static final String ELEM_DESC = "desc";
    public static final String ELEM_MEDIA_TYPE = "media-type";
    public static final String ELEM_NAME = "name";
    public static final String ELEM_SIZE = "size";

    private final Date date;
    private final String desc;
    private final HashElement hash;
    private final String mediaType;
    private final String name;
    private final int size;
    private final Range range;

    public JingleFileTransferChild(Date date, String desc, HashElement hash, String mediaType, String name, int size, Range range) {
        this.date = date;
        this.desc = desc;
        this.hash = hash;
        this.mediaType = mediaType;
        this.name = name;
        this.size = size;
        this.range = range;
    }

    public Date getDate() {
        return date;
    }

    public String getDescription() {
        return desc;
    }

    public HashElement getHash() {
        return hash;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public Range getRange() {
        return range;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML(String enclosingNamespace) {
        XmlStringBuilder sb = new XmlStringBuilder(this);
        sb.rightAngleBracket();

        sb.optElement(ELEM_DATE, date);
        sb.optElement(ELEM_DESC, desc);
        sb.optElement(ELEM_MEDIA_TYPE, mediaType);
        sb.optElement(ELEM_NAME, name);
        sb.optElement(range);
        if (size > 0) {
            sb.element(ELEM_SIZE, Integer.toString(size));
        }
        sb.optElement(hash);
        sb.closeElement(this);
        return sb;
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private Date date;
        private String desc;
        private HashElement hash;
        private String mediaType;
        private String name;
        private int size;
        private Range range;

        private Builder() {
        }

        public Builder setDate(Date date) {
            this.date = date;
            return this;
        }

        public Builder setDescription(String desc) {
            this.desc = desc;
            return this;
        }

        public Builder setHash(HashElement hash) {
            this.hash = hash;
            return this;
        }

        public Builder setMediaType(String mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setSize(int size) {
            this.size = size;
            return this;
        }

        public Builder setRange(Range range) {
            this.range = range;
            return this;
        }

        public JingleFileTransferChild build() {
            return new JingleFileTransferChild(date, desc, hash, mediaType, name, size, range);
        }

        public Builder setFile(File file) {
            return setDate(new Date(file.lastModified()))
                    .setName(file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("/") + 1))
                    .setSize((int) file.length());
        }
    }
}
