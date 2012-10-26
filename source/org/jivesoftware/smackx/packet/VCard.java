/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.packet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;

/**
 * A VCard class for use with the
 * <a href="http://www.jivesoftware.org/smack/" target="_blank">SMACK jabber library</a>.<p>
 * <p/>
 * You should refer to the
 * <a href="http://www.jabber.org/jeps/jep-0054.html" target="_blank">JEP-54 documentation</a>.<p>
 * <p/>
 * Please note that this class is incomplete but it does provide the most commonly found
 * information in vCards. Also remember that VCard transfer is not a standard, and the protocol
 * may change or be replaced.<p>
 * <p/>
 * <b>Usage:</b>
 * <pre>
 * <p/>
 * // To save VCard:
 * <p/>
 * VCard vCard = new VCard();
 * vCard.setFirstName("kir");
 * vCard.setLastName("max");
 * vCard.setEmailHome("foo@fee.bar");
 * vCard.setJabberId("jabber@id.org");
 * vCard.setOrganization("Jetbrains, s.r.o");
 * vCard.setNickName("KIR");
 * <p/>
 * vCard.setField("TITLE", "Mr");
 * vCard.setAddressFieldHome("STREET", "Some street");
 * vCard.setAddressFieldWork("CTRY", "US");
 * vCard.setPhoneWork("FAX", "3443233");
 * <p/>
 * vCard.save(connection);
 * <p/>
 * // To load VCard:
 * <p/>
 * VCard vCard = new VCard();
 * vCard.load(conn); // load own VCard
 * vCard.load(conn, "joe@foo.bar"); // load someone's VCard
 * </pre>
 *
 * @author Kirill Maximov (kir@maxkir.com)
 */
public class VCard extends IQ {

    /**
     * Phone types:
     * VOICE?, FAX?, PAGER?, MSG?, CELL?, VIDEO?, BBS?, MODEM?, ISDN?, PCS?, PREF?
     */
    private Map<String, String> homePhones = new HashMap<String, String>();
    private Map<String, String> workPhones = new HashMap<String, String>();


    /**
     * Address types:
     * POSTAL?, PARCEL?, (DOM | INTL)?, PREF?, POBOX?, EXTADR?, STREET?, LOCALITY?,
     * REGION?, PCODE?, CTRY?
     */
    private Map<String, String> homeAddr = new HashMap<String, String>();
    private Map<String, String> workAddr = new HashMap<String, String>();

    private String firstName;
    private String lastName;
    private String middleName;

    private String emailHome;
    private String emailWork;

    private String organization;
    private String organizationUnit;

    private String avatar;

    /**
     * Such as DESC ROLE GEO etc.. see JEP-0054
     */
    private Map<String, String> otherSimpleFields = new HashMap<String, String>();

    // fields that, as they are should not be escaped before forwarding to the server
    private Map<String, String> otherUnescapableFields = new HashMap<String, String>();

    public VCard() {
    }

    /**
     * Set generic VCard field.
     *
     * @param field value of field. Possible values: NICKNAME, PHOTO, BDAY, JABBERID, MAILER, TZ,
     *              GEO, TITLE, ROLE, LOGO, NOTE, PRODID, REV, SORT-STRING, SOUND, UID, URL, DESC.
     */
    public String getField(String field) {
        return otherSimpleFields.get(field);
    }

    /**
     * Set generic VCard field.
     *
     * @param value value of field
     * @param field field to set. See {@link #getField(String)}
     * @see #getField(String)
     */
    public void setField(String field, String value) {
        setField(field, value, false);
    }

    /**
     * Set generic, unescapable VCard field. If unescabale is set to true, XML maybe a part of the
     * value.
     *
     * @param value         value of field
     * @param field         field to set. See {@link #getField(String)}
     * @param isUnescapable True if the value should not be escaped, and false if it should.
     */
    public void setField(String field, String value, boolean isUnescapable) {
        if (!isUnescapable) {
            otherSimpleFields.put(field, value);
        }
        else {
            otherUnescapableFields.put(field, value);
        }
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        // Update FN field
        updateFN();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        // Update FN field
        updateFN();
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
        // Update FN field
        updateFN();
    }

    public String getNickName() {
        return otherSimpleFields.get("NICKNAME");
    }

