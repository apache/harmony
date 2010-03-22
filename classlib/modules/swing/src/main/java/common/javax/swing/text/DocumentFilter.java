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
 * @author Anton Avtamonov
 */
package javax.swing.text;

public class DocumentFilter {
    public abstract static class FilterBypass {
        public abstract Document getDocument();

        public abstract void remove(int offset, int length)
            throws BadLocationException;

        public abstract void insertString(int offset, String string,
                                          AttributeSet attrs)
            throws BadLocationException;

        public abstract void replace(int offset, int length,
                                     String text, AttributeSet attrs)
            throws BadLocationException;
    }

    public void remove(final FilterBypass fb,
                       final int offset,
                       final int length) throws BadLocationException {
        fb.remove(offset, length);
    }

    public void insertString(final FilterBypass fb, final int offset,
                             final String text, final AttributeSet attrs)
        throws BadLocationException {

        fb.insertString(offset, text, attrs);
    }

    public void replace(final FilterBypass fb,
                        final int offset, final int length,
                        final String text, final AttributeSet attrs)
        throws BadLocationException {

        fb.replace(offset, length, text, attrs);
    }
}
