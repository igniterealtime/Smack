/**
 *
 * Copyright © 2019 Paul Schaub
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
package org.jivesoftware.smackx.omemo_media_sharing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import org.jivesoftware.smack.util.StringUtils;

import org.junit.jupiter.api.Test;


public class OmemoMediaSharingUtilsTest {

    private static final String iv_12 = "8c3d050e9386ec173861778f";
    private static final String iv_16 = "1ad857dcbb119e2642e4f8f7c137819e";
    private static final String key = "4f15af8f1a28100d0101fb1c2e119b0c18c34396c68ad379f5912ee21dca6b0b";
    private static final String key_iv_12 = iv_12 + key;
    private static final String key_iv_16 = iv_16 + key;

    private static final String file = "download.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/tr%C3%A8s%20cool.jpg";
    private static final String file_https = "https://" + file;
    private static final String file_aesgcm_12 = "aesgcm://" + file + "#" + key_iv_12;
    private static final String file_aesgcm_16 = "aesgcm://" + file + "#" + key_iv_16;

    @Test
    public void test12byteIvVariant() throws MalformedURLException {
        AesgcmUrl aesgcm = new AesgcmUrl(file_aesgcm_12);

        // Make sure, that parsed aesgcm url still equals input string
        assertEquals(file_aesgcm_12, aesgcm.getAesgcmUrl());
        assertEquals(file_https, aesgcm.getDownloadUrl().toString());

        URL url = new URL(file_https);
        aesgcm = new AesgcmUrl(url, StringUtils.hexStringToByteArray(key),
                StringUtils.hexStringToByteArray(iv_12));
        assertEquals(file_aesgcm_12, aesgcm.getAesgcmUrl());
        assertEquals(file_https, aesgcm.getDownloadUrl().toString());
    }

    @Test
    public void test16byteIvVariant() throws MalformedURLException {
        AesgcmUrl aesgcm = new AesgcmUrl(file_aesgcm_16);

        // Make sure, that parsed aesgcm url still equals input string
        assertEquals(file_aesgcm_16, aesgcm.getAesgcmUrl());
        assertEquals(file_https, aesgcm.getDownloadUrl().toString());

        URL url = new URL(file_https);
        aesgcm = new AesgcmUrl(url, StringUtils.hexStringToByteArray(key),
                StringUtils.hexStringToByteArray(iv_16));
        assertEquals(file_aesgcm_16, aesgcm.getAesgcmUrl());
        assertEquals(file_https, aesgcm.getDownloadUrl().toString());
    }
}
