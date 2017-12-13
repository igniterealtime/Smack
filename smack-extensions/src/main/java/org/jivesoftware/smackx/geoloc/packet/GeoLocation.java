/**
 *
 * Copyright 2015-2016 Ishan Khanna
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
package org.jivesoftware.smackx.geoloc.packet;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * A GeoLocation Extension packet, which is used by the XMPP clients to exchange their respective geographic locations.
 * 
 * @see <a href="http://www.xmpp.org/extensions/xep-0080.html">XEP-0080</a>
 * @author Ishan Khanna
 */
public final class GeoLocation implements Serializable, ExtensionElement {

    private static final long serialVersionUID = 1L;
    public static final String NAMESPACE = "http://jabber.org/protocol/geoloc";
    public static final String ELEMENT = "geoloc";

    private static final Logger LOGGER = Logger.getLogger(GeoLocation.class.getName());

    private final Double accuracy;
    private final Double alt;
    private final String area;
    private final Double bearing;
    private final String building;
    private final String country;
    private final String countryCode;
    private final String datum;
    private final String description;
    private final Double error;
    private final String floor;
    private final Double lat;
    private final String locality;
    private final Double lon;
    private final String postalcode;
    private final String region;
    private final String room;
    private final Double speed;
    private final String street;
    private final String text;
    private final Date timestamp;
    private final String tzo;
    private final URI uri;

    private GeoLocation(Double accuracy, Double alt, String area, Double bearing, String building, String country,
                    String countryCode, String datum, String description, Double error, String floor, Double lat,
                    String locality, Double lon, String postalcode, String region, String room, Double speed,
                    String street, String text, Date timestamp, String tzo, URI uri) {
        this.accuracy = accuracy;
        this.alt = alt;
        this.area = area;
        this.bearing = bearing;
        this.building = building;
        this.country = country;
        this.countryCode = countryCode;

        // If datum is not included, receiver MUST assume WGS84; receivers MUST implement WGS84; senders MAY use another
        // datum, but it is not recommended.

        if (StringUtils.isNullOrEmpty(datum)) {
            datum = "WGS84";
        }

        this.datum = datum;
        this.description = description;

        // error element is deprecated in favor of accuracy
        if (accuracy != null) {
            error = null;
            LOGGER.log(Level.WARNING,
                            "Error and accuracy set. Ignoring error as it is deprecated in favor of accuracy");
        }

        this.error = error;
        this.floor = floor;
        this.lat = lat;
        this.locality = locality;
        this.lon = lon;
        this.postalcode = postalcode;
        this.region = region;
        this.room = room;
        this.speed = speed;
        this.street = street;
        this.text = text;
        this.timestamp = timestamp;
        this.tzo = tzo;
        this.uri = uri;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public Double getAlt() {
        return alt;
    }

    public String getArea() {
        return area;
    }

    public Double getBearing() {
        return bearing;
    }

    public String getBuilding() {
        return building;
    }

    public String getCountry() {
        return country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getDatum() {
        return datum;
    }

    public String getDescription() {
        return description;
    }

    public Double getError() {
        return error;
    }

    public String getFloor() {
        return floor;
    }

    public Double getLat() {
        return lat;
    }

    public String getLocality() {
        return locality;
    }

    public Double getLon() {
        return lon;
    }

    public String getPostalcode() {
        return postalcode;
    }

    public String getRegion() {
        return region;
    }

    public String getRoom() {
        return room;
    }

    public Double getSpeed() {
        return speed;
    }

    public String getStreet() {
        return street;
    }

    public String getText() {
        return text;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getTzo() {
        return tzo;
    }

    public URI getUri() {
        return uri;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        xml.optElement("accuracy", accuracy);
        xml.optElement("alt", alt);
        xml.optElement("area", area);
        xml.optElement("bearing", bearing);
        xml.optElement("building", building);
        xml.optElement("country", country);
        xml.optElement("countrycode", countryCode);
        xml.optElement("datum", datum);
        xml.optElement("description", description);
        xml.optElement("error", error);
        xml.optElement("floor", floor);
        xml.optElement("lat", lat);
        xml.optElement("locality", locality);
        xml.optElement("lon", lon);
        xml.optElement("postalcode", postalcode);
        xml.optElement("region", region);
        xml.optElement("room", room);
        xml.optElement("speed", speed);
        xml.optElement("street", street);
        xml.optElement("text", text);
        xml.optElement("timestamp", timestamp);
        xml.optElement("tzo", tzo);
        xml.optElement("uri", uri);
        xml.closeElement(this);
        return xml;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public static Builder builder() {
        return new GeoLocation.Builder();
    }

    public static GeoLocation from(Message message) {
        return message.getExtension(ELEMENT, NAMESPACE);
    }

    public static class Builder {

        private Double accuracy;
        private Double alt;
        private String area;
        private Double bearing;
        private String building;
        private String country;
        private String countryCode;
        private String datum;
        private String description;
        private Double error;
        private String floor;
        private Double lat;
        private String locality;
        private Double lon;
        private String postalcode;
        private String region;
        private String room;
        private Double speed;
        private String street;
        private String text;
        private Date timestamp;
        private String tzo;
        private URI uri;

        public Builder setAccuracy(Double accuracy) {
            this.accuracy = accuracy;
            return this;
        }

        public Builder setAlt(Double alt) {
            this.alt = alt;
            return this;
        }

        public Builder setArea(String area) {
            this.area = area;
            return this;
        }

        public Builder setBearing(Double bearing) {
            this.bearing = bearing;
            return this;
        }

        public Builder setBuilding(String building) {
            this.building = building;
            return this;
        }

        public Builder setCountry(String country) {
            this.country = country;
            return this;
        }

        public Builder setCountryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public Builder setDatum(String datum) {
            this.datum = datum;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setError(Double error) {
            this.error = error;
            return this;
        }

        public Builder setFloor(String floor) {
            this.floor = floor;
            return this;
        }

        public Builder setLat(Double lat) {
            this.lat = lat;
            return this;
        }

        public Builder setLocality(String locality) {
            this.locality = locality;
            return this;
        }

        public Builder setLon(Double lon) {
            this.lon = lon;
            return this;
        }

        public Builder setPostalcode(String postalcode) {
            this.postalcode = postalcode;
            return this;
        }

        public Builder setRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder setRoom(String room) {
            this.room = room;
            return this;
        }

        public Builder setSpeed(Double speed) {
            this.speed = speed;
            return this;
        }

        public Builder setStreet(String street) {
            this.street = street;
            return this;
        }

        public Builder setText(String text) {
            this.text = text;
            return this;
        }

        public Builder setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder setTzo(String tzo) {
            this.tzo = tzo;
            return this;
        }

        public Builder setUri(URI uri) {
            this.uri = uri;
            return this;
        }

        public GeoLocation build() {

            return new GeoLocation(accuracy, alt, area, bearing, building, country, countryCode, datum, description,
                            error, floor, lat, locality, lon, postalcode, region, room, speed, street, text, timestamp,
                            tzo, uri);
        }

    }

}
