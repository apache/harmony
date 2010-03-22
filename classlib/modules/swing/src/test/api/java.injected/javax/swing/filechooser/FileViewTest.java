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
 * @author Anton Avtamonov, Sergey Burlak
 */
package javax.swing.filechooser;

import java.io.File;
import javax.swing.SwingTestCase;

public class FileViewTest extends SwingTestCase {
    private FileView view;

    private File file;

    public FileViewTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        view = new FileView() {
        };
        file = new File("");
    }

    @Override
    protected void tearDown() throws Exception {
        view = null;
        file = null;
    }

    public void testGetName() throws Exception {
        assertNull(view.getName(file));
        assertNull(view.getName(null));
    }

    public void testGetDescription() throws Exception {
        assertNull(view.getDescription(file));
        assertNull(view.getDescription(null));
    }

    public void testGetTypeDescription() throws Exception {
        assertNull(view.getTypeDescription(file));
        assertNull(view.getTypeDescription(null));
    }

    public void testGetIcon() throws Exception {
        assertNull(view.getIcon(file));
        assertNull(view.getIcon(null));
    }

    public void testIsTraversable() throws Exception {
        assertNull(view.isTraversable(file));
        assertNull(view.isTraversable(null));
    }
}
