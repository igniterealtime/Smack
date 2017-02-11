/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.vcardtemp.packet;

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
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jxmpp.jid.EntityBareJid;

/**
 * A VCard class for use with the
 * <a href="http://www.jivesoftware.org/smack/" target="_blank">SMACK jabber library</a>.<p>
 * <p/>
 * You should refer to the
 * <a href="http://www.xmpp.org/extensions/jep-0054.html" target="_blank">XEP-54 documentation</a>.<p>
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
    public static final String ELEMENT = "vCard";
    public static final String NAMESPACE = "vcard-temp";

    private static final Logger LOGGER = Logger.getLogger(VCard.class.getName());

    private static final String DEFAULT_MIME_TYPE = "image/jpeg";

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
    private String prefix;
    private String suffix;

    private String emailHome;
    private String emailWork;

    private String organization;
    private String organizationUnit;

    private String photoMimeType;
    private String photoBinval;

    /**
     * Such as DESC ROLE GEO etc.. see XEP-0054
     */
    private Map<String, String> otherSimpleFields = new HashMap<String, String>();

    // fields that, as they are should not be escaped before forwarding to the server
    private Map<String, String> otherUnescapableFields = new HashMap<String, String>();

    public VCard() {
        super(ELEMENT, NAMESPACE);
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

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        updateFN();
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
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
     * Get home address field.
     *
     * @param addrField one of POSTAL, PARCEL, (DOM | INTL), PREF, POBOX, EXTADR, STREET,
     *                  LOCALITY, REGION, PCODE, CTRY
     */
    public String getAddressFieldHome(String addrField) {
        return homeAddr.get(addrField);
    }

    /**
     * Set home address field.
     *
     * @param addrField one of POSTAL, PARCEL, (DOM | INTL), PREF, POBOX, EXTADR, STREET,
     *                  LOCALITY, REGION, PCODE, CTRY
     */
    public void setAddressFieldHome(String addrField, String value) {
        homeAddr.put(addrField, value);
    }

    /**
     * Get work address field.
     *
     * @param addrField one of POSTAL, PARCEL, (DOM | INTL), PREF, POBOX, EXTADR, STREET,
     *                  LOCALITY, REGION, PCODE, CTRY
     */
    public String getAddressFieldWork(String addrField) {
        return workAddr.get(addrField);
    }

    /**
     * Set work address field.
     *
     * @param addrField one of POSTAL, PARCEL, (DOM | INTL), PREF, POBOX, EXTADR, STREET,
     *                  LOCALITY, REGION, PCODE, CTRY
     */
    public void setAddressFieldWork(String addrField, String value) {
        workAddr.put(addrField, value);
    }


    /**
     * Set home phone number.
     *
     * @param phoneType one of VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF
     * @param phoneNum  phone number
     */
    public void setPhoneHome(String phoneType, String phoneNum) {
        homePhones.put(phoneType, phoneNum);
    }

    /**
     * Get home phone number.
     *
     * @param phoneType one of VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF
     */
    public String getPhoneHome(String phoneType) {
        return homePhones.get(phoneType);
    }

    /**
     * Set work phone number.
     *
     * @param phoneType one of VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF
     * @param phoneNum  phone number
     */
    public void setPhoneWork(String phoneType, String phoneNum) {
        workPhones.put(phoneType, phoneNum);
    }

    /**
     * Get work phone number.
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
            LOGGER.log(Level.SEVERE, "Error getting bytes from URL: " + avatarURL, e);
        }

        setAvatar(bytes);
    }

    /**
     * Removes the avatar from the vCard.
     *
     *  This is done by setting the PHOTO value to the empty string as defined in XEP-0153
     */
    public void removeAvatar() {
        // Remove avatar (if any)
        photoBinval = null;
        photoMimeType = null;
    }

    /**
     * Specify the bytes of the JPEG for the avatar to use.
     * If bytes is null, then the avatar will be removed.
     * 'image/jpeg' will be used as MIME type.
     *
     * @param bytes the bytes of the avatar, or null to remove the avatar data
     */
    public void setAvatar(byte[] bytes) {
        setAvatar(bytes, DEFAULT_MIME_TYPE);
    }

    /**
     * Specify the bytes for the avatar to use as well as the mime type.
     *
     * @param bytes the bytes of the avatar.
     * @param mimeType the mime type of the avatar.
     */
    public void setAvatar(byte[] bytes, String mimeType) {
        // If bytes is null, remove the avatar
        if (bytes == null) {
            removeAvatar();
            return;
        }

        // Otherwise, add to mappings.
        String encodedImage = Base64.encodeToString(bytes);

        setAvatar(encodedImage, mimeType);
    }

    /**
     * Specify the Avatar used for this vCard.
     *
     * @param encodedImage the Base64 encoded image as String
     * @param mimeType the MIME type of the image
     */
    public void setAvatar(String encodedImage, String mimeType) {
        photoBinval = encodedImage;
        photoMimeType = mimeType;
    }

    /**
     * Set the encoded avatar string. This is used by the provider.
     *
     * @param encodedAvatar the encoded avatar string.
     * @deprecated Use {@link #setAvatar(String, String)} instead.
     */
    @Deprecated
    public void setEncodedImage(String encodedAvatar) {
        setAvatar(encodedAvatar, DEFAULT_MIME_TYPE);
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
        if (photoBinval == null) {
            return null;
        }
        return Base64.decode(photoBinval);
    }

    /**
     * Returns the MIME Type of the avatar or null if none is set.
     *
     * @return the MIME Type of the avatar or null
     */
    public String getAvatarMimeType() {
        return photoMimeType;
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
            LOGGER.log(Level.SEVERE, "Failed to get message digest", e);
            return null;
        }

        digest.update(bytes);
        return StringUtils.encodeHex(digest.digest());
    }

    private void updateFN() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null) {
            sb.append(StringUtils.escapeForXml(firstName)).append(' ');
        }
        if (middleName != null) {
            sb.append(StringUtils.escapeForXml(middleName)).append(' ');
        }
        if (lastName != null) {
            sb.append(StringUtils.escapeForXml(lastName));
        }
        setField("FN", sb.toString());
    }

    /**
     * Save this vCard for the user connected by 'connection'. XMPPConnection should be authenticated
     * and not anonymous.
     *
     * @param connection the XMPPConnection to use.
     * @throws XMPPErrorException thrown if there was an issue setting the VCard in the server.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     * @deprecated use {@link VCardManager#saveVCard(VCard)} instead.
     */
    @Deprecated
    public void save(XMPPConnection connection) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        VCardManager.getInstanceFor(connection).saveVCard(this);
    }

    /**
     * Load VCard information for a connected user. XMPPConnection should be authenticated
     * and not anonymous.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     * @deprecated use {@link VCardManager#loadVCard()} instead.
     */
    @Deprecated
    public void load(XMPPConnection connection) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        load(connection, null);
    }

    /**
     * Load VCard information for a given user. XMPPConnection should be authenticated and not anonymous.
     * @throws XMPPErrorException 
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     * @deprecated use {@link VCardManager#loadVCard(EntityBareJid)} instead.
     */
    @Deprecated
    public void load(XMPPConnection connection, EntityBareJid user) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        VCard result = VCardManager.getInstanceFor(connection).loadVCard(user);
        copyFieldsFrom(result);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        if (!hasContent()) {
            xml.setEmptyElement();
            return xml;
        }
        xml.rightAngleBracket();
        if (hasNameField()) {
            xml.openElement("N");
            xml.optElement("FAMILY", lastName);
            xml.optElement("GIVEN", firstName);
            xml.optElement("MIDDLE", middleName);
            xml.optElement("PREFIX", prefix);
            xml.optElement("SUFFIX", suffix);
            xml.closeElement("N");
        }
        if (hasOrganizationFields()) {
            xml.openElement("ORG");
            xml.optElement("ORGNAME", organization);
            xml.optElement("ORGUNIT", organizationUnit);
            xml.closeElement("ORG");
        }
        for (Entry<String, String> entry : otherSimpleFields.entrySet()) {
            xml.optElement(entry.getKey(), entry.getValue());
        }
        for (Entry<String, String> entry : otherUnescapableFields.entrySet()) {
            final String value = entry.getValue();
            if (value == null) {
                continue;
            }
            xml.openElement(entry.getKey());
            xml.append(value);
            xml.closeElement(entry.getKey());
        }
        if (photoBinval != null) {
            xml.openElement("PHOTO");
            xml.escapedElement("BINVAL", photoBinval);
            xml.element("TYPE", photoMimeType);
            xml.closeElement("PHOTO");
        }
        if (emailWork != null) {
            xml.openElement("EMAIL");
            xml.emptyElement("WORK");
            xml.emptyElement("INTERNET");
            xml.emptyElement("PREF");
            xml.element("USERID", emailWork);
            xml.closeElement("EMAIL");
        }
        if (emailHome != null) {
            xml.openElement("EMAIL");
            xml.emptyElement("HOME");
            xml.emptyElement("INTERNET");
            xml.emptyElement("PREF");
            xml.element("USERID", emailHome);
            xml.closeElement("EMAIL");
        }
        for (Entry<String, String> phone : workPhones.entrySet()) {
            final String number = phone.getValue();
            if (number == null) {
                continue;
            }
            xml.openElement("TEL");
            xml.emptyElement("WORK");
            xml.emptyElement(phone.getKey());
            xml.element("NUMBER", number);
            xml.closeElement("TEL");
        }
        for (Entry<String, String> phone : homePhones.entrySet()) {
            final String number = phone.getValue();
            if (number == null) {
                continue;
            }
            xml.openElement("TEL");
            xml.emptyElement("HOME");
            xml.emptyElement(phone.getKey());
            xml.element("NUMBER", number);
            xml.closeElement("TEL");
        }
        if (!workAddr.isEmpty()) {
            xml.openElement("ADR");
            xml.emptyElement("WORK");
            for (Entry<String, String> entry : workAddr.entrySet()) {
                final String value = entry.getValue();
                if (value == null) {
                    continue;
                }
                xml.element(entry.getKey(), value);
            }
            xml.closeElement("ADR");
        }
        if (!homeAddr.isEmpty()) {
            xml.openElement("ADR");
            xml.emptyElement("HOME");
            for (Entry<String, String> entry : homeAddr.entrySet()) {
                final String value = entry.getValue();
                if (value == null) {
                    continue;
                }
                xml.element(entry.getKey(), value);
            }
            xml.closeElement("ADR");
        }
        return xml;
    }

    private void copyFieldsFrom(VCard from) {
        Field[] fields = VCard.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getDeclaringClass() == VCard.class &&
                    !Modifier.isFinal(field.getModifiers())) {
                try {
                    field.setAccessible(true);
                    field.set(this, field.get(from));
                }
                catch (IllegalAccessException e) {
                    throw new RuntimeException("This cannot happen:" + field, e);
                }
            }
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
                || photoBinval != null
                ;
    }

    private boolean hasNameField() {
        return firstName != null || lastName != null || middleName != null
                || prefix != null || suffix != null;
    }

    private boolean hasOrganizationFields() {
        return organization != null || organizationUnit != null;
    }

    // Used in tests:

    @Override
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
        if (photoBinval != null ? !photoBinval.equals(vCard.photoBinval) : vCard.photoBinval != null) {
            return false;
        }

        return workPhones.equals(vCard.workPhones);
    }

    @Override
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
        result = 29 * result + (photoBinval != null ? photoBinval.hashCode() : 0);
        return result;
    }

}

