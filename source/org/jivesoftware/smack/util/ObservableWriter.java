/**
* $RCSfile$
* $Revision$
* $Date$
*
* Copyright (C) 2002-2003 Jive Software. All rights reserved.
* ====================================================================
* The Jive Software License (based on Apache Software License, Version 1.1)
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution,
*    if any, must include the following acknowledgment:
*       "This product includes software developed by
*        Jive Software (http://www.jivesoftware.com)."
*    Alternately, this acknowledgment may appear in the software itself,
*    if and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Smack" and "Jive Software" must not be used to
*    endorse or promote products derived from this software without
*    prior written permission. For written permission, please
*    contact webmaster@jivesoftware.com.
*
* 5. Products derived from this software may not be called "Smack",
*    nor may "Smack" appear in their name, without prior written
*    permission of Jive Software.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
*/

package org.jivesoftware.smack.util;

import java.io.*;
import java.util.*;

/**
 * An ObservableWriter is a wrapper on a Writer that notifies to its listeners when
 * writing to character streams.
 * 
 * @author Gaston Dombiak
 */
public class ObservableWriter extends Writer {

    Writer wrappedWriter = null;
    List listeners = new ArrayList();

    public ObservableWriter(Writer wrappedWriter) {
        this.wrappedWriter = wrappedWriter;
    }

    public void write(char cbuf[], int off, int len) throws IOException {
        wrappedWriter.write(cbuf, off, len);
        String str = new String(cbuf, off, len);
        notifyListeners(str);
    }

    public void flush() throws IOException {
        wrappedWriter.flush();
    }

    public void close() throws IOException {
        wrappedWriter.close();
    }

    public void write(int c) throws IOException {
        wrappedWriter.write(c);
    }

    public void write(char cbuf[]) throws IOException {
        wrappedWriter.write(cbuf);
        String str = new String(cbuf);
        notifyListeners(str);
    }

    public void write(String str) throws IOException {
        wrappedWriter.write(str);
        notifyListeners(str);
    }

    public void write(String str, int off, int len) throws IOException {
        wrappedWriter.write(str, off, len);
        str = str.substring(off, off + len);
        notifyListeners(str);
    }

    /**
     * Notify that a new string has been written.
     * 
     * @param str the written String to notify 
     */
    private void notifyListeners(String str) {
        WriterListener[] writerListeners = null;
        synchronized (listeners) {
            writerListeners = new WriterListener[listeners.size()];
            listeners.toArray(writerListeners);
        }
        for (int i = 0; i < writerListeners.length; i++) {
            writerListeners[i].write(str);
        }
    }

    /**
     * Adds a writer listener to this writer that will be notified when
     * new strings are sent.
     *
     * @param writerListener a writer listener.
     */
    public void addWriterListener(WriterListener writerListener) {
        if (writerListener == null) {
            return;
        }
        synchronized (listeners) {
            if (!listeners.contains(writerListener)) {
                listeners.add(writerListener);
            }
        }
    }

    /**
     * Removes a writer listener from this writer.
     *
     * @param writerListener a writer listener.
     */
    public void removeWriterListener(WriterListener writerListener) {
        synchronized (listeners) {
            listeners.remove(writerListener);
        }
    }

}
