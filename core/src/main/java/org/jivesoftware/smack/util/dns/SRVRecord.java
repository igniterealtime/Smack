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

/**
 * @see <a href="http://tools.ietf.org/html/rfc2782>RFC 2782: A DNS RR for specifying the location of services (DNS
 * SRV)<a>
 * @author Florian Schmaus
 * 
 */
public class SRVRecord extends HostAddress implements Comparable<SRVRecord> {
    
    private int weight;
    private int priority;
    
    /**
     * Create a new SRVRecord
     * 
     * @param fqdn Fully qualified domain name
     * @param port The connection port
     * @param priority Priority of the target host
     * @param weight Relative weight for records with same priority
     * @throws IllegalArgumentException fqdn is null or any other field is not in valid range (0-65535).
     */
    public SRVRecord(String fqdn, int port, int priority, int weight) {
        super(fqdn, port);
        if (weight < 0 || weight > 65535)
            throw new IllegalArgumentException(
                    "DNS SRV records weight must be a 16-bit unsiged integer (i.e. between 0-65535. Weight was: "
                            + weight);

        if (priority < 0 || priority > 65535)
            throw new IllegalArgumentException(
                    "DNS SRV records priority must be a 16-bit unsiged integer (i.e. between 0-65535. Priority was: "
                            + priority);

        this.priority = priority;
        this.weight = weight;

    }
    
    public int getPriority() {
        return priority;
    }
    
    public int getWeight() {
        return weight;
    }

    @Override
    public int compareTo(SRVRecord other) {
        // According to RFC2782,
        // "[a] client MUST attempt to contact the target host with the lowest-numbered priority it can reach".
        // This means that a SRV record with a higher priority is 'less' then one with a lower.
        int res = other.priority - this.priority;
        if (res == 0) {
            res = this.weight - other.weight;
        }
        return res;
    }

    @Override
    public String toString() {
        return super.toString() + " prio:" + priority + ":w:" + weight;
    }
}
