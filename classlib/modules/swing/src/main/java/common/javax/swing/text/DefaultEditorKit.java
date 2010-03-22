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
 * @author Alexander T. Simbirtsev, Evgeniya G. Maenkova
 */
package javax.swing.text;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import javax.swing.Action;

import org.apache.harmony.awt.text.ActionSet;

@SuppressWarnings("serial")
public class DefaultEditorKit extends EditorKit {
    private static class KitAction extends TextAction {
        public KitAction(final String name) {
            super(name, true);
        }

        public void actionPerformed(final ActionEvent e) {
            performTextAction(e);
        }
    }

    public static class BeepAction extends TextAction {
        public BeepAction() {
            super(beepAction, true);
        }

        public void actionPerformed(final ActionEvent e) {
            performTextAction(e);
        }
    }

    public static class CopyAction extends TextAction {
        public CopyAction() {
            super(copyAction, true);
        }

        public void actionPerformed(final ActionEvent e) {
            performTextAction(e);
        }
    }

    public static class CutAction extends TextAction {
        public CutAction() {
            super(cutAction, true);
        }

        public void actionPerformed(final ActionEvent e) {
            performTextAction(e);
        }
    }

    public static class DefaultKeyTypedAction extends TextAction {
        public DefaultKeyTypedAction() {
            super(defaultKeyTypedAction, true);
        }

        public void actionPerformed(final ActionEvent event) {
            if (event == null) {
                return;
            }
            JTextComponent source = getEditableTextComponent(event);
            String text = event.getActionCommand();
            if (source != null) {
                if ((text == null)
                      || Character.getType(text.charAt(0)) != Character.CONTROL
                      || (text.charAt(0) > 127)) {

                     source.replaceSelection(text);
                     setCurrentPositionAsMagic(source);
                }
            }
        }
    }

    public static class InsertBreakAction extends TextAction {
        public InsertBreakAction() {
            super(insertBreakAction, true);
        }
        public void actionPerformed(final ActionEvent e) {
            performTextAction(e);
        }
    }

    public static class InsertContentAction extends TextAction {
        public InsertContentAction() {
            super(insertContentAction, true);
        }

        public void actionPerformed(final ActionEvent event) {
            if (event == null) {
                return;
            }
            final JTextComponent c = getEditableTextComponent(event);
            if (c != null) {
                final String content = event.getActionCommand();
                if (content != null) {
                    c.replaceSelection(content);
                }
            }
        }
    }

    public static class InsertTabAction extends TextAction {
        public InsertTabAction() {
            super(insertTabAction, true);
        }

        public void actionPerformed(final ActionEvent e) {
            performTextAction(e);
        }
    }

    public static class PasteAction extends TextAction {
        public PasteAction() {
            super(pasteAction, true);
        }

        public void actionPerformed(final ActionEvent e) {
            performTextAction(e);
        }
    }

    static final class ReadOnlyAction extends TextAction {
        public ReadOnlyAction() {
            super(readOnlyAction, true);
        }

        public void actionPerformed(final ActionEvent event) {
            JTextComponent source = getEditableTextComponent(event);
            if (source != null) {
                source.setEditable(false);
            }
        }
    }

    static final class WritableAction extends TextAction {

        public WritableAction() {
            super(writableAction, true);
        }

        public void actionPerformed(final ActionEvent event) {
            JTextComponent source = getTextComponent(event);
            if (source != null) {
                source.setEditable(true);
            }
        }

    }

    public static final String backwardAction = ActionSet.backwardAction;

    public static final String beepAction = ActionSet.beepAction;

    public static final String beginAction = ActionSet.beginAction;

    public static final String beginLineAction = ActionSet.beginLineAction;

    public static final String beginParagraphAction = ActionSet.beginParagraphAction;

    public static final String beginWordAction = ActionSet.beginWordAction;

    public static final String copyAction = ActionSet.copyAction;

    public static final String cutAction = ActionSet.cutAction;

    public static final String defaultKeyTypedAction = ActionSet.defaultKeyTypedAction;

    public static final String deleteNextCharAction = ActionSet.deleteNextCharAction;

    public static final String deletePrevCharAction = ActionSet.deletePrevCharAction;

    public static final String downAction = ActionSet.downAction;

    static final String dumpModelAction = ActionSet.dumpModelAction;

    public static final String endAction = ActionSet.endAction;

    public static final String endLineAction = ActionSet.endLineAction;