    public void setNickName(String nickName) {
        otherSimpleFields.put("NICKNAME", nickName);
    }

    public String getEmailHome() {
        return emailHome;
    }

    public void setEmailHome(String email) {
        this.emailHome = email;
    }

    public String getEmailWork() {
        return emailWork;
    }

    public void setEmailWork(String emailWork) {
        this.emailWork = emailWork;
    }

    public String getJabberId() {
        return otherSimpleFields.get("JABBERID");
    }

    public void setJabberId(String jabberId) {
        otherSimpleFields.put("JABBERID", jabberId);
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getOrganizationUnit() {
        return organizationUnit;
    }

    public void setOrganizationUnit(String organizationUnit) {
        this.organizationUnit = organizationUnit;
    }

    /**
     * Get home address field
     *
     * @param addrField one of POSTAL, PARCEL, (DOM | INTL), PREF, POBOX, EXTADR, STREET,
     *                  LOCALITY, REGION, PCODE, CTRY
     */
    public String getAddressFieldHome(String addrField) {
        return homeAddr.get(addrField);
    }

    /**
     * Set home address field
     *
     * @param addrField one of POSTAL, PARCEL, (DOM | INTL), PREF, POBOX, EXTADR, STREET,
     *                  LOCALITY, REGION, PCODE, CTRY
     */
    public void setAddressFieldHome(String addrField, String value) {
        homeAddr.put(addrField, value);
    }

    /**
     * Get work address field
     *
     * @param addrField one of POSTAL, PARCEL, (DOM | INTL), PREF, POBOX, EXTADR, STREET,
     *                  LOCALITY, REGION, PCODE, CTRY
     */
    public String getAddressFieldWork(String addrField) {
        return workAddr.get(addrField);
    }

    /**
     * Set work address field
     *
     * @param addrField one of POSTAL, PARCEL, (DOM | INTL), PREF, POBOX, EXTADR, STREET,
     *                  LOCALITY, REGION, PCODE, CTRY
     */
    public void setAddressFieldWork(String addrField, String value) {
        workAddr.put(addrField, value);
    }


    /**
     * Set home phone number
     *
     * @param phoneType one of VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF
     * @param phoneNum  phone number
     */
    public void setPhoneHome(String phoneType, String phoneNum) {
        homePhones.put(phoneType, phoneNum);
    }

    /**
     * Get home phone number
     *
     * @param phoneType one of VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF
     */
    public String getPhoneHome(String phoneType) {
        return homePhones.get(phoneType);
    }

    /**
     * Set work phone number
     *
     * @param phoneType one of VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF
     * @param phoneNum  phone number
     */
    public void setPhoneWork(String phoneType, String phoneNum) {
        workPhones.put(phoneType, phoneNum);
    }

    /**
     * Get work phone number
     *
     * @param phoneType one of VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF
     */
    public String getPhoneWork(String phoneType) {
        return workPhones.get(phoneType);
    }

    /**
     * Set the avatar for the VCard by specifying the url to the image.
     *
     * @param avatarURL the url to the image(png,jpeg,gif,bmp)
     */
    public void setAvatar(URL avatarURL) {
        byte[] bytes = new byte[0];
        try {
            bytes = getBytes(avatarURL);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        setAvatar(bytes);
    }

    /**
     * Specify the bytes for the avatar to use.
     *
     * @param bytes the bytes of the avatar.
     */
    public void setAvatar(byte[] bytes) {
        if (bytes == null) {
            // Remove avatar (if any) from mappings
            otherUnescapableFields.remove("PHOTO");
            return;
        }

        // Otherwise, add to mappings.
        String encodedImage = StringUtils.encodeBase64(bytes);
        avatar = encodedImage;

        setField("PHOTO", "<TYPE>image/jpeg</TYPE><BINVAL>" + encodedImage + "</BINVAL>", true);
    }

    /**
     * Specify the bytes for the avatar to use as well as the mime type.
     *
     * @param bytes the bytes of the avatar.
     * @param mimeType the mime type of the avatar.
     */
    public void setAvatar(byte[] bytes, String mimeType) {
        if (bytes == null) {
            // Remove avatar (if any) from mappings
            otherUnescapableFields.remove("PHOTO");
            return;
        }

        // Otherwise, add to mappings.
        String encodedImage = StringUtils.encodeBase64(bytes);
        avatar = encodedImage;

        setField("PHOTO", "<TYPE>" + mimeType + "</TYPE><BINVAL>" + encodedImage + "</BINVAL>", true);
    }

    /**
     * Set the encoded avatar string. This is used by the provider.
     *
     * @param encodedAvatar the encoded avatar string.
     */
    public void setEncodedImage(String encodedAvatar) {
        //TODO Move VCard and VCardProvider into a vCard package.
        this.avatar = encodedAvatar;
    }

    /**
     * Return the byte representation of the avatar(if one exists), otherwise returns null if
     * no avatar could be found.
     * <b>Example 1</b>
     * <pre>
     * // Load Avatar from VCard
     * byte[] avatarBytes = vCard.getAvatar();
     * <p/>
     * // To create an ImageIcon for Swing applications
     * ImageIcon icon = new ImageIcon(avatar);
     * <p/>
     * // To create just an image object from the bytes
     * ByteArrayInputStream bais = new ByteArrayInputStream(avatar);
     * try {
     *   Image image = ImageIO.read(bais);
     *  }
     *  catch (IOException e) {
     *    e.printStackTrace();
     * }
     * </pre>
     *
     * @return byte representation of avatar.
     */
    public byte[] getAvatar() {
        if (avatar == null) {
            return null;
        }
        return StringUtils.decodeBase64(avatar);
    }

    /**
     * Common code for getting the bytes of a url.
     *
     * @param url the url to read.
     */
    public static byte[] getBytes(URL url) throws IOException {
        final String path = url.getPath();
        final File file = new File(path);
        if (file.exists()) {
            return getFileBytes(file);
        }

        return null;
    }

    private static byte[] getFileBytes(File file) throws IOException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            int bytes = (int) file.length();
            byte[] buffer = new byte[bytes];
            int readBytes = bis.read(buffer);
            if (readBytes != buffer.length) {
                throw new IOException("Entire file not read");
            }
            return buffer;
        }
        finally {
            if (bis != null) {
                bis.close();
            }
        }
    }

