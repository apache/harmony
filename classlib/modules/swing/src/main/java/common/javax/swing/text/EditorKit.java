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
package javax.swing.text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import javax.swing.Action;
import javax.swing.JEditorPane;

public abstract class EditorKit implements Cloneable, Serializable {

    public abstract Caret createCaret();

    public abstract Document createDefaultDocument();

    public Object clone() {
        Object result = null;
        try {
            result = super.clone();
        } catch (final CloneNotSupportedException e) {
        }

        return result;
    }

    public void install(final JEditorPane jep) {
    }

    public void deinstall(final JEditorPane jep) {
    }

    public abstract Action[] getActions();

    public abstract String getContentType();

    public abstract ViewFactory getViewFactory();

    public abstract void read(InputStream in, Document doc, int pos)
            throws IOException, BadLocationException;

    public abstract void read(Reader in, Document doc, int pos)
            throws IOException, BadLocationException;

    public abstract void write(OutputStream out, Document doc, int pos, int len)
            throws IOException, BadLocationException;

    public abstract void write(Writer out, Document doc, int pos, int len)
            throws IOException, BadLocationException;

}