    public static final String EndOfLineStringProperty = "__EndOfLine__";

    public static final String endParagraphAction = ActionSet.endParagraphAction;

    public static final String endWordAction = ActionSet.endWordAction;

    public static final String forwardAction = ActionSet.forwardAction;

    public static final String insertBreakAction = ActionSet.insertBreakAction;

    public static final String insertContentAction = ActionSet.insertContentAction;

    public static final String insertTabAction = ActionSet.insertTabAction;

    public static final String nextWordAction = ActionSet.nextWordAction;

    public static final String pageDownAction = ActionSet.pageDownAction;

    public static final String pageUpAction = ActionSet.pageUpAction;

    public static final String pasteAction = ActionSet.pasteAction;

    public static final String previousWordAction = ActionSet.previousWordAction;

    public static final String readOnlyAction = ActionSet.readOnlyAction;

    public static final String selectAllAction = ActionSet.selectAllAction;

    public static final String selectionBackwardAction = ActionSet.selectionBackwardAction;

    public static final String selectionBeginAction = ActionSet.selectionBeginAction;

    public static final String selectionBeginLineAction = ActionSet.selectionBeginLineAction;

    public static final String selectionBeginParagraphAction = ActionSet.selectionBeginParagraphAction;

    public static final String selectionBeginWordAction = ActionSet.selectionBeginWordAction;

    public static final String selectionDownAction = ActionSet.selectionDownAction;

    public static final String selectionEndAction = ActionSet.selectionEndAction;

    public static final String selectionEndLineAction = ActionSet.selectionEndLineAction;

    public static final String selectionEndParagraphAction = ActionSet.selectionEndParagraphAction;

    public static final String selectionEndWordAction = ActionSet.selectionEndWordAction;

    public static final String selectionForwardAction = ActionSet.selectionForwardAction;

    public static final String selectionNextWordAction = ActionSet.selectionNextWordAction;

    static final String selectionPageDownAction = ActionSet.selectionPageDownAction;

    static final String selectionPageLeftAction = ActionSet.selectionPageLeftAction;

    static final String selectionPageRightAction = ActionSet.selectionPageRightAction;

    static final String selectionPageUpAction = ActionSet.selectionPageUpAction;

    public static final String selectionPreviousWordAction = ActionSet.selectionPreviousWordAction;

    public static final String selectionUpAction = ActionSet.selectionUpAction;

    public static final String selectLineAction = ActionSet.selectLineAction;

    public static final String selectParagraphAction = ActionSet.selectParagraphAction;

    public static final String selectWordAction = ActionSet.selectWordAction;

    static final String toggleComponentOrientationAction = ActionSet.toggleComponentOrientationAction;

    static final String unselectAction = ActionSet.unselectAction;

    public static final String upAction = ActionSet.upAction;

    public static final String writableAction = ActionSet.writableAction;

    static final TextAction selectWordDoing = new KitAction(selectWordAction);
    static final TextAction selectLineDoing = new KitAction(selectLineAction);


    private static final int CHARACTERS_TO_READ_AT_ONCE = 256;
    private static final String CONTENT_TYPE = "text/plain";

    /**
     * array containing all actions that DEK provides by default
     * it is shared by all instances of DEK
     */
    private static Action[] actions;

    @Override
    public Caret createCaret() {
        return null;
    }

    @Override
    public Document createDefaultDocument() {
        return new PlainDocument();
    }