    /**
     * Returns the SHA-1 Hash of the Avatar image.
     *
     * @return the SHA-1 Hash of the Avatar image.
     */
    public String getAvatarHash() {
        byte[] bytes = getAvatar();
        if (bytes == null) {
            return null;
        }

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        digest.update(bytes);
        return StringUtils.encodeHex(digest.digest());
    }

    private void updateFN() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null) {
            sb.append(StringUtils.escapeForXML(firstName)).append(' ');
        }
        if (middleName != null) {
            sb.append(StringUtils.escapeForXML(middleName)).append(' ');
        }
        if (lastName != null) {
            sb.append(StringUtils.escapeForXML(lastName));
        }
        setField("FN", sb.toString());
    }

    /**
     * Save this vCard for the user connected by 'connection'. Connection should be authenticated
     * and not anonymous.<p>
     * <p/>
     * NOTE: the method is asynchronous and does not wait for the returned value.
     *
     * @param connection the Connection to use.
     * @throws XMPPException thrown if there was an issue setting the VCard in the server.
     */
    public void save(Connection connection) throws XMPPException {
        checkAuthenticated(connection, true);

        setType(IQ.Type.SET);
        setFrom(connection.getUser());
        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(getPacketID()));
        connection.sendPacket(this);

        Packet response = collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server on status set.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
    }

    /**
     * Load VCard information for a connected user. Connection should be authenticated
     * and not anonymous.
     */
    public void load(Connection connection) throws XMPPException {
        checkAuthenticated(connection, true);

        setFrom(connection.getUser());
        doLoad(connection, connection.getUser());
    }

    /**
     * Load VCard information for a given user. Connection should be authenticated and not anonymous.
     */
    public void load(Connection connection, String user) throws XMPPException {
        checkAuthenticated(connection, false);

        setTo(user);
        doLoad(connection, user);
    }

    private void doLoad(Connection connection, String user) throws XMPPException {
        setType(Type.GET);
        PacketCollector collector = connection.createPacketCollector(
                new PacketIDFilter(getPacketID()));
        connection.sendPacket(this);

        VCard result = null;
        try {
            result = (VCard) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

            if (result == null) {
                String errorMessage = "Timeout getting VCard information";
                throw new XMPPException(errorMessage, new XMPPError(
                        XMPPError.Condition.request_timeout, errorMessage));
            }
            if (result.getError() != null) {
                throw new XMPPException(result.getError());
            }
        }
        catch (ClassCastException e) {
            System.out.println("No VCard for " + user);
        }

        copyFieldsFrom(result);
    }

    public String getChildElementXML() {
        StringBuilder sb = new StringBuilder();
        new VCardWriter(sb).write();
        return sb.toString();
    }

    private void copyFieldsFrom(VCard result) {
        if (result == null) result = new VCard();

        Field[] fields = VCard.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getDeclaringClass() == VCard.class &&
                    !Modifier.isFinal(field.getModifiers())) {
                try {
                    field.setAccessible(true);
                    field.set(this, field.get(result));
                }
                catch (IllegalAccessException e) {
                    throw new RuntimeException("This cannot happen:" + field, e);
                }
            }
        }
    }

    private void checkAuthenticated(Connection connection, boolean checkForAnonymous) {
        if (connection == null) {
            throw new IllegalArgumentException("No connection was provided");
        }
        if (!connection.isAuthenticated()) {
            throw new IllegalArgumentException("Connection is not authenticated");
        }
        if (checkForAnonymous && connection.isAnonymous()) {
            throw new IllegalArgumentException("Connection cannot be anonymous");
        }
    }

    private boolean hasContent() {
        //noinspection OverlyComplexBooleanExpression
        return hasNameField()
                || hasOrganizationFields()
                || emailHome != null
                || emailWork != null
                || otherSimpleFields.size() > 0
                || otherUnescapableFields.size() > 0
                || homeAddr.size() > 0
                || homePhones.size() > 0
                || workAddr.size() > 0
                || workPhones.size() > 0
                ;
    }

    private boolean hasNameField() {
        return firstName != null || lastName != null || middleName != null;
    }

    private boolean hasOrganizationFields() {
        return organization != null || organizationUnit != null;
    }

    // Used in tests:

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final VCard vCard = (VCard) o;

        if (emailHome != null ? !emailHome.equals(vCard.emailHome) : vCard.emailHome != null) {
            return false;
        }
        if (emailWork != null ? !emailWork.equals(vCard.emailWork) : vCard.emailWork != null) {
            return false;
        }
        if (firstName != null ? !firstName.equals(vCard.firstName) : vCard.firstName != null) {
            return false;
        }
        if (!homeAddr.equals(vCard.homeAddr)) {
            return false;
        }
        if (!homePhones.equals(vCard.homePhones)) {
            return false;
        }
        if (lastName != null ? !lastName.equals(vCard.lastName) : vCard.lastName != null) {
            return false;
        }
        if (middleName != null ? !middleName.equals(vCard.middleName) : vCard.middleName != null) {
            return false;
        }
        if (organization != null ?
                !organization.equals(vCard.organization) : vCard.organization != null) {
            return false;
        }
        if (organizationUnit != null ?
                !organizationUnit.equals(vCard.organizationUnit) : vCard.organizationUnit != null) {
            return false;
        }
        if (!otherSimpleFields.equals(vCard.otherSimpleFields)) {
            return false;
        }
        if (!workAddr.equals(vCard.workAddr)) {
            return false;
        }
        return workPhones.equals(vCard.workPhones);

    }

    public int hashCode() {
        int result;
        result = homePhones.hashCode();
        result = 29 * result + workPhones.hashCode();
        result = 29 * result + homeAddr.hashCode();
        result = 29 * result + workAddr.hashCode();
        result = 29 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 29 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 29 * result + (middleName != null ? middleName.hashCode() : 0);
        result = 29 * result + (emailHome != null ? emailHome.hashCode() : 0);
        result = 29 * result + (emailWork != null ? emailWork.hashCode() : 0);
        result = 29 * result + (organization != null ? organization.hashCode() : 0);
        result = 29 * result + (organizationUnit != null ? organizationUnit.hashCode() : 0);
        result = 29 * result + otherSimpleFields.hashCode();
        return result;
    }

    public String toString() {
        return getChildElementXML();
    }

    //==============================================================

    private class VCardWriter {

        private final StringBuilder sb;

        VCardWriter(StringBuilder sb) {
            this.sb = sb;
        }

        public void write() {
            appendTag("vCard", "xmlns", "vcard-temp", hasContent(), new ContentBuilder() {
                public void addTagContent() {
                    buildActualContent();
                }
            });
        }

        private void buildActualContent() {
            if (hasNameField()) {
                appendN();
            }

            appendOrganization();
            appendGenericFields();

            appendEmail(emailWork, "WORK");
            appendEmail(emailHome, "HOME");

            appendPhones(workPhones, "WORK");
            appendPhones(homePhones, "HOME");

            appendAddress(workAddr, "WORK");
            appendAddress(homeAddr, "HOME");
        }

        private void appendEmail(final String email, final String type) {
            if (email != null) {
                appendTag("EMAIL", true, new ContentBuilder() {
                    public void addTagContent() {
                        appendEmptyTag(type);
                        appendEmptyTag("INTERNET");
                        appendEmptyTag("PREF");
                        appendTag("USERID", StringUtils.escapeForXML(email));
                    }
                });
            }
        }

        private void appendPhones(Map<String, String> phones, final String code) {
            Iterator<Map.Entry<String, String>> it = phones.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<String,String> entry = it.next();
                appendTag("TEL", true, new ContentBuilder() {
                    public void addTagContent() {
                        appendEmptyTag(entry.getKey());
                        appendEmptyTag(code);
                        appendTag("NUMBER", StringUtils.escapeForXML(entry.getValue()));
                    }
                });
            }
        }

        private void appendAddress(final Map<String, String> addr, final String code) {
            if (addr.size() > 0) {
                appendTag("ADR", true, new ContentBuilder() {
                    public void addTagContent() {
                        appendEmptyTag(code);

                        Iterator<Map.Entry<String, String>> it = addr.entrySet().iterator();
                        while (it.hasNext()) {
                            final Entry<String, String> entry = it.next();
                            appendTag(entry.getKey(), StringUtils.escapeForXML(entry.getValue()));
                        }
                    }
                });
            }
        }

        private void appendEmptyTag(Object tag) {
            sb.append('<').append(tag).append("/>");
        }

        private void appendGenericFields() {
            Iterator<Map.Entry<String, String>> it = otherSimpleFields.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                appendTag(entry.getKey().toString(),
                        StringUtils.escapeForXML(entry.getValue()));
            }

            it = otherUnescapableFields.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                appendTag(entry.getKey().toString(),entry.getValue());
            }
        }

        private void appendOrganization() {
            if (hasOrganizationFields()) {
                appendTag("ORG", true, new ContentBuilder() {
                    public void addTagContent() {
                        appendTag("ORGNAME", StringUtils.escapeForXML(organization));
                        appendTag("ORGUNIT", StringUtils.escapeForXML(organizationUnit));
                    }
                });
            }
        }

        private void appendN() {
            appendTag("N", true, new ContentBuilder() {
                public void addTagContent() {
                    appendTag("FAMILY", StringUtils.escapeForXML(lastName));
                    appendTag("GIVEN", StringUtils.escapeForXML(firstName));
                    appendTag("MIDDLE", StringUtils.escapeForXML(middleName));
                }
            });
        }

        private void appendTag(String tag, String attr, String attrValue, boolean hasContent,
                ContentBuilder builder) {
            sb.append('<').append(tag);
            if (attr != null) {
                sb.append(' ').append(attr).append('=').append('\'').append(attrValue).append('\'');
            }

            if (hasContent) {
                sb.append('>');
                builder.addTagContent();
                sb.append("</").append(tag).append(">\n");
            }
            else {
                sb.append("/>\n");
            }
        }

        private void appendTag(String tag, boolean hasContent, ContentBuilder builder) {
            appendTag(tag, null, null, hasContent, builder);
        }

        private void appendTag(String tag, final String tagText) {
            if (tagText == null) return;
            final ContentBuilder contentBuilder = new ContentBuilder() {
                public void addTagContent() {
                    sb.append(tagText.trim());
                }
            };
            appendTag(tag, true, contentBuilder);
        }

    }

    //==============================================================

    private interface ContentBuilder {

        void addTagContent();
    }

    //==============================================================
}

