/**
 *
 * Copyright 2009 Jonas Ådahl.
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

package org.jivesoftware.smack.serverless;


import org.jivesoftware.smack.util.Tuple;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class for describing a Link-local presence information according to XEP-0174.
 * XEP-0174 describes how to represent XMPP presences using mDNS/DNS-SD.
 * The presence information is stored as TXT fields; example from the documentation
 * follows:
 *        juliet IN TXT "txtvers=1"
 *        juliet IN TXT "1st=Juliet"
 *        juliet IN TXT "email=juliet@capulet.lit"
 *        juliet IN TXT "hash=sha-1"
 *        juliet IN TXT "jid=juliet@capulet.lit"
 *        juliet IN TXT "last=Capulet"
 *        juliet IN TXT "msg=Hanging out downtown"
 *        juliet IN TXT "nick=JuliC"
 *        juliet IN TXT "node=http://www.adiumx.com"
 *        juliet IN TXT "phsh=a3839614e1a382bcfebbcf20464f519e81770813"
 *        juliet IN TXT "port.p2pj=5562"
 *        juliet IN TXT "status=avail"
 *        juliet IN TXT "vc=CA!"
 *        juliet IN TXT "ver=66/0NaeaBKkwk85efJTGmU47vXI="
 */
public class LLPresence {
    // Service info, gathered from the TXT fields
    private String firstName, lastName, email, msg, nick, jid;
    // caps version
    private String hash, ver, node;
    // XEP-0174 specifies that if status is not specified it is equal to "avail".
    private Mode status = Mode.avail;

    // The unknown
    private Map<String,String> rest =
        new ConcurrentHashMap<String,String>();

    public static enum Mode {
        avail, away, dnd
    }

    // Host details
    private int port = 0;
    private String host;
    private String serviceName;

    public LLPresence(String serviceName) {
        this.serviceName = serviceName;
    }

    public LLPresence(String serviceName, String host, int port) {
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
    }

    public LLPresence(String serviceName, String host, int port,
            List<Tuple<String,String>> records) {
        this(serviceName, host, port);

        // Parse the tuple list (originating from the TXT fields) and put them
        // in variables
        for (Tuple<String,String> t : records) {
            if (t.a.equals("1st"))
                setFirstName(t.b);
            else if (t.a.equals("last"))
                setLastName(t.b);
            else if (t.a.equals("email"))
                setEMail(t.b);
            else if (t.a.equals("jid"))
                setJID(t.b);
            else if (t.a.equals("nick"))
                setNick(t.b);
            else if (t.a.equals("hash"))
                setHash(t.b);
            else if (t.a.equals("node"))
                setNode(t.b);
            else if (t.a.equals("ver"))
                setVer(t.b);
            else if (t.a.equals("status")) {
                try {
                    setStatus(Mode.valueOf(t.b));
                }
                catch (IllegalArgumentException iae) {
                    System.err.println("Found invalid presence status (" +
                            t.b + ") in TXT entry.");
                }
            }
            else if (t.a.equals("msg"))
                setMsg(t.b);
            else {
                // Unknown key
                if (!rest.containsKey(t.a))
                    rest.put(t.a, t.b);
            }
        }
    }

    public List<Tuple<String, String>> toList() {
        LinkedList<Tuple<String, String>> list = new LinkedList<>();
        list.add(new Tuple<>("txtvers", "1"));
        list.add(new Tuple<>("1st", firstName));
        list.add(new Tuple<>("last", lastName));
        list.add(new Tuple<>("email", email));
        list.add(new Tuple<>("jid", jid));
        list.add(new Tuple<>("nick", nick));
        list.add(new Tuple<>("status", status.toString()));
        list.add(new Tuple<>("msg", msg));
        list.add(new Tuple<>("hash", hash));
        list.add(new Tuple<>("node", node));
        list.add(new Tuple<>("ver", ver));
        list.add(new Tuple<>("port.p2ppj", Integer.toString(port)));

        for (Map.Entry<String,String> e : rest.entrySet()) {
            list.add(new Tuple<>(e.getKey(), e.getValue()));
        }

        return list;
    }

    /**
     * Update all the values of the presence.
     */
    void update(LLPresence p) {
        setFirstName(p.getFirstName());
        setLastName(p.getLastName());
        setEMail(p.getEMail());
        setMsg(p.getMsg());
        setNick(p.getNick());
        setStatus(p.getStatus());
        setJID(p.getJID());
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setFirstName(String name) {
        firstName = name;
    }

    public void setLastName(String name) {
        lastName = name;
    }

    public void setEMail(String email) {
        this.email = email;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public void setStatus(Mode status) {
        this.status = status;
    }

    public void setJID(String jid) {
        this.jid = jid;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public void setVer(String ver) {
        this.ver = ver;
    }

    void setPort(int port) {
        this.port = port;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEMail() {
        return email;
    }

    public String getMsg() {
        return msg;
    }

    public String getNick() {
        return nick;
    }

    public Mode getStatus() {
        return status;
    }

    public String getJID() {
        return jid;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getHost() {
        return host;
    }

    public String getHash() {
        return hash;
    }

    public String getNode() {
        return node;
    }

    public String getVer() {
        return ver;
    }

    public String getNodeVer() {
        return node + "#" + hash;
    }

    public int getPort() {
        return port;
    }

    public String getValue(String key) {
        return rest.get(key);
    }

    public void putValue(String key, String value) {
        rest.put(key, value);
    }

    public boolean equals(Object o) {
        if (o instanceof LLPresence) {
            LLPresence p = (LLPresence)o;
            return p.serviceName == serviceName &&
                p.host == host;
        }
        return false;
    }

    public int hashCode() {
        return serviceName.hashCode();
    }
}
