/**
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

package org.jivesoftware.smack.util;


/**
 * @author Florian Schmaus
 */
public class Base64Encoder implements StringEncoder {

    private static Base64Encoder instance;

    private Base64Encoder() {
        // Use getInstance()
    }

    public static Base64Encoder getInstance() {
        if (instance == null) {
            instance = new Base64Encoder();
        }
        return instance;
    }

    public String encode(String s) {
        return Base64.encodeBytes(s.getBytes());
    }

    public String decode(String s) {
        return new String(Base64.decode(s));
    }

}
