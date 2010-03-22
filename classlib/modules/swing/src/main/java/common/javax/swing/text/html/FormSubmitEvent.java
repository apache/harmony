/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Vadim L. Bogdanov
 */

package javax.swing.text.html;

import java.net.URL;

import javax.swing.text.Element;

public class FormSubmitEvent extends HTMLFrameHyperlinkEvent {

    public static enum MethodType {GET, POST}

    private String data;
    private MethodType method;

    FormSubmitEvent(final Object source, final EventType type,
                    final URL targetURL, final String desc,
                    final Element sourceElement,
                    final String targetFrame,
                    final MethodType method,
                    final String data) {

        super(source, type, targetURL, targetFrame);

        this.data = data;
        this.method = method;
    }

    public String getData() {
        return data;
    }

    public MethodType getMethod() {
        return method;
    }
}
