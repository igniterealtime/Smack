/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smack.websocket;

import java.util.Collections;
import java.util.List;

public final class WebsocketException extends Exception {
    private static final long serialVersionUID = 1L;

    private final List<Throwable> throwableList;

    public WebsocketException(List<Throwable> throwableList) {
        this.throwableList = throwableList;
    }

    public WebsocketException(Throwable throwable) {
        this.throwableList = Collections.singletonList(throwable);
    }

    public List<Throwable> getThrowableList() {
        return throwableList;
    }
}
