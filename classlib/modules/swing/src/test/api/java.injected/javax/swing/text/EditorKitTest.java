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
 * Created on 03.03.2005

 */
package javax.swing.text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import javax.swing.Action;
import javax.swing.SwingTestCase;

public class EditorKitTest extends SwingTestCase {
    protected EditorKit kit = null;

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(EditorKitTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        kit = new EditorKit() {
            private static final long serialVersionUID = 1L;

            protected int i = 0;

            @Override
            public Caret createCaret() {
                return null;
            }

            @Override
            public Document createDefaultDocument() {
                return null;
            }

            @Override
            public Action[] getActions() {
                return null;
            }

            @Override
            public String getContentType() {
                return i + "";
            }

            @Override
            public ViewFactory getViewFactory() {
                i += 100;
                return null;
            }

            @Override
            public void read(final InputStream in, final Document doc, final int pos)
                    throws IOException, BadLocationException {
            }

            @Override
            public void read(final Reader in, final Document doc, final int pos)
                    throws IOException, BadLocationException {
            }

            @Override
            public void write(final OutputStream out, final Document doc, final int pos,
                    final int len) throws IOException, BadLocationException {
            }

            @Override
            public void write(final Writer out, final Document doc, final int pos, final int len)
                    throws IOException, BadLocationException {
            }
        };
    }

    /*
     * Class under test for Object clone()
     */
    public void testClone() {
        kit.getViewFactory();
        assertTrue("cloned", kit.clone() != null);
        assertEquals("cloned field", kit.getContentType(), ((EditorKit) kit.clone())
                .getContentType());
    }

    public void testDeinstall() {
    }

    public void testInstall() {
    }
}
