/**
 *
 * Copyright 2019 Florian Schmaus
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

public class BooleansUtils {

    public static boolean contains(boolean[] array, boolean target) {
        for (boolean b : array) {
            if (b == target) {
                return true;
            }
        }
        return false;
    }

    public static int numberOf(boolean[] array, boolean target) {
        int res = 0;
        for (boolean b : array) {
            if (b == target) {
                res++;
            }
        }
        return res;
    }
}
