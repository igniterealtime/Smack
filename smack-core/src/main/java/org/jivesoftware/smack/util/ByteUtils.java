/*
 *
 * Copyright © 2014-2019 Florian Schmaus
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
package org.jivesoftware.smack.util;

public class ByteUtils {

    /**
     * Concatenate two byte arrays.
     *
     * @param arrayOne the first input array.
     * @param arrayTwo the second input array
     * @return the concatenation of the first and second input array.
     *
     */
    public static byte[] concat(byte[] arrayOne, byte[] arrayTwo) {
        int combinedLength = arrayOne.length + arrayTwo.length;
        byte[] res = new byte[combinedLength];
        System.arraycopy(arrayOne, 0, res, 0, arrayOne.length);
        System.arraycopy(arrayTwo, 0, res, arrayOne.length, arrayTwo.length);
        return res;
    }

    /**
     * Concatenate multiple byte arrays.
     *
     * @param arrays the input arrays.
     * @return the concatenation of all input arrays.
     */
    public static byte[] concat(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] array : arrays) {
            if (array != null) {
                totalLength += array.length;
            }
        }
        byte[] res = new byte[totalLength];
        int offset = 0;
        for (byte[] array : arrays) {
            if (array != null) {
                System.arraycopy(array, 0, res, offset, array.length);
                offset += array.length;
            }
        }
        return res;
    }

}
