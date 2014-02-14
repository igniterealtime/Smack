package org.jivesoftware.smackx;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.provider.VCardProvider;

public class VCardUnitTest {

    @Test
    public void testNoWorkHomeSpecifier_EMAIL() throws Throwable {
        VCard card = VCardProvider.createVCardFromXML("<vcard><EMAIL><USERID>foo@fee.www.bar</USERID></EMAIL></vcard>");
        assertEquals("foo@fee.www.bar", card.getEmailHome());
    }

    @Test
    public void testNoWorkHomeSpecifier_TEL() throws Throwable {
        VCard card = VCardProvider.createVCardFromXML("<vcard><TEL><FAX/><NUMBER>3443233</NUMBER></TEL></vcard>");
        assertEquals("3443233", card.getPhoneWork("FAX"));
    }

    @Test
    public void testNoWorkHomeSpecifier_ADDR() throws Throwable {
        VCard card = VCardProvider.createVCardFromXML("<vcard><ADR><STREET>Some street</STREET><FF>ddss</FF></ADR></vcard>");
        assertEquals("Some street", card.getAddressFieldWork("STREET"));
        assertEquals("ddss", card.getAddressFieldWork("FF"));
    }

    @Test
    public void testFN() throws Throwable {
        VCard card = VCardProvider.createVCardFromXML("<vcard><FN>kir max</FN></vcard>");
        assertEquals("kir max", card.getField("FN"));
       // assertEquals("kir max", card.getFullName());
    }

    private final static String MIME_TYPE = "testtype";
    private final static String VCARD_XML = "<vcard><PHOTO><BINVAL>" + getAvatarEncoded() + "</BINVAL><TYPE>" + MIME_TYPE + "</TYPE></PHOTO></vcard>";
    @Test
    public void testPhoto() throws Throwable {
        VCard vc = VCardProvider.createVCardFromXML(VCARD_XML);
        byte[] avatar = vc.getAvatar();
        String mimeType = vc.getAvatarMimeType();
        assertEquals(mimeType, MIME_TYPE);
        
        byte[] expectedAvatar = getAvatarBinary();
        assertTrue(Arrays.equals(avatar, expectedAvatar));
    }

