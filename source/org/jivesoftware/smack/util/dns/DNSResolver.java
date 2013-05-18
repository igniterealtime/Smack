/**
 * Copyright 2013 Florian Schmaus
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
package org.jivesoftware.smack.util.dns;

import java.util.List;

/**
 * Implementations of this interface define a class that is capable of resolving DNS addresses.
 *
 */
public interface DNSResolver {

    /**
     * Gets a list of service records for the specified service.
     * @param name The symbolic name of the service.
     * @return The list of SRV records mapped to the service name.
     */
    List<SRVRecord> lookupSRVRecords(String name);

}
