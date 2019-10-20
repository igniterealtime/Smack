/**
 *
 * Copyright 2019 Aditya Borikar.
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
package org.jivesoftware.smackx.usertune.element;

import java.net.URI;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.datatypes.UInt16;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * {@link ExtensionElement} that contains the UserTune. <br>
 * Instance of UserTuneElement can be created using {@link Builder#build()}
 * method.
 */
public final class UserTuneElement implements ExtensionElement {

    public static final String NAMESPACE = "http://jabber.org/protocol/tune";
    public static final String ELEMENT = "tune";
    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    private final String artist;
    private final UInt16 length;
    private final Integer rating;
    private final String source;
    private final String title;
    private final String track;
    private final URI uri;

    public static final UserTuneElement EMPTY_USER_TUNE = null;

    private UserTuneElement(Builder builder) {
        this.artist = builder.artist;
        this.length = builder.length;
        this.rating = builder.rating;
        this.source = builder.source;
        this.title = builder.title;
        this.track = builder.track;
        this.uri = builder.uri;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    public String getArtist() {
        return artist;
    }

    public UInt16 getLength() {
        return length;
    }

    public Integer getRating() {
        return rating;
    }

    public String getSource() {
        return source;
    }

    public String getTitle() {
        return title;
    }

    public String getTrack() {
        return track;
    }

    public URI getUri() {
        return uri;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        if (isEmptyUserTune()) {
            return xml.closeEmptyElement();
        }
        xml.rightAngleBracket();
        xml.optElement("artist", artist);
        xml.optElement("length", length);
        xml.optElement("rating", rating);
        xml.optElement("source", source);
        xml.optElement("title", title);
        xml.optElement("track", track);
        xml.optElement("uri", uri);
        return xml.closeElement(getElementName());
    }

    private boolean isEmptyUserTune() {
        return this.equals(EMPTY_USER_TUNE);
    }

    public static boolean hasUserTuneElement(Message message) {
        return message.hasExtension(UserTuneElement.class);
    }

    public static UserTuneElement from(Message message) {
        return message.getExtension(UserTuneElement.class);
    }

    @Override
    public int hashCode() {
        return HashCode.builder()
                .append(artist)
                .append(length)
                .append(rating)
                .append(source)
                .append(title)
                .append(track)
                .append(uri).build();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsUtil
                .equals(this, obj, (equalsBuilder, otherTune) -> equalsBuilder
                                                            .append(artist, otherTune.artist)
                                                            .append(length, otherTune.length)
                                                            .append(rating, otherTune.rating)
                                                            .append(source, otherTune.source)
                                                            .append(title, otherTune.title)
                                                            .append(track, otherTune.track)
                                                            .append(uri, otherTune.uri));
    }

    /**
     * Returns a new instance of {@link Builder}.
     * @return Builder
     */
    public static Builder getBuilder() {
        return new Builder();
    }

    /**
     * This class defines a Builder class for {@link UserTuneElement}. <br>
     * {@link UserTuneElement} instance can be obtained using the {@link #build()} method as follows. <br><br>
     * <pre>
     * {@code
     * UserTuneElement.Builder builder = UserTuneElement.getBuilder();
     * builder.setSource("Yessongs");
     * builder.setTitle("Heart of the Sunrise");
     * UserTuneElement userTuneElement = builder.build();
     * }
     * </pre>
     * Values such as title, source, artist, length, source, track and uri can be set using their respective setters through {@link Builder}.
     */
    public static final class Builder {
        private String artist;
        private UInt16 length;
        private Integer rating;
        private String source;
        private String title;
        private String track;
        private URI uri;

        private Builder() {
        }

        /**
         * Artist is an optional element in UserTuneElement.
         * @param artist.
         * @return builder.
         */
        public Builder setArtist(String artist) {
            this.artist = artist;
            return this;
        }

        /**
         * Length is an optional element in UserTuneElement.
         * @param length.
         * @return builder.
         */
        public Builder setLength(int length) {
            return setLength(UInt16.from(length));
        }

        /**
         * Length is an optional element in UserTuneElement.
         * @param length.
         * @return builder.
         */
        public Builder setLength(UInt16 length) {
            this.length = length;
            return this;
        }

        /**
         * Rating is an optional element in UserTuneElement.
         * @param rating.
         * @return builder.
         */
        public Builder setRating(int rating) {
            this.rating = rating;
            return this;
        }

        /**
         * Source is an optional element in UserTuneElement.
         * @param source.
         * @return builder.
         */
        public Builder setSource(String source) {
            this.source = source;
            return this;
        }

        /**
         * Title is an optional element in UserTuneElement.
         * @param title.
         * @return builder.
         */
        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        /**
         * Track is an optional element in UserTuneElement.
         * @param track.
         * @return builder.
         */
        public Builder setTrack(String track) {
            this.track = track;
            return this;
        }

        /**
         * URI is an optional element in UserTuneElement.
         * @param uri.
         * @return builder.
         */
        public Builder setUri(URI uri) {
            this.uri = uri;
            return this;
        }

        /**
         * This method is called to build a UserTuneElement.
         * @return UserTuneElement.
         */
        public UserTuneElement build() {
            return new UserTuneElement(this);
        }
    }
}
