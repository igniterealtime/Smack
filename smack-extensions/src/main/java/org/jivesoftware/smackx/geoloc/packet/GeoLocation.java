/**
 *
 * Copyright 2015-2017 Ishan Khanna, Fernando Ramirez, 2019 Florian Schmaus
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

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.FormFieldChildElement;

/**
 * A GeoLocation Extension packet, which is used by the XMPP clients to exchange their respective geographic locations.
 *
 * @see <a href="http://www.xmpp.org/extensions/xep-0080.html">XEP-0080</a>
 * @author Ishan Khanna
 */
public final class GeoLocation implements Serializable, ExtensionElement, FormFieldChildElement {

    private static final long serialVersionUID = 1L;

    public static final String NAMESPACE = "http://jabber.org/protocol/geoloc";

    public static final String ELEMENT = "geoloc";

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    public static final GeoLocation EMPTY_GEO_LOCATION = GeoLocation.builder().build();

    private static final Logger LOGGER = Logger.getLogger(GeoLocation.class.getName());

    private final Double accuracy;
    private final Double alt;
    private final Double altAccuracy;
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

    private GeoLocation(Double accuracy, Double alt, Double altAccuracy, String area, Double bearing, String building, String country,
                    String countryCode, String datum, String description, Double error, String floor, Double lat,
                    String locality, Double lon, String postalcode, String region, String room, Double speed,
                    String street, String text, Date timestamp, String tzo, URI uri) {
        this.accuracy = accuracy;
        this.alt = alt;
        this.altAccuracy = altAccuracy;
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

    public Double getAltAccuracy() {
        return altAccuracy;
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
    public QName getQName() {
        return QNAME;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        xml.optElement("accuracy", accuracy);
        xml.optElement("alt", alt);
        xml.optElement("altaccuracy", altAccuracy);
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

    /**
     * Returns a new instance of {@link Builder}.
     * @return Builder
     */
    public static Builder builder() {
        return new GeoLocation.Builder();
    }

    @Override
    public boolean isExclusiveElement() {
        return true;
    }

    /**
     * Returns the first GeoLocation, or <code>null</code> if it doesn't exist in {@link Message}.
     * <br>
     * @param message The Message stanza containing GeoLocation
     * @return GeoLocation
     */
    public static GeoLocation from(Message message) {
        return message.getExtension(GeoLocation.class);
    }

    /**
     * Returns the first GeoLocation, or <code>null</code> if it doesn't exist in {@link FormField}.
     * <br>
     * @param formField the Formfield containing GeoLocation
     * @return GeoLocation
     */
    public static GeoLocation from(FormField formField) {
        return (GeoLocation) formField.getFormFieldChildElement(QNAME);
    }

    /**
     * This class defines a builder class for {@link GeoLocation}.
     * <br>
     * {@link GeoLocation} instance can be obtained using {@link #build()} method as follows.<br><br>
     * <code>GeoLocation.Builder builder = GeoLocation.builder(); <br>
     *       GeoLocation geoLocation = builder.build();</code>
     *  <br><br>
     *  To set GeoLocation parameters, use their respective setters.
     */
    public static class Builder {

        private Double accuracy;
        private Double alt;
        private Double altAccuracy;
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

        /**
         * Sets accuracy of horizontal GPS error in meters.
         *
         * @param accuracy accuracy in meters
         * @return Builder
         */
        public Builder setAccuracy(Double accuracy) {
            this.accuracy = accuracy;
            return this;
        }

        /**
         * Sets Altitude in meters above or below sea level.
         *
         * @param alt altitude in meters
         * @return Builder
         */
        public Builder setAlt(Double alt) {
            this.alt = alt;
            return this;
        }

        /**
         * Sets Vertical GPS error in meters.
         *
         * @param altAccuracy altAccuracy in meters
         * @return Builder
         */
        public Builder setAltAccuracy(Double altAccuracy) {
            this.altAccuracy = altAccuracy;
            return this;
        }

        /**
         * Sets a named area such as a campus or neighborhood.
         *
         * @param area the named area
         * @return Builder
         */
        public Builder setArea(String area) {
            this.area = area;
            return this;
        }

        /**
         * Sets GPS bearing (direction in which the entity is heading<br>
         * to reach its next waypoint), measured in decimal degrees,<br>
         * relative to true north.
         *
         * @param bearing bearing in decimal degrees
         * @return Builder
         */
        public Builder setBearing(Double bearing) {
            this.bearing = bearing;
            return this;
        }

        /**
         * Sets a specific building on a street or in an area.
         *
         * @param building name of the building
         * @return Builder
         */
        public Builder setBuilding(String building) {
            this.building = building;
            return this;
        }

        /**
         * Sets the nation where the user is located.
         *
         * @param country user's country of location
         * @return Builder
         */
        public Builder setCountry(String country) {
            this.country = country;
            return this;
        }

        /**
         * Sets The ISO 3166 two-letter country code.
         *
         * @param countryCode two-letter country code
         * @return Builder
         */
        public Builder setCountryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        /**
         * Sets GPS Datum.
         *
         * @param datum GPS datum
         * @return Builder
         */
        public Builder setDatum(String datum) {
            this.datum = datum;
            return this;
        }

        /**
         * Sets A natural-language name for or description of the location.
         *
         * @param description description of the location
         * @return Builder
         */
        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets Horizontal GPS error in arc minutes;<br>
         * this element is deprecated in favor of accuracy.
         *
         * @param error error in arc minutes
         * @return Builder
         */
        public Builder setError(Double error) {
            this.error = error;
            return this;
        }

        /**
         * Sets a particular floor in a building.
         *
         * @param floor floor in a building
         * @return Builder
         */
        public Builder setFloor(String floor) {
            this.floor = floor;
            return this;
        }

        /**
         * Sets Latitude in decimal degrees North.
         *
         * @param lat latitude in decimal degrees
         * @return Builder
         */
        public Builder setLat(Double lat) {
            this.lat = lat;
            return this;
        }

        /**
         * Sets Locality within the administrative region,<br>
         * such as a town or city.
         *
         * @param locality locality in a region
         * @return Builder
         */
        public Builder setLocality(String locality) {
            this.locality = locality;
            return this;
        }

        /**
         * Sets Longitude in decimal degrees East.
         *
         * @param lon longitude in decimal degrees
         * @return Builder
         */
        public Builder setLon(Double lon) {
            this.lon = lon;
            return this;
        }

        /**
         * Sets PostalCode used for postal delivery.
         *
         * @param postalcode code for postal delivery
         * @return Builder
         */
        public Builder setPostalcode(String postalcode) {
            this.postalcode = postalcode;
            return this;
        }

        /**
         * Sets an administrative region of the nation,<br>
         * such as a state or province.
         *
         * @param region an administrative region
         * @return Builder
         */
        public Builder setRegion(String region) {
            this.region = region;
            return this;
        }

        /**
         * Sets a particular room in a building.
         *
         * @param room room inside a building
         * @return Builder
         */
        public Builder setRoom(String room) {
            this.room = room;
            return this;
        }

        /**
         * Sets Speed at which the entity is moving, in meters per second.
         *
         * @param speed speed in meters per second
         * @return Builder
         */
        public Builder setSpeed(Double speed) {
            this.speed = speed;
            return this;
        }

        /**
         * Sets a thoroughfare within the locality, or a crossing of two thoroughfares.
         *
         * @param street name of the street
         * @return Builder
         */
        public Builder setStreet(String street) {
            this.street = street;
            return this;
        }

        /**
         * Sets a catch-all element that captures any other information about the location.
         *
         * @param text distinctive feature about the location
         * @return Builder
         */
        public Builder setText(String text) {
            this.text = text;
            return this;
        }

        /**
         * Sets UTC timestamp specifying the moment when the reading was taken.
         *
         * @param timestamp timestamp of the reading
         * @return Builder
         */
        public Builder setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Sets the time zone offset from UTC for the current location.
         *
         * @param tzo time zone offset
         * @return Builder
         */
        public Builder setTzo(String tzo) {
            this.tzo = tzo;
            return this;
        }

        /**
         * Sets URI or URL pointing to information about the location.
         *
         * @param uri uri to the location
         * @return Builder
         */
        public Builder setUri(URI uri) {
            this.uri = uri;
            return this;
        }

        /**
         * This method is called to build {@link GeoLocation} from the Builder.
         *
         * @return GeoLocation
         */
        public GeoLocation build() {

            return new GeoLocation(accuracy, alt, altAccuracy, area, bearing, building, country, countryCode, datum, description,
                            error, floor, lat, locality, lon, postalcode, region, room, speed, street, text, timestamp,
                            tzo, uri);
        }
    }
}
