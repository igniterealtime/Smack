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

package org.jivesoftware.smack.util;

import java.io.*;
import java.util.*;

/**
 * An ObservableReader is a wrapper on a Reader that notifies to its listeners when
 * reading character streams.
 * 
 * @author Gaston Dombiak
 */
public class ObservableReader extends Reader {

    Reader wrappedReader = null;
    List listeners = new ArrayList();

    public ObservableReader(Reader wrappedReader) {
        this.wrappedReader = wrappedReader;
    }
        
    public int read(char[] cbuf, int off, int len) throws IOException {
        int count = wrappedReader.read(cbuf, off, len);
        if (count > 0) {
            String str = new String(cbuf, off, count);
            // Notify that a new string has been read
            ReaderListener[] readerListeners = null;
            synchronized (listeners) {
                readerListeners = new ReaderListener[listeners.size()];
                listeners.toArray(readerListeners);
            }
            for (int i = 0; i < readerListeners.length; i++) {
                readerListeners[i].read(str);
            }
        }
        return count;
    }

    public void close() throws IOException {
        wrappedReader.close();
    }

    public int read() throws IOException {
        return wrappedReader.read();
    }

    public int read(char cbuf[]) throws IOException {
        return wrappedReader.read(cbuf);
    }

    public long skip(long n) throws IOException {
        return wrappedReader.skip(n);
    }

    public boolean ready() throws IOException {
        return wrappedReader.ready();
    }

    public boolean markSupported() {
        return wrappedReader.markSupported();
    }

    public void mark(int readAheadLimit) throws IOException {
        wrappedReader.mark(readAheadLimit);
    }

    public void reset() throws IOException {
        wrappedReader.reset();
    }

    /**
     * Adds a reader listener to this reader that will be notified when
     * new strings are read.
     *
     * @param readerListener a reader listener.
     */
    public void addReaderListener(ReaderListener readerListener) {
        if (readerListener == null) {
            return;
        }
        synchronized (listeners) {
            if (!listeners.contains(readerListener)) {
                listeners.add(readerListener);
            }
        }
    }

    /**
     * Removes a reader listener from this reader.
     *
     * @param readerListener a reader listener.
     */
    public void removeReaderListener(ReaderListener readerListener) {
        synchronized (listeners) {
            listeners.remove(readerListener);
        }
    }

}
