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
 * @author Alexander T. Simbirtsev
 */
package javax.swing.text.html;

import java.awt.Component;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class ObjectView extends ComponentView {
    public ObjectView(final Element elem) {
        super(elem);
        throw new UnsupportedOperationException(Messages.getString("swing.9F")); //$NON-NLS-1$
    }

    protected Component createComponent() {
        throw new UnsupportedOperationException(Messages.getString("swing.9F")); //$NON-NLS-1$
    }
}


