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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.event;

import java.net.URL;
import java.util.EventObject;
import javax.swing.text.Element;

public class HyperlinkEvent extends EventObject {

    public static final class EventType {
        public static final EventType ACTIVATED = new EventType("ACTIVATED");
        public static final EventType ENTERED = new EventType("ENTERED");
        public static final EventType EXITED = new EventType("EXITED");

        private final String name;

        private EventType(final String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final EventType type;
    private final String description;
    private final Element sourceElement;
    private final URL url;

    public HyperlinkEvent(final Object source, final EventType type,
                          final URL url) {
        this(source, type, url, null, null);
    }

    public HyperlinkEvent(final Object source, final EventType type,
                          final URL url, final String desc) {
        this(source, type, url, desc, null);
    }



    public HyperlinkEvent(final Object source,
                          final EventType type,
                          final URL url,
                          final String desc,
                          final Element sourceElement) {
        super(source);
        this.type = type;
        this.url = url;
        this.description = desc;
        this.sourceElement = sourceElement;
    }

    public String getDescription() {
        return description;
    }

    public EventType getEventType() {
        return type;
    }

    public Element getSourceElement() {
        return sourceElement;
    }

    public URL getURL() {
        return url;
    }
}