    public static byte[] getAvatarBinary() {
        return StringUtils.decodeBase64(getAvatarEncoded());
    }
    private static String getAvatarEncoded() {
        return "/9j/4AAQSkZJRgABAQEASABIAAD/4QAWRXhpZgAATU0AKgAAAAgAAAAAAAD/2wBDAAUDBAQEAwUE\n" +
                "BAQFBQUGBwwIBwcHBw8LCwkMEQ8SEhEPERETFhwXExQaFRERGCEYGh0dHx8fExciJCIeJBweHx7/\n" +
                "2wBDAQUFBQcGBw4ICA4eFBEUHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4e\n" +
                "Hh4eHh4eHh4eHh7/wAARCABQAFADASIAAhEBAxEB/8QAHAAAAgIDAQEAAAAAAAAAAAAABwgFBgID\n" +
                "BAkB/8QAORAAAgEDAwIDBwIDBwUAAAAAAQIDBAURAAYSITEHE0EIFBUiMlFxYbEjUqEkQoGR0eHw\n" +
                "M0NicsH/xAAZAQADAQEBAAAAAAAAAAAAAAACAwQBAAX/xAAgEQACAgMAAwADAAAAAAAAAAAAAQIR\n" +
                "AxIhBBMxMmGR/9oADAMBAAIRAxEAPwDOor6ir6RqwhH0hfX9fx++t1FbGmYRUyEg4A6k5Ot9staw\n" +
                "ny4FP8R+RDNkE9s6s1TR2yzW0190QVGOiq/0k/bj21Ko2/0Miv6bKSOKyW1aeAqzjq5B+pvXXKdy\n" +
                "BRyYkYOqVd9xw1crSQWiCKnXIXCDl/nj9tUu80016u8dPPdKyC3ypzMMT4ZmGAUz9hkHJz3xqlTa\n" +
                "4ilRk/oYJd8WunJjlr6NJT2RplB/fWUO7AwBDhhjIIPTVSsXhltF6FXlslLKGHzNLlmb9e+uC8bC\n" +
                "t9muNHJa2qKeJ5eJhErFGABbA69Ppx+M6KUnR3Y/UFa17pilK8I5JSTjIIA/rqJ3TYWeve8UlH5a\n" +
                "VKjzgGGCw7N+cd/wNDykNdBKI5KgD5sjI6aJW3qyueDyJI/MjIwSDlW/00vdPjMyRlVFMqoOMhjZ\n" +
                "WR/5WGD/AIffUVUUoZ8EaIlDQJXVr0VTGfLlbA/8WJ6ah9zbdms1XGkh5JMnJGx9uhB/UHQShy0T\n" +
                "X2iatSxSX96RXTIYRL64Oev761+L7UduTlc3ZII8BEHdjj0GrPZbRTVV5MskKJ5vE5Ax17Hr/wA9\n" +
                "NUv2p57BtHbluul4q55qjzpFo7fM4Z6h1CgovqEGQWbOACO5KqdriDxy1fQSVO8DXF4LfZ3SmQdW\n" +
                "diCfX0H21Xqu+Ri726oWadY3ZgyDDBBhcgEfc4z+NBi7XGqula9VVPlmJIUdFQfZR6D/AIdc8Ukk\n" +
                "MqSxO0ciMGR1OCpHYg+h0aib7h69rCoa2RK7FSVGVHpqq+KNS1NV2aGeOsZ0qTxkhcqEVhxYnH5H\n" +
                "X0xoXeDfjlNZsWnejz1dGSiwV0cYaSEDCkSAYLrj5uXV8g/VkYZyJbRfrRDdqCWiudG2QskTpLFK\n" +
                "uSGAIJBwQR+Rps6cEGpbWAzdFpv07T8I63hEAIwwPXPc4Hr+dTnh8246CzPdUmm8mneNJ6eo+vkx\n" +
                "IIH3HTP40cK+009SvvMYCiTv9gfXX21USUswWWKCcN0yy9QNI1oZJ7dIinSasus7UsL8iiuxxhQD\n" +
                "+v37nXd4g2mtjstFVVlQ0s5qWV1KBRllznH7/jVlsdsaTckwY8YXwf0C46n/AC1xeLknvtdQW2PJ\n" +
                "bLSOq+nLB/Yf10VtRaJH+RYLrZaSyxz1k9XFT0VPG0ss8zBI4kUFmLMegUKCST0AGvNvxs35W+JH\n" +
                "iRdN0VUk3u8r+TQRSEjyaZOka8eTBSR8zBTjm7kd9Nr7fPiDd7LsW0bZs881Ku4pJxWzxS8S1PEq\n" +
                "coCMZw5mXJBHRCpyHI0i2iquAXfSV2rYLnuW8xWq1QiSaTqzMcJEg7u59FGf2AySASJv3wVu1ktE\n" +
                "V0sM816jBVJ6dIP46HAHNVBPJS2eg6qCPqALC5+DO2327sVLpMh9+uwWpIDdocfwh0JByCWz0Pz4\n" +
                "PbRXscVQLYWqj8zDOMems7ZbHxl69m+iOa6fiFf8L+Fe/VPw/wA/3j3XzW8nzePHzOGccuPTljOO\n" +
                "mmO8TPDSy7qc1dseC1Xnk7M6wgRVGcn+IB2bkf8AqDJwTkN0wud5oJrVd622VDxvNR1EkEjRklSy\n" +
                "MVJGQDjI+w0TVE08cofQneylfrlafF2gt9NXSQ2+5RzR11PnMc4SGR05A+oYDBHUZIzhiC5lPV07\n" +
                "SBlmHQ9j/rpV/ZB2tSXw7pu3u6SXS1rS+5yN1KLJ53mADsCQijPfGR2Jywe3qoeeUcYcdMY7aXKT\n" +
                "TLfGxp47YSTc/crcayni8xuisxOPxqFo6ee43ISVEhWpq34tIf8Atqx/c6kaFTLZ5CygoHQnp07j\n" +
                "UxV0kFPNNIsfFoqlXBX8jQyl0kyJKXBS/boqZrpZtk3CKCY00T1sckvA8UZxAUUnsCQjED14t9jp\n" +
                "W9ej1bbrbuKxVtnvlFFWUFbmOaGQfKQT0P3BBAIIwQQCCCAdKn4kezjuayxz3Pacvx+2qSwp8BKy\n" +
                "NfmOOPaXACjK4ZmPRNV5MTXUIj8Iza/jfclaODdlL8QiUn+1UyKk3949U6I390dOOAM/MdT27vaF\n" +
                "5U4ptq2Tjzw0k9xHUd8qqI3/AKnkW+44+ugPV01RR1c1JVwS09RBI0csUqFXjdTgqwPUEEEEHWrS\n" +
                "KH+/JVWXCbxM3nJVvULdhGWYkKtPGVUfYZUnA/Uk6gNxXu5bguJuN2mjnqigRpFgSMsB25cAMnHT\n" +
                "J64AHYDVs234Q75vfkyfDIrbTy8szXCdYfLxn6kyZBkjA+X1B7ddWOP2e94StxhvO25TnrwqJiF/\n" +
                "J8rWnOOWa7ZXtgeMO/djW2ntW3rnSwW2Kfz3pGoICs7Egt5j8PMbIAXPLkFAAIwMNB4d7xsW/bdS\n" +
                "3iyAwVYZYq+hZ8yUrkdc/wAynB4t2IB7EMoTbeG3rjtXctbt+6iL3ujcK5ifmjggMrKfsVIIyAev\n" +
                "UA5GurZ28dwbRW5fAK+Sje40vu0siMQyDkDzTrgSABlDd1DtjBIIySs7HkeN9HFvftPeGFjWp2/D\n" +
                "T326SU8oV6yhghemkYYzwZpVLAHI5YwcZBIIJLuyN5WDxB2jJubbVX59FUModJFCy08gC8opFyeL\n" +
                "rkZGSCCCCVIJ8vdO97EsZtfgZWS148lbjeZZ6Y8gecYSKItgHp88bjBwemexBIuKF3bCZMDTgggg\n" +
                "GZSNStuhLRlyAAGP9P8AfOoKW6Udbeqe38i0kANQwHoFHrq0WpG9yp+fdkBb8nrr1GhexDbk2zaN\n" +
                "x0vul8tlHcaZG8xI6qBZVVwCOYDAjOCRn9Toe1GwNsWyqBpduWihqkBaKogoo43AIwcMoBHQkaNP\n" +
                "lgxYx6ai9xWb4lQfwQBURLyjP3HqupM2NfUPwZNWAi4WmvimKxvLxB6FW1O7XpK1VXzeROe7tqSq\n" +
                "/PilaGWNkkU4ZWHUayo5nV8Fv8MakU2uHr+1uIvHtW+Hl5oNy1G+6fFZaK4RLO0a/NRyKixgOP5W\n" +
                "4jD9snicHiWBGvTnaFtnnmSeZCsQIKgj6v8AbV5jlDS1AXsqBRqqGJyVs8bM0pcEL9mz2e7pvivi\n" +
                "3BvCirLZteMLLDHKjRS3QlQyiPsRCQQTIO4PFDnLI9NBZKKgpaCjtdPDR0YaPhBGgRI1UfKiqOgA\n" +
                "CgADtrKoqPLpKaXPVXUdPtnXTNUBLlTQR4xHlj+gHT/7pjw8oTsf/9k=";
    }
}
