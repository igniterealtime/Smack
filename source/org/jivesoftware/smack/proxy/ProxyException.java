/**
 * $RCSfile$
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
package org.jivesoftware.smack.proxy;

import java.io.IOException;

/**
 * An exception class to handle exceptions caused by proxy.
 * 
 * @author Atul Aggarwal
 */
public class ProxyException 
    extends IOException
{
    public ProxyException(ProxyInfo.ProxyType type, String ex, Throwable cause)
    {
        super("Proxy Exception " + type.toString() + " : "+ex+", "+cause);
    }
    
    public ProxyException(ProxyInfo.ProxyType type, String ex)
    {
        super("Proxy Exception " + type.toString() + " : "+ex);
    }
    
    public ProxyException(ProxyInfo.ProxyType type)
    {
        super("Proxy Exception " + type.toString() + " : " + "Unknown Error");
    }
}
