/**
 *
 * Copyright 2016 Fernando Ramirez
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
package org.jivesoftware.smack.tbr;

/**
 * Token-based reconnection token model class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://www.xmpp.org/extensions/inbox/token-reconnection.html">
 *      XEP-xxxx: Token-based reconnection</a>
 */
public class TBRTokens {

    private String accessToken;
    private String refreshToken;

    public TBRTokens(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    /**
     * Get the access token.
     * 
     * @return the access token
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Get the refresh token.
     * 
     * @return the refresh token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

}