    @Override
    public Action[] getActions() {
        if (actions == null) {
            initActions();
        }
        return actions.clone();
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public ViewFactory getViewFactory() {
        return null;
    }

    @Override
    public void read(final InputStream in, final Document doc, final int pos)
            throws IOException, BadLocationException {
        read(new InputStreamReader(in), doc, pos);
    }

    @Override
    public void read(final Reader in, final Document doc, final int pos)
            throws IOException, BadLocationException {
        if (!in.ready()) {
            return;
        }
        int maxCharToRead = CHARACTERS_TO_READ_AT_ONCE;
        char[] readArray = new char[maxCharToRead];
        int numCharRead = -1;
        AttributeSet attributes = doc.getDefaultRootElement().getAttributes();
        int offset = pos;
        boolean delimiterInitialised = false;
        doc.putProperty(EndOfLineStringProperty, null);
        while ((numCharRead = in.read(readArray, 0, maxCharToRead)) != -1) {
            String readStr = new String(readArray, 0, numCharRead);
            if (!delimiterInitialised) {
                final String lineDelimeter = checkDelimiters(readStr);
                if (lineDelimeter != null) {
                    doc.putProperty(EndOfLineStringProperty, lineDelimeter);
                    delimiterInitialised = true;
                }
            }
            if (delimiterInitialised) {
                readStr = replaceLineDelimiters(readStr);
            }
            doc.insertString(offset, readStr, attributes);
            offset += readStr.length();
        }
    }

    @Override
    public void write(final OutputStream out, final Document doc,
            final int pos, final int len) throws IOException,
            BadLocationException {
        write(new OutputStreamWriter(out), doc, pos, len);
    }

    @Override
    public void write(final Writer out, final Document doc, final int pos,
            final int len) throws IOException, BadLocationException {
        String writeStr = doc.getText(pos, len);
        String newLine = (String) doc.getProperty(EndOfLineStringProperty);
        if (newLine != null) {
            writeStr = writeStr.replaceAll("\n", (String) doc
                    .getProperty(EndOfLineStringProperty));
        }
        out.write(writeStr);
        out.flush();
    }

    private String checkDelimiters(final String str) {
        String lineDelimeter = null;
        final int length = str.length();
        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);
            if (c == '\n') {
                lineDelimeter = "\n";
                break;
            } else if (c == '\r') {
                if (i + 1 < length && str.charAt(i + 1) == '\n') {
                    lineDelimeter = "\r\n";
                } else {
                    lineDelimeter = "\r";
                }
                break;
            }
        }
        return lineDelimeter;
    }

    private String replaceLineDelimiters(final String str) {
        int index = str.indexOf('\r');
        if (index == -1) {
            return str;
        }
        final int length = str.length();
        final StringBuilder buffer = new StringBuilder(length);
        int prevIndex = 0;
        do {
            buffer.append(str.subSequence(prevIndex, index));
            buffer.append('\n');
            if (index + 1 < length && str.charAt(index + 1) == '\n') {
                index++;
            }
            prevIndex = index + 1;
            index = str.indexOf('\r', prevIndex);
        } while (index != -1);
        buffer.append(str.subSequence(prevIndex, length));
        return buffer.toString();
    }

/*
 * TODO Simplify this construction.
 * 1) May be store indexes as in original version
 * 2) Find solution how to get these TextAction's from AWTTextAction
 */
    private void initActions() {
        actions = new Action[] {
              new InsertContentAction(),
              new KitAction(deletePrevCharAction),
              new KitAction(deleteNextCharAction),
              new ReadOnlyAction(),
              new WritableAction(),
              new CutAction(),
              new CopyAction(),
              new PasteAction(),
              new KitAction(pageUpAction),
              new KitAction(pageDownAction),
              new KitAction(selectionPageUpAction),
              new KitAction(selectionPageDownAction),
              new KitAction(selectionPageLeftAction),
              new KitAction(selectionPageRightAction),
              new InsertBreakAction(),
              new BeepAction(),
              new KitAction(forwardAction),
              new KitAction(backwardAction),
              new KitAction(selectionForwardAction),
              new KitAction(selectionBackwardAction),
              new KitAction(upAction),
              new KitAction(downAction),
              new KitAction(selectionUpAction),
              new KitAction(selectionDownAction),
              new KitAction(beginWordAction),
              new KitAction(selectionBeginWordAction),
              new KitAction(endAction),
              new KitAction(selectionEndAction),
              new KitAction(endParagraphAction),
              new KitAction(selectionEndParagraphAction),
              new KitAction(endLineAction),
              new KitAction(selectionEndLineAction),
              new KitAction(endWordAction),
              new KitAction(selectionEndWordAction),
              new KitAction(previousWordAction),
              new KitAction(selectionPreviousWordAction),
              new KitAction(nextWordAction),
              new KitAction(selectionNextWordAction),
              new KitAction(beginLineAction),
              new KitAction(selectionBeginLineAction),
              new KitAction(beginParagraphAction),
              new KitAction(selectionBeginParagraphAction),
              new KitAction(beginAction),
              new KitAction(selectionBeginAction),
              new DefaultKeyTypedAction(),
              new InsertTabAction(),
              selectWordDoing,
              selectLineDoing,
              new KitAction(selectParagraphAction),
              new KitAction(selectAllAction),
              new KitAction(unselectAction),
              new KitAction(toggleComponentOrientationAction),
              new KitAction(dumpModelAction)
         };
    }
}
