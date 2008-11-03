/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright 2005-2007 Jive Software.
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

package org.jivesoftware.smackx.commands;

/**
 * Notes can be added to a command execution response. A note has a type and value.
 * 
 * @author Gabriel Guardincerri
 */
public class AdHocCommandNote {

    private Type type;
    private String value;

    /**
     * Creates a new adhoc command note with the specified type and value.
     *
     * @param type the type of the note.
     * @param value the value of the note.
     */
    public AdHocCommandNote(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Returns the value or message of the note.
     * 
     * @return the value or message of the note.
     */
    public String getValue() {
        return value;
    }

    /**
     * Return the type of the note.
     * 
     * @return the type of the note.
     */
    public Type getType() {
        return type;
    }

    /**
     * Represents a note type.
     */
    public enum Type {

        /**
         * The note is informational only. This is not really an exceptional
         * condition.
         */
        info,

        /**
         * The note indicates a warning. Possibly due to illogical (yet valid)
         * data.
         */
        warn,

        /**
         * The note indicates an error. The text should indicate the reason for
         * the error.
         */
        error
    }

}