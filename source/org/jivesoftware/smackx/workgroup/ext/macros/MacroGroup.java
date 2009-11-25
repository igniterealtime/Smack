/**
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.workgroup.ext.macros;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * MacroGroup datamodel.
 */
public class MacroGroup {
    private List<Macro> macros;
    private List<MacroGroup> macroGroups;


    // Define MacroGroup
    private String title;

    public MacroGroup() {
        macros = new ArrayList<Macro>();
        macroGroups = new ArrayList<MacroGroup>();
    }

    public void addMacro(Macro macro) {
        macros.add(macro);
    }

    public void removeMacro(Macro macro) {
        macros.remove(macro);
    }

    public Macro getMacroByTitle(String title) {
        Collection<Macro> col = Collections.unmodifiableList(macros);
        Iterator<Macro> iter = col.iterator();
        while (iter.hasNext()) {
            Macro macro = (Macro)iter.next();
            if (macro.getTitle().equalsIgnoreCase(title)) {
                return macro;
            }
        }
        return null;
    }

    public void addMacroGroup(MacroGroup group) {
        macroGroups.add(group);
    }

    public void removeMacroGroup(MacroGroup group) {
        macroGroups.remove(group);
    }

    public Macro getMacro(int location) {
        return (Macro)macros.get(location);
    }

    public MacroGroup getMacroGroupByTitle(String title) {
        Collection<MacroGroup> col = Collections.unmodifiableList(macroGroups);
        Iterator<MacroGroup> iter = col.iterator();
        while (iter.hasNext()) {
            MacroGroup group = (MacroGroup)iter.next();
            if (group.getTitle().equalsIgnoreCase(title)) {
                return group;
            }
        }
        return null;
    }

    public MacroGroup getMacroGroup(int location) {
        return (MacroGroup)macroGroups.get(location);
    }


    public List<Macro>  getMacros() {
        return macros;
    }

    public void setMacros(List<Macro> macros) {
        this.macros = macros;
    }

    public List<MacroGroup> getMacroGroups() {
        return macroGroups;
    }

    public void setMacroGroups(List<MacroGroup> macroGroups) {
        this.macroGroups = macroGroups;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
    public String toXML() {
    	StringBuilder buf = new StringBuilder();
    	buf.append("<macrogroup>");
    	buf.append("<title>" +  getTitle() + "</title>");
    	buf.append("<macros>");
    	for (Macro macro : getMacros())
		{
    		buf.append("<macro>");
    		buf.append("<title>" + macro.getTitle() + "</title>");
    		buf.append("<type>" + macro.getType() + "</type>");
    		buf.append("<description>" + macro.getDescription() + "</description>");
    		buf.append("<response>" + macro.getResponse() + "</response>");
    		buf.append("</macro>");
		}
    	buf.append("</macros>");
    	
    	if (getMacroGroups().size() > 0) {
    		buf.append("<macroGroups>");
    		for (MacroGroup groups : getMacroGroups()) {
    			buf.append(groups.toXML());
    		}
    		buf.append("</macroGroups>");
    	}
    	buf.append("</macrogroup>");
    	return buf.toString(); 
    }
}
