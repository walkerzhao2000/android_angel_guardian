package com.skydeck.angelguardian;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * XML parser of location requests and responses.
 */

class LocationXmlParser {

    static class AccessPoint {
        private String mac;
        private int signalStrength;
        AccessPoint(final String _mac, final int _signalStrength) {
            mac = _mac;
            signalStrength = _signalStrength;
        }
        String getMac() {
            return mac;
        }
        int getSignalStrength() {
            return signalStrength;
        }
    }

    static class StreetAddress {
        private double lat;
        private double lon;
        private int hpe;
        private int distance_to_point;
        private String street_number;
        private String address_line;
        private String city;
        private String postal_code;
        private String county;
        private String state;
        private String country;

        private StreetAddress(double lat, double lon, int hpe, int distance_to_point, String street_number,
                              String address_line, String city, String postal_code, String county,
                              String state, String country) {
            this.lat = lat;
            this.lon = lon;
            this.hpe = hpe;
            this.distance_to_point = distance_to_point;
            this.street_number = street_number;
            this.address_line = address_line;
            this.city = city;
            this.postal_code = postal_code;
            this.county = county;
            this.state = state;
            this.country = country;
        }

        StreetAddress() {
            this(0, 0, 0, 0, null, null, null, null, null, null, null);
        }

        StreetAddress(StreetAddress address) {
            lat = address.lat;
            lon = address.lon;
            hpe = address.hpe;
            distance_to_point = address.getDistance_to_point();
            street_number = address.getStreet_number();
            address_line = address.getAddress_line();
            city = address.getCity();
            postal_code = address.getPostal_code();
            county = address.getCounty();
            state = address.getState();
            country = address.getCountry();
        }

        double getLat() {
            return lat;
        }

        double getLon() {
            return lon;
        }

        int getHpe() {
            return hpe;
        }

        int getDistance_to_point() {
            return distance_to_point;
        }

        String getStreet_number() {
            return street_number;
        }

        String getAddress_line() {
            return address_line;
        }

        String getCity() {
            return city;
        }

        String getPostal_code() {
            return postal_code;
        }

        String getCounty() {
            return county;
        }

        String getState() {
            return state;
        }

        String getCountry() {
            return country;
        }

        void setLat(double _lat) {
            lat = _lat;
        }

        void setLon(double _lon) {
            lon = _lon;
        }

        void setHpe(int _hpe) {
            hpe = _hpe;
        }

        void setDistance_to_point(int _distance_to_point) {
            distance_to_point = _distance_to_point;
        }

        void setStreet_number(String _street_number) {
            street_number = _street_number;
        }

        void setAddress_line(String _address_line) {
            address_line = _address_line;
        }

        void setCity(String _city) {
            city = _city;
        }

        void setPostal_code(String _postal_code) {
            postal_code = _postal_code;
        }

        void setCounty(String _county) {
            county = _county;
        }

        void setState(String _state) {
            state = _state;
        }

        void setCountry(String _country) {
            country = _country;
        }
    }

    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String LOCATION_REQUEST = "<LocationRQ xmlns=\"http://skyhookwireless.com/wps/2005\" version=\"2.24\" street-address-lookup=\"full\">%s</LocationRQ>";
    private static final String AUTHENTICATION = "<authentication version=\"2.2\"><key key=\"%s\" username=\"%s\"/></authentication>";
    private static final String AP = "<access-point><mac>%s</mac><signal-strength>%s</signal-strength></access-point>";

    private static final String LAT = "latitude";
    private static final String LON = "longitude";
    private static final String HPE = "hpe";
    private static final String STREET_ADDRESS = "street-address";
    private static final String DISTANCE_TO_POINT = "distanceToPoint";
    private static final String STREET_NUMBER = "street-number";
    private static final String ADDRESS_LINE = "address-line";
    private static final String CITY = "metro1";
    private static final String POSTAL_CODE = "postal-code";
    private static final String COUNTY = "county";
    private static final String STATE = "state";
    private static final String COUNTRY = "country";

    private String username;
    private String key;
    private List<AccessPoint> aps;

    private XmlPullParser xmlParser;

    // constructor for parseLocationResponse()
    LocationXmlParser() {
        try {
            XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
            xmlFactory.setNamespaceAware(true);
            xmlParser = xmlFactory.newPullParser();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    // constructor for createLocationRequest()
    LocationXmlParser(final String _key, final String _username, final List<AccessPoint> _aps) {
        this();
        key = _key;
        username = _username;
        aps = _aps;
    }

    String createLocationRequest() {
        String xml_aps = "";
        for (final AccessPoint ap : aps) {
            xml_aps += String.format(AP, ap.getMac(), ap.getSignalStrength());
        }
        String auth = String.format(AUTHENTICATION, key, username);
        return String.format(XML_HEADER + LOCATION_REQUEST, auth + xml_aps);
    }

    StreetAddress parseLocationResponse(final String locationResponse) {
        try {
            InputStream xmlStream = new ByteArrayInputStream(locationResponse.getBytes(Charset.defaultCharset()));
            xmlParser.setInput(xmlStream, null);
            int event = xmlParser.getEventType();

            StreetAddress streetAddr = new StreetAddress();
            String text = "";
            while (event != XmlPullParser.END_DOCUMENT)  {
                String name=xmlParser.getName();
                switch (event){
                    case XmlPullParser.START_TAG:
                        if(name.equals(STREET_ADDRESS)){
                            final String dtp = xmlParser.getAttributeName(0);
                            if (dtp.equals(DISTANCE_TO_POINT)) {
                                streetAddr.setDistance_to_point((int)Double.parseDouble(xmlParser.getAttributeValue(0)));
                            }
                        }
                        break;
                    case XmlPullParser.TEXT:
                        text = xmlParser.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        if(name.equals(LAT)){
                            streetAddr.setLat(Double.parseDouble(text));
                        }
                        if(name.equals(LON)){
                            streetAddr.setLon(Double.parseDouble(text));
                        }
                        if(name.equals(HPE)){
                            streetAddr.setHpe(Integer.parseInt(text));
                        }
                        if (name.equalsIgnoreCase(STREET_NUMBER)) {
                            streetAddr.setStreet_number(text);
                        }
                        if (name.equalsIgnoreCase(ADDRESS_LINE)) {
                            streetAddr.setAddress_line(text);
                        }
                        if (name.equalsIgnoreCase(CITY)) {
                            streetAddr.setCity(text);
                        }
                        if (name.equalsIgnoreCase(POSTAL_CODE)) {
                            streetAddr.setPostal_code(text);
                        }
                        if (name.equalsIgnoreCase(COUNTY)) {
                            streetAddr.setCounty(text);
                        }
                        if (name.equalsIgnoreCase(STATE)) {
                            streetAddr.setState(text);
                        }
                        if (name.equalsIgnoreCase(COUNTRY)) {
                            streetAddr.setCountry(text);
                        }
                        break;
                    default:
                        break;
                }
                event = xmlParser.next();
            }
            return streetAddr;
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
