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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import javax.swing.Action;

public class DefaultEditorKitTest extends EditorKitTest {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        kit = new DefaultEditorKit();
    }

    public void testCreateCaret() {
        assertNull("Caret", kit.createCaret());
    }

    public void testCreateDefaultDocument() {
        Document doc = kit.createDefaultDocument();
        assertTrue("document's type", doc instanceof PlainDocument);
        assertEquals("document's length", 0, doc.getLength());
        assertEquals("number of root elements", 2, doc.getRootElements().length);
        assertNull("document's title", doc.getProperty(Document.TitleProperty));
        assertNull("document's StreamDescription", doc
                .getProperty(Document.StreamDescriptionProperty));
    }

    public void testGetActions() {
        Action[] actions1 = kit.getActions();
        Action[] actions2 = kit.getActions();
        String templateStr = "insert-content, delete-previous, delete-next, "
                + "set-read-only, set-writable, cut-to-clipboard, copy-to-clipboard, "
                + "paste-from-clipboard, page-up, page-down, selection-page-up, "
                + "selection-page-down, selection-page-left, selection-page-right, "
                + "insert-break, beep, caret-forward, caret-backward, selection-forward, "
                + "selection-backward, caret-up, caret-down, selection-up, selection-down, "
                + "caret-begin-word, caret-end-word, selection-begin-word, selection-end-word, "
                + "caret-previous-word, caret-next-word, selection-previous-word, selection-next-word, "
                + "caret-begin-line, caret-end-line, selection-begin-line, selection-end-line, "
                + "caret-begin-paragraph, caret-end-paragraph, selection-begin-paragraph, "
                + "selection-end-paragraph, caret-begin, caret-end, selection-begin, selection-end, "
                + "default-typed, insert-tab, select-word, select-line, select-paragraph, select-all, "
                + "unselect, toggle-componentOrientation, dump-model, ";
        assertEquals("number of actions", actions1.length, actions2.length);
        for (int i = 0; i < actions1.length; i++) {
            assertTrue(i + " action is shared", actions1[i] == actions2[i]);
        }
        assertEquals("number of actions", 53, actions1.length);
        for (int i = 0; i < actions1.length; i++) {
            String name = (String) actions1[i].getValue(Action.NAME);
            name += ", ";
            assertTrue(templateStr.indexOf(name) >= 0);
            templateStr = templateStr.replaceFirst(name, "");
        }
        assertEquals("", templateStr);
    }

    public void testGetContentType() {
        assertEquals("content type", "text/plain", kit.getContentType());
    }

    public void testGetViewFactory() {
        assertNull("ViewFactory", kit.getViewFactory());
    }

    public void testActionSharing() {
        assertTrue(
                "actions are being shared",
                new DefaultEditorKit().getActions()[0] == new DefaultEditorKit().getActions()[0]);
        assertTrue(
                "actions are being shared",
                new DefaultEditorKit().getActions()[10] == new DefaultEditorKit().getActions()[10]);
        assertTrue(
                "actions are being shared",
                new DefaultEditorKit().getActions()[20] == new DefaultEditorKit().getActions()[20]);
    }

    /*
     * Class under test for void read(InputStream, Document, int)
     */
    public void testReadInputStreamDocumentint() throws Exception {
        final ByteArrayOutputStream outFile = new ByteArrayOutputStream();
        String str = "This is a very short plain-text document.\nIt's to be read only by the test.";
        outFile.write(str.getBytes());
        InputStream inFile = new ByteArrayInputStream(outFile.toByteArray());
        final Document doc = kit.createDefaultDocument();
        kit.read(inFile, doc, 0);
        assertEquals("document's length", str.length(), doc.getLength());
        inFile.close();
        inFile = new ByteArrayInputStream(outFile.toByteArray());
        kit.read(inFile, doc, 10);
        assertEquals("document's length", 2 * str.length(), doc.getLength());
        String head = null;
        head = doc.getText(0, 20);
        assertEquals("document's head", "This is a This is a ", head);
        inFile.close();
        testExceptionalCase(new BadLocationCase() {
            @Override
            public void exceptionalAction() throws Exception {
                kit.read(new ByteArrayInputStream(outFile.toByteArray()), doc, 1000);
            }
        });
    }

    /*
     * Class under test for void read(Reader, Document, int)
     */
    public void testReadReaderDocumentint() throws Exception {
        String str = "This is a very short plain-text document.\nIt's to be read only by the test.";
        final ByteArrayOutputStream outFile = new ByteArrayOutputStream();
        outFile.write(str.getBytes());
        InputStreamReader inFile = new InputStreamReader(new ByteArrayInputStream(outFile
                .toByteArray()));
        final Document doc = kit.createDefaultDocument();
        readKitReader(inFile, doc, 0);
        assertEquals("document's length", str.length(), doc.getLength());
        inFile.close();
        inFile = new InputStreamReader(new ByteArrayInputStream(outFile.toByteArray()));
        readKitReader(inFile, doc, 10);
        assertEquals("document's length", 2 * str.length(), doc.getLength());
        String head = doc.getText(0, 20);
        assertEquals("document's head", "This is a This is a ", head);
        inFile.close();
        testExceptionalCase(new BadLocationCase() {
            @Override
            public void exceptionalAction() throws Exception {
                kit.read(
                        new InputStreamReader(new ByteArrayInputStream(outFile.toByteArray())),
                        doc, 1000);
            }
        });
    }

    /*
     * Class under test for void write(OutputStream, Document, int, int)
     */
    public void testWriteOutputStreamDocumentintint() throws Exception {
        String str = "This is a very short plain-text document.It's to be read only by the test.";
        final Document doc = kit.createDefaultDocument();
        doc.insertString(0, str, doc.getDefaultRootElement().getAttributes());
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeKitFile(outputStream, str, doc, 0, str.length());
        String strRead = readStringFromFile(new ByteArrayInputStream(outputStream.toByteArray()));
        assertEquals("document", str, strRead);
        outputStream.reset();
        writeKitFile(outputStream, str, doc, 11, str.length() - 13);
        strRead = readStringFromFile(new ByteArrayInputStream(outputStream.toByteArray()));
        assertEquals("document",
                "ery short plain-text document.It's to be read only by the tes", strRead);
        testExceptionalCase(new BadLocationCase() {
            @Override
            public void exceptionalAction() throws Exception {
                kit.write(outputStream, doc, 0, 1000);
            }
        });
    }

    /*
     * Class under test for void write(Writer, Document, int, int)
     */
    public void testWriteWriterDocumentintint() throws Exception {
        String str = "This is a very short plain-text document.It's to be read only by the test.";
        final Document doc = kit.createDefaultDocument();
        doc.insertString(0, str, doc.getDefaultRootElement().getAttributes());
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeKitWriter(outputStream, str, doc, 0, str.length());
        String strRead = readStringFromFile(new ByteArrayInputStream(outputStream.toByteArray()));
        assertEquals("document", str, strRead);
        outputStream.reset();
        writeKitWriter(outputStream, str, doc, 11, str.length() - 13);
        strRead = readStringFromFile(new ByteArrayInputStream(outputStream.toByteArray()));
        assertEquals("document",
                "ery short plain-text document.It's to be read only by the tes", strRead);
        testExceptionalCase(new BadLocationCase() {
            @Override
            public void exceptionalAction() throws Exception {
                kit.write(new OutputStreamWriter(outputStream), doc, 0, 1000);
            }
        });
    }

    public void testWriteWriterFlush() throws Exception {
        final String str = "Test text";
        final Document doc = kit.createDefaultDocument();
        doc.insertString(0, str, null);
        final Marker flushMarker = new Marker();
        final Marker closeMarker = new Marker();
        StringWriter writer = new StringWriter() {
            @Override
            public void close() throws IOException {
                closeMarker.setOccurred();
                super.close();
            }

            @Override
            public void flush() {
                flushMarker.setOccurred();
                super.flush();
            }
        };
        kit.write(writer, doc, 0, doc.getLength());
        assertFalse(closeMarker.isOccurred());
        assertTrue(flushMarker.isOccurred());
    }

    public void testWriteOutputStreamFlush() throws Exception {
        final String str = "Test text";
        final Document doc = kit.createDefaultDocument();
        doc.insertString(0, str, null);
        final Marker flushMarker = new Marker();
        final Marker closeMarker = new Marker();
        OutputStream stream = new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                closeMarker.setOccurred();
                super.close();
            }

            @Override
            public void flush() throws IOException {
                flushMarker.setOccurred();
                super.flush();
            }
        };
        kit.write(stream, doc, 0, doc.getLength());
        assertFalse(closeMarker.isOccurred());
        assertTrue(flushMarker.isOccurred());
    }

    public void testNewLineReader() throws IOException, BadLocationException {
        String str1 = "This is a very \r\nshort plain-text document.\r\nIt's to be read only by the \r\ntest.";
        String str2 = "This is a very \rshort plain-text document.\rIt's to be read only by the test.";
        StringReader reader = new StringReader(str1);
        Document doc = new PlainDocument();
        DefaultEditorKit kit = new DefaultEditorKit();
        kit.read(reader, doc, 0);
        String readStr = doc.getText(0, doc.getLength());
        assertEquals("no \"\\r\" found", -1, readStr.indexOf("\r"));
        assertEquals("string length", str1.length() - 3, readStr.length());
        assertEquals("newLine property", "\r\n", doc
                .getProperty(DefaultEditorKit.EndOfLineStringProperty));
        reader = new StringReader(str2);
        doc = new PlainDocument();
        kit = new DefaultEditorKit();
        kit.read(reader, doc, 0);
        readStr = doc.getText(0, doc.getLength());
        assertEquals("no \"\\r\" found", -1, readStr.indexOf("\r"));
        assertEquals("string length", str2.length(), readStr.length());
        assertEquals("newLine property", "\r", doc
                .getProperty(DefaultEditorKit.EndOfLineStringProperty));
    }

    public void testNewLineInputStream() throws IOException, BadLocationException {
        String str1 = "This is a very \r\nshort plain-text document.\r\nIt's to be read only by the \r\ntest.";
        String str2 = "This is a very \rshort plain-text document.\rIt's to be read only by the test.";
        InputStream reader = new ByteArrayInputStream(str1.getBytes());
        Document doc = new PlainDocument();
        DefaultEditorKit kit = new DefaultEditorKit();
        kit.read(reader, doc, 0);
        String readStr = doc.getText(0, doc.getLength());
        assertEquals("no \"\\r\" found", -1, readStr.indexOf("\r"));
        assertEquals("string length", str1.length() - 3, readStr.length());
        assertEquals("newLine property", "\r\n", doc
                .getProperty(DefaultEditorKit.EndOfLineStringProperty));
        reader = new ByteArrayInputStream(str2.getBytes());
        doc = new PlainDocument();
        kit = new DefaultEditorKit();
        kit.read(reader, doc, 0);
        readStr = doc.getText(0, doc.getLength());
        assertEquals("no \"\\r\" found", -1, readStr.indexOf("\r"));
        assertEquals("string length", str2.length(), readStr.length());
        assertEquals("newLine property", "\r", doc
                .getProperty(DefaultEditorKit.EndOfLineStringProperty));
    }

    public void testNewLineWriter() throws IOException, BadLocationException {
        String str1 = "This is a very \r\nshort plain-text document.\r\nIt's to be read only by the \r\ntest.";
        String str2 = "This is a very \rshort plain-text document.\rIt's to be read only by the \rtest.";
        String str3 = "This is a very \nshort plain-text document.\nIt's to be read only by the \ntest.";
        StringReader reader = new StringReader(str1);
        StringWriter writer = new StringWriter();
        Document doc = new PlainDocument();
        DefaultEditorKit kit = new DefaultEditorKit();
        kit.read(reader, doc, 0);
        kit.write(writer, doc, 0, doc.getLength());
        assertEquals(str1, writer.toString());
        assertEquals(str3, doc.getText(0, doc.getLength()));
        reader = new StringReader(str2);
        writer = new StringWriter();
        doc = new PlainDocument();
        kit = new DefaultEditorKit();
        kit.read(reader, doc, 0);
        kit.write(writer, doc, 0, doc.getLength());
        assertEquals(str2, writer.toString());
        assertEquals(str3, doc.getText(0, doc.getLength()));
    }

    public void testNewLineOutputStream() throws IOException, BadLocationException {
        String str1 = "This is a very \r\nshort plain-text document.\r\nIt's to be read only by the \r\ntest.";
        String str2 = "This is a very \rshort plain-text document.\rIt's to be read only by the \rtest.";
        String str3 = "This is a very \nshort plain-text document.\nIt's to be read only by the \ntest.";
        InputStream reader = new ByteArrayInputStream(str1.getBytes());
        OutputStream writer = new ByteArrayOutputStream();
        Document doc = new PlainDocument();
        DefaultEditorKit kit = new DefaultEditorKit();
        kit.read(reader, doc, 0);
        assertEquals(str3, doc.getText(0, doc.getLength()));
        kit.write(writer, doc, 0, doc.getLength());
        assertEquals(str1, writer.toString());
        reader = new ByteArrayInputStream(str2.getBytes());
        writer = new ByteArrayOutputStream();
        doc = new PlainDocument();
        kit = new DefaultEditorKit();
        kit.read(reader, doc, 0);
        assertEquals(str3, doc.getText(0, doc.getLength()));
        kit.write(writer, doc, 0, doc.getLength());
        assertEquals(str2, writer.toString());
    }

    private void readKitReader(final InputStreamReader inFile, final Document doc, final int pos)
            throws Exception {
        kit.read(inFile, doc, pos);
    }

    private String readStringFromFile(final InputStream inputStream) throws Exception {
        InputStreamReader inFile = new InputStreamReader(inputStream);
        char[] arrayRead = new char[1000];
        int readLength = inFile.read(arrayRead);
        return new String(arrayRead, 0, readLength);
    }

    private void writeKitWriter(final OutputStream outputStream, final String str,
            final Document doc, final int pos, final int length) throws Exception {
        OutputStreamWriter outFile = new OutputStreamWriter(outputStream);
        kit.write(outFile, doc, pos, length);
        outFile.close();
    }

    private void writeKitFile(final OutputStream outputStream, final String str,
            final Document doc, final int pos, final int length) throws Exception {
        kit.write(outputStream, doc, pos, length);
        outputStream.close();
    }
}