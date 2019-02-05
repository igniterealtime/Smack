/**
 *
 * Copyright 2015-2017 Ishan Khanna, Fernando Ramirez
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
package org.jivesoftware.smackx.geoloc.provider;

import java.io.IOException;
import java.text.ParseException;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;

import org.jivesoftware.smackx.geoloc.packet.GeoLocation;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class GeoLocationProvider extends ExtensionElementProvider<GeoLocation> {

    @Override
    public GeoLocation parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException,
                    ParseException {

        GeoLocation.Builder builder = GeoLocation.builder();

        outerloop: while (true) {
            int event = parser.next();
            switch (event) {
            case XmlPullParser.START_TAG:
                String name = parser.getName();
                switch (name) {
                case "accuracy":
                    builder.setAccuracy(ParserUtils.getDoubleFromNextText(parser));
                    break;
                case "alt":
                    builder.setAlt(ParserUtils.getDoubleFromNextText(parser));
                    break;
                case "altaccuracy":
                    builder.setAltAccuracy(ParserUtils.getDoubleFromNextText(parser));
                    break;
                case "area":
                    builder.setArea(parser.nextText());
                    break;
                case "bearing":
                    builder.setBearing(ParserUtils.getDoubleFromNextText(parser));
                    break;
                case "building":
                    builder.setBuilding(parser.nextText());
                    break;
                case "country":
                    builder.setCountry(parser.nextText());
                    break;
                case "countrycode":
                    builder.setCountryCode(parser.nextText());
                    break;
                case "datum":
                    builder.setDatum(parser.nextText());
                    break;
                case "description":
                    builder.setDescription(parser.nextText());
                    break;
                case "error":
                    builder.setError(ParserUtils.getDoubleFromNextText(parser));
                    break;
                case "floor":
                    builder.setFloor(parser.nextText());
                    break;
                case "lat":
                    builder.setLat(ParserUtils.getDoubleFromNextText(parser));
                    break;
                case "locality":
                    builder.setLocality(parser.nextText());
                    break;
                case "lon":
                    builder.setLon(ParserUtils.getDoubleFromNextText(parser));
                    break;
                case "postalcode":
                    builder.setPostalcode(parser.nextText());
                    break;
                case "region":
                    builder.setRegion(parser.nextText());
                    break;
                case "room":
                    builder.setRoom(parser.nextText());
                    break;
                case "speed":
                    builder.setSpeed(ParserUtils.getDoubleFromNextText(parser));
                    break;
                case "street":
                    builder.setStreet(parser.nextText());
                    break;
                case "text":
                    builder.setText(parser.nextText());
                    break;
                case "timestamp":
                    builder.setTimestamp(ParserUtils.getDateFromNextText(parser));
                    break;
                case "tzo":
                    builder.setTzo(parser.nextText());
                    break;
                case "uri":
                    builder.setUri(ParserUtils.getUriFromNextText(parser));
                    break;
                }
                break;
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
        }

        return builder.build();
    }

}
