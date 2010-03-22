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
* @author Vadim L. Bogdanov, Alexander T. Simbirtsev
*/
package javax.swing.text.html;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Position;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.Position.Bias;
import javax.swing.text.html.parser.ParserDelegator;

import org.apache.harmony.awt.text.TextUtils;
import org.apache.harmony.x.swing.StringConstants;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class HTMLEditorKit extends StyledEditorKit implements Accessible {
    public static class HTMLFactory implements ViewFactory {

        public View create(final Element elem) {
            HTML.Tag tag = getHTMLTagByElement(elem);

            if (HTML.Tag.CONTENT.equals(tag)) {
                return new InlineView(elem);

            } else if (HTML.Tag.IMPLIED.equals(tag)
                    || HTML.Tag.P.equals(tag)
                    || HTML.Tag.H1.equals(tag)
                    || HTML.Tag.H2.equals(tag)
                    || HTML.Tag.H3.equals(tag)
                    || HTML.Tag.H4.equals(tag)
                    || HTML.Tag.H5.equals(tag)
                    || HTML.Tag.H6.equals(tag)
                    || HTML.Tag.DT.equals(tag)
                    ) {
                return new ParagraphView(elem);

            } else if (HTML.Tag.MENU.equals(tag)
                    || HTML.Tag.DIR.equals(tag)
                    || HTML.Tag.UL.equals(tag)
                    || HTML.Tag.OL.equals(tag)) {
                return new ListView(elem);

            } else if (HTML.Tag.LI.equals(tag)
                    || HTML.Tag.DL.equals(tag)
                    || HTML.Tag.DD.equals(tag)
                    || HTML.Tag.BODY.equals(tag)
                    || HTML.Tag.HTML.equals(tag)
                    || HTML.Tag.CENTER.equals(tag)
                    || HTML.Tag.DIV.equals(tag)
                    || HTML.Tag.BLOCKQUOTE.equals(tag)
                    || HTML.Tag.PRE.equals(tag)) {
                return new BlockView(elem, View.Y_AXIS);

            } else if (HTML.Tag.IMG.equals(tag)) {
                return new ImageView(elem);

            } else if (HTML.Tag.HR.equals(tag)) {
                return new HRuleTagView(elem);

            } else if (HTML.Tag.BR.equals(tag)) {
                return new BRView(elem);

            } else if (HTML.Tag.TABLE.equals(tag)) {
                return new TableTagView(elem);

            } else if (HTML.Tag.FORM.equals(tag)) {
                return new BlockView(elem, View.X_AXIS);

            } else if (HTML.Tag.INPUT.equals(tag)) {
                return new FormView(elem);

            } else if (HTML.Tag.SELECT.equals(tag)
                    || HTML.Tag.TEXTAREA.equals(tag)) {
                return new FormView(elem);

            } else if (HTML.Tag.OBJECT.equals(tag)) {
                return new ObjectView(elem);

            } else if (HTML.Tag.FRAMESET.equals(tag)) {
                return new FrameSetTagView(elem);

            } else if (HTML.Tag.FRAME.equals(tag)) {
                return new FrameTagView(elem);

            } else if (HTML.Tag.NOFRAMES.equals(tag)) {
                return new NoFramesTagView(elem);

            } else if (HTML.Tag.HEAD.equals(tag)) {
                return new HeadTagView(elem);
            }


            // HARMONY-4570
            // We should not throw exception on uknown tag
            return new InlineView(elem);
        }
    }

    public abstract static class HTMLTextAction extends StyledTextAction {
        public HTMLTextAction(final String name) {
            super(name);
        }

        protected int elementCountToTag(final HTMLDocument doc,
                                        final int offset,
                                        final HTML.Tag tag) {
            int count = -1;
            Element e;
            for (e = doc.getCharacterElement(offset);
                 e != null && !tag.equals(getHTMLTagByElement(e));
                 e = e.getParentElement()) {
                count++;
            }
            if (e == null) {
                return -1;
            }
            return count;
        }

        protected Element findElementMatchingTag(final HTMLDocument doc,
                                                 final int offset,
                                                 final HTML.Tag tag) {
            Element e = doc.getCharacterElement(offset);
            while (e != null && !tag.equals(getHTMLTagByElement(e))) {
                e = e.getParentElement();
            }
            return e;
        }

        protected Element[] getElementsAt(final HTMLDocument doc,
                                          final int offset) {
            ArrayList list = new ArrayList();
            Element e = doc.getDefaultRootElement();
            while (true) {
                list.add(e);
                if (e.getElementCount() == 0) {
                    break;
                }
                e = e.getElement(e.getElementIndex(offset));
            }

            return (Element[])list.toArray(new Element[list.size()]);
        }

        protected HTMLDocument getHTMLDocument(final JEditorPane pane) {
            Document doc = pane.getDocument();
            if (doc instanceof HTMLDocument) {
                return (HTMLDocument)doc;
            }
            throw new IllegalArgumentException(Messages.getString("swing.A0")); //$NON-NLS-1$
        }

        protected HTMLEditorKit getHTMLEditorKit(final JEditorPane pane) {
            EditorKit editorKit = pane.getEditorKit();
            if (editorKit instanceof HTMLEditorKit) {
                return (HTMLEditorKit)editorKit;
            }
            throw new IllegalArgumentException(Messages.getString("swing.A1")); //$NON-NLS-1$
        }
    }

    public static class InsertHTMLTextAction extends HTMLTextAction {
        protected HTML.Tag addTag;
        protected HTML.Tag alternateAddTag;
        protected HTML.Tag alternateParentTag;
        protected String html;
        protected HTML.Tag parentTag;

        public InsertHTMLTextAction(final String name, final String html,
                                    final HTML.Tag parentTag,
                                    final HTML.Tag addTag,
                                    final HTML.Tag alternateParentTag,
                                    final HTML.Tag alternateAddTag) {
            super(name);
            this.html = html;
            this.parentTag = parentTag;
            this.addTag = addTag;
            this.alternateParentTag = alternateParentTag;
            this.alternateAddTag = alternateAddTag;
        }

        public InsertHTMLTextAction(final String name, final String html,
                                    final HTML.Tag parentTag,
                                    final HTML.Tag addTag) {
            super(name);
            this.html = html;
            this.parentTag = parentTag;
            this.addTag = addTag;
        }

        public void actionPerformed(final ActionEvent event) {
            if (event == null) {
                return;
            }

            JEditorPane editor = getEditor(event);
            HTMLDocument doc = getHTMLDocument(editor);
            int offset = editor.getCaretPosition();

            HTML.Tag usedParentTag = parentTag;
            HTML.Tag usedAddTag = addTag;
            int popDepth = elementCountToTag(doc, offset, parentTag);
            if (popDepth == -1 && alternateParentTag != null) {
                usedParentTag = alternateParentTag;
                usedAddTag = alternateAddTag;
                popDepth = elementCountToTag(doc, offset, alternateParentTag);
            }
            if (popDepth == -1) {
                return;
            }

            Element insertElement = findElementMatchingTag(doc, offset,
                                                           usedParentTag);
            if (insertElement.getStartOffset() == offset) {
                insertAtBoundary(editor, doc, offset, insertElement, html,
                                 usedParentTag, usedAddTag);
            } else {
                int pushDepth = 0;
                insertHTML(editor, doc, offset, html,
                           popDepth, pushDepth, usedAddTag);
            }
        }

        protected void insertHTML(final JEditorPane editor,
                                  final HTMLDocument doc, final int offset,
                                  final String html, final int popDepth,
                                  final int pushDepth, final HTML.Tag addTag) {
            HTMLEditorKit editorKit = getHTMLEditorKit(editor);
            try {
                editorKit.insertHTML(doc, offset, html,
                                     popDepth, pushDepth, addTag);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        protected void insertAtBoundary(final JEditorPane editor,
                                        final HTMLDocument doc,
                                        final int offset,
                                        final Element insertElement,
                                        final String html,
                                        final HTML.Tag parentTag,
                                        final HTML.Tag addTag) {
            insertAtBoundaryImpl(editor, doc, offset, insertElement, html,
                                 parentTag, addTag);
        }

        /**
         * @deprecated
         */
        protected void insertAtBoundry(final JEditorPane editor,
                                       final HTMLDocument doc,
                                       final int offset,
                                       final Element insertElement,
                                       final String html,
                                       final HTML.Tag parentTag,
                                       final HTML.Tag addTag) {
            insertAtBoundaryImpl(editor, doc, offset, insertElement, html,
                                 parentTag, addTag);
        }

        private void insertAtBoundaryImpl(final JEditorPane editor,
                                          final HTMLDocument doc,
                                          final int offset,
                                          final Element insertElement,
                                          final String html,
                                          final HTML.Tag parentTag,
                                          final HTML.Tag addTag) {
            int popDepth = elementCountToTag(doc, offset, parentTag) + 1;
            int pushDepth = 1;
            insertHTML(editor, doc, offset, html, popDepth, pushDepth, addTag);
        }
    }

    public static class LinkController extends MouseAdapter
            implements MouseMotionListener, Serializable {

        private Element prevLinkUnderMouse;

        public void mouseClicked(final MouseEvent e) {
            JEditorPane pane = (JEditorPane)e.getSource();
            if (pane.isEditable()) {
                return;
            }

            int pos = pane.viewToModel(e.getPoint());
            activateLink(pos, pane);
        }

        public void mouseDragged(final MouseEvent e) {
            // does nothing
        }

        public void mouseMoved(final MouseEvent e) {
            JEditorPane pane = (JEditorPane)e.getSource();
            Element linkElement = getLinkElement(pane, e.getPoint());
            updateMouseCursor(e, linkElement);

            if (pane.isEditable()) {
                return;
            }

            fireHyperlinkEvent(e, linkElement);
        }

        protected void activateLink(final int pos, final JEditorPane editor) {
            Element elem = HTMLEditorKit.getLinkElement(editor, pos);
            if (elem != null) {
                HTMLEditorKit.fireHyperlinkEvent(editor,
                        HyperlinkEvent.EventType.ACTIVATED, elem);
            }
        }

        private void updateMouseCursor(final MouseEvent e,
                                       final Element linkElement) {
            JEditorPane pane = (JEditorPane)e.getSource();
            HTMLEditorKit editorKit = (HTMLEditorKit)pane.getEditorKit();
            if (!pane.isEditable() && linkElement != null) {
                e.getComponent().setCursor(editorKit.getLinkCursor());
            } else {
                e.getComponent().setCursor(editorKit.getDefaultCursor());
            }
        }

        private void fireHyperlinkEvent(final MouseEvent e,
                                        final Element linkElement) {
            if (prevLinkUnderMouse == linkElement) {
                return;
            }

            JEditorPane pane = (JEditorPane)e.getSource();
            if (prevLinkUnderMouse != null) {
                HTMLEditorKit.fireHyperlinkEvent(pane,
                        HyperlinkEvent.EventType.EXITED, prevLinkUnderMouse);
            }
            if (linkElement != null) {
                HTMLEditorKit.fireHyperlinkEvent(pane,
                        HyperlinkEvent.EventType.ENTERED, linkElement);
            }
            prevLinkUnderMouse = linkElement;
        }

        private Element getLinkElement(final JEditorPane pane, final Point p) {
            return HTMLEditorKit.getLinkElement(pane, pane.viewToModel(p));
        }
    }

    public abstract static class Parser {
        public abstract void parse(final Reader r, final ParserCallback cb,
                                   final boolean ignoreCharSet)
                throws IOException;
    }

    public static class ParserCallback {
        public static final Object IMPLIED = "_implied_";

        public void flush() throws BadLocationException {
        }

        public void handleComment(final char[] data, final int pos) {
        }

        public void handleEndOfLineString(final String eol) {
        }

        public void handleEndTag(final HTML.Tag tag, final int pos) {
        }

        public void handleError(final String errorMsg, final int pos) {
        }

        public void handleSimpleTag(final HTML.Tag tag,
                                    final MutableAttributeSet attr,
                                    final int pos) {
        }

        public void handleStartTag(final HTML.Tag tag,
                                   final MutableAttributeSet attr,
                                   final int pos) {
        }

        public void handleText(final char[] data, final int pos) {
        }
    }

    private static class NavigateLinkAction extends HTMLTextAction {
        private static final HashMap highlightTags = new HashMap();

        public NavigateLinkAction(final String name) {
            super(name);
        }

        public void actionPerformed(final ActionEvent e) {
            if (getEditor(e).isEditable()) {
                return;
            }

            JEditorPane editor = getEditor(e);
            Caret caret = editor.getCaret();
            HTMLDocument doc = getHTMLDocument(editor);

            Element link = getNextLinkElement(doc, caret.getDot(), isForward());
            if (link != null) {
                moveHighlight(editor,
                              link.getStartOffset(), link.getEndOffset());
            }
        }

        private boolean isForward() {
            return "next-link-action".equals(getValue(NAME));
        }

        private static Element getNextLinkElement(final HTMLDocument doc,
                                                  final int pos,
                                                  final boolean forward) {
            HTMLDocument.Iterator it = doc.getIterator(HTML.Tag.A);
            int linkPos = -1;
            for (; it.isValid(); it.next()) {
                if (forward) {
                    if (it.getStartOffset() > pos) {
                        linkPos = it.getStartOffset();
                        break;
                    }
                } else {
                    if (it.getStartOffset() >= pos) {
                        break;
                    }
                    linkPos = it.getStartOffset();
                }
            }

            return linkPos != -1 ? doc.getCharacterElement(linkPos) : null;
        }

        private static void moveHighlight(final JEditorPane editor,
                                          final int start, final int end) {
            Highlighter highlighter = editor.getHighlighter();
            Object tag = highlightTags.get(highlighter);
            if (tag != null) {
                highlighter.removeHighlight(tag);
                highlightTags.remove(highlighter);
            }
            try {
                tag = highlighter.addHighlight(start, end,
                                               new LinkHighlightPainter());
                highlightTags.put(highlighter, tag);
                editor.getCaret().setDot(start);
            } catch (final BadLocationException e) {
            }
        }

        static void removeHighlight(final JEditorPane editor) {
            Highlighter highlighter = editor.getHighlighter();
            Object tag = highlightTags.get(highlighter);
            if (tag != null) {
                highlighter.removeHighlight(tag);
                highlightTags.remove(highlighter);
            }
        }
    }

    private static class ActivateLinkAction extends HTMLTextAction {
        public ActivateLinkAction() {
            super("activate-link-action");
        }

        public void actionPerformed(final ActionEvent e) {
            JEditorPane editor = getEditor(e);
            if (!editor.isEditable()) {
                activateLink(editor.getCaretPosition(), editor);
            }
        }
    }

    private static class LinkHighlightPainter
            extends DefaultHighlighter.DefaultHighlightPainter {
        public LinkHighlightPainter() {
            super(Color.RED);
        }

        public Shape paintLayer(final Graphics g, final int p0, final int p1,
                                final Shape shape, final JTextComponent jtc,
                                final View view) {
            return TextUtils.paintLayer(g, p0, p1, shape,
                                        jtc.getSelectionColor(), view, false);
        }
    }

    private static class HeadTagView extends View {
        public HeadTagView(final Element elem) {
            super(elem);
        }

        public float getPreferredSpan(final int axis) {
            return 0.0f;
        }

        public int viewToModel(final float x, final float y,
                               final Shape a, final Bias[] biasRet) {
            return 0;
        }

        public void paint(final Graphics g, final Shape allocation) {
        }

        public Shape modelToView(final int pos, final Shape a,
                                 final Bias b) throws BadLocationException {
            return new Rectangle(0, 0);
        }

        public int getNextVisualPositionFrom(final int pos, final Bias b,
                final Shape a, final int direction,
                final Bias[] biasRet) throws BadLocationException {
            if (direction != NORTH && direction != SOUTH
                    && direction != EAST && direction != WEST) {
                throw new IllegalArgumentException(Messages.getString("swing.84")); //$NON-NLS-1$
            }
            biasRet[0] = Position.Bias.Forward;
            return getEndOffset();
        }
    }

    public static final String BOLD_ACTION = "html-bold-action";
    public static final String COLOR_ACTION = "html-color-action";
    public static final String FONT_CHANGE_BIGGER = "html-font-bigger";
    public static final String FONT_CHANGE_SMALLER = "html-font-smaller";
    public static final String IMG_ALIGN_BOTTOM = "html-image-align-bottom";
    public static final String IMG_ALIGN_MIDDLE = "html-image-align-middle";
    public static final String IMG_ALIGN_TOP = "html-image-align-top";
    public static final String IMG_BORDER = "html-image-border";
    public static final String ITALIC_ACTION = "html-italic-action";
    public static final String LOGICAL_STYLE_ACTION = "html-logical-style-action";
    public static final String PARA_INDENT_LEFT = "html-para-indent-left";
    public static final String PARA_INDENT_RIGHT = "html-para-indent-right";

    public static final String DEFAULT_CSS = "default.css";

    private static StyleSheet styleSheet;
    private static Parser parser;
    private static ViewFactory viewFactory;
    private static final LinkController linkController = new LinkController();
    private static Action[] actions;

    private Cursor defaultCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private Cursor linkCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private boolean autoFormSubmission = true;

    public HTMLEditorKit() {
        if (actions == null) {
            createStaticActions();
        }
    }

    public Object clone() {
        return super.clone();
    }

    public Document createDefaultDocument() {
        HTMLDocument document = new HTMLDocument();
        document.getStyleSheet().addStyleSheet(getStyleSheet());
        document.setParser(getParser());
        document.setAsynchronousLoadPriority(4);
        return document;
    }

    public AccessibleContext getAccessibleContext() {
        // TODO: implement
        throw new UnsupportedOperationException(Messages.getString("swing.9F")); //$NON-NLS-1$
    }

    public Action[] getActions() {
        return (Action[])actions.clone();
    }

    public String getContentType() {
        return "text/html";
    }

    public MutableAttributeSet getInputAttributes() {
        return super.getInputAttributes();
    }

    public ViewFactory getViewFactory() {
        if (viewFactory == null) {
            viewFactory = new HTMLFactory();
        }
        return viewFactory;
    }

    public void install(final JEditorPane pane) {
        super.install(pane);

        pane.addMouseListener(linkController);
        pane.addMouseMotionListener(linkController);
    }

    public void deinstall(final JEditorPane pane) {
        pane.removeMouseListener(linkController);
        pane.removeMouseMotionListener(linkController);
        NavigateLinkAction.removeHighlight(pane);

        super.deinstall(pane);
    }

    public void setAutoFormSubmission(final boolean auto) {
        autoFormSubmission = auto;
    }

    public boolean isAutoFormSubmission() {
        return autoFormSubmission;
    }

    public void setDefaultCursor(final Cursor cursor) {
        defaultCursor = cursor;
    }

    public Cursor getDefaultCursor() {
        return defaultCursor;
    }

    public void setLinkCursor(final Cursor cursor) {
        linkCursor = cursor;
    }

    public Cursor getLinkCursor() {
        return linkCursor;
    }

    public void setStyleSheet(final StyleSheet ss) {
        styleSheet = ss;
    }

    public StyleSheet getStyleSheet() {
        if (styleSheet == null) {
            styleSheet = new StyleSheet();
            URL url = HTMLEditorKit.class.getResource(DEFAULT_CSS);
            try {
                styleSheet.loadRules(new BufferedReader(
                    new InputStreamReader(url.openStream())), url);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        return styleSheet;
    }

    public void insertHTML(final HTMLDocument doc, final int offset,
                           final String html, final int popDepth,
                           final int pushDepth, final HTML.Tag insertTag)
            throws BadLocationException, IOException {
        if (offset > doc.getLength()) {
            throw new BadLocationException(Messages.getString("swing.98"), offset); //$NON-NLS-1$
        }

        ParserCallback htmlReader = doc.getReader(offset, popDepth,
                                                  pushDepth, insertTag);
        StringReader in = new StringReader(html);

        getParser().parse(in, htmlReader, false);
        htmlReader.flush();
        in.close();
    }

    public void read(final Reader in, final Document doc, final int pos)
            throws IOException, BadLocationException {
        if (!(doc instanceof HTMLDocument)) {
            super.read(in, doc, pos);
            return;
        }

        HTMLDocument htmlDoc = (HTMLDocument)doc;
        checkReadPosition(htmlDoc, pos);

        ParserCallback htmlReader = htmlDoc.getReader(pos);
        Object property = doc.getProperty(StringConstants
                                          .IGNORE_CHARSET_DIRECTIVE);
        getParser().parse(in, htmlReader, property == null ? false
                          : ((Boolean)property).booleanValue());
        htmlReader.flush();
        in.close();
    }

    public void write(final Writer out, final Document doc, final int pos,
                      final int len) throws IOException, BadLocationException {
        HTMLDocument htmlDoc;
        int fixedPos = pos;

        if (doc instanceof HTMLDocument) {
            htmlDoc = (HTMLDocument)doc;
        } else {
            htmlDoc = (HTMLDocument)createDefaultDocument();
            htmlDoc.insertString(0, doc.getText(pos, len), null);
            fixedPos = 0;
        }

        HTMLWriter writer = new HTMLWriter(out, htmlDoc, fixedPos, len);
        writer.write();
    }

    protected Parser getParser() {
        if (parser == null) {
            parser = new ParserDelegator();
        }

        return parser;
    }

    protected void createInputAttributes(final Element elem,
                                         final MutableAttributeSet set) {
        super.createInputAttributes(elem, set);
    }

    static HTML.Tag getHTMLTagByElement(final Element elem) {
        final Object result = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
        return (result instanceof HTML.Tag) ? (HTML.Tag)result : null;
    }

    private static String getURLString(final Element e) {
        AttributeSet aSet = (AttributeSet)e.getAttributes()
                .getAttribute(HTML.Tag.A);
        return aSet == null
                ? null
                : (String)aSet.getAttribute(HTML.Attribute.HREF);
    }

    private static void checkReadPosition(final HTMLDocument doc, final int pos)
            throws BadLocationException {
        if (pos < 0) {
            throw new RuntimeException(Messages.getString("swing.A2")); //$NON-NLS-1$
        }
        if (pos > doc.getLength()) {
            throw new BadLocationException(Messages.getString("swing.98"), pos); //$NON-NLS-1$
        }
        if (doc.getLength() != 0) {
            Element body = doc.getElement(doc.getDefaultRootElement(),
                                          StyleConstants.NameAttribute,
                                          HTML.Tag.BODY);
            if (pos < body.getStartOffset() || pos > body.getEndOffset()) {
                throw new RuntimeException(Messages.getString("swing.A3")); //$NON-NLS-1$
            }
        }
    }

    private Action[] createStaticActions() {
        if (actions == null) {
            Action[] htmlActions = getDefaultActions();
            Action[] styledActions = super.getActions();
            actions = new Action[htmlActions.length + styledActions.length];
            System.arraycopy(styledActions, 0, actions, 0, styledActions.length);
            System.arraycopy(htmlActions, 0, actions, styledActions.length,
                             htmlActions.length);
        }
        return actions;
    }

    private Action[] getDefaultActions() {
        return new Action[] {
            new InsertHTMLTextAction("InsertOrderedList", "<ol><li></li></ol>",
                HTML.Tag.BODY, HTML.Tag.OL),
            new InsertHTMLTextAction("InsertOrderedListItem", "<ol><li></li></ol>",
                HTML.Tag.OL, HTML.Tag.LI, HTML.Tag.BODY, HTML.Tag.OL),
            new InsertHTMLTextAction("InsertUnorderedList", "<ul><li></li></ul>",
                HTML.Tag.BODY, HTML.Tag.UL),
            new InsertHTMLTextAction("InsertUnorderedListItem", "<ul><li></li></ul>",
                HTML.Tag.UL, HTML.Tag.LI, HTML.Tag.BODY, HTML.Tag.UL),
            new InsertHTMLTextAction("InsertTable",
                                     "<table border=1><tr><td></td></tr></table>",
                                     HTML.Tag.BODY, HTML.Tag.TABLE),
            new InsertHTMLTextAction("InsertTableDataCell",
                                     "<table border=1><tr><td></td></tr></table>",
                                     HTML.Tag.TR, HTML.Tag.TD,
                                     HTML.Tag.BODY, HTML.Tag.TABLE),
            new InsertHTMLTextAction("InsertTableRow",
                                     "<table border=1><tr><td></td></tr></table>",
                                     HTML.Tag.TABLE, HTML.Tag.TR,
                                     HTML.Tag.BODY, HTML.Tag.TABLE),
            new InsertHTMLTextAction("InsertPre", "<pre></pre>",
                                     HTML.Tag.BODY, HTML.Tag.PRE),
            new InsertHTMLTextAction("InsertHR", "<hr>", HTML.Tag.P, HTML.Tag.HR),
            new NavigateLinkAction("next-link-action"),
            new NavigateLinkAction("previous-link-action"),
            new ActivateLinkAction()
        };
    }

    private static void activateLink(final int pos, final JEditorPane editor) {
        Element elem = getLinkElement(editor, pos);
        if (elem != null) {
            fireHyperlinkEvent(editor, HyperlinkEvent.EventType.ACTIVATED,
                               elem);
        }
    }

    private static void fireHyperlinkEvent(final JEditorPane pane,
                                           final HyperlinkEvent.EventType eventID,
                                           final Element elem) {
        String urlString = getURLString(elem);

        URL base = ((HTMLDocument)pane.getDocument()).getBase();
        HyperlinkEvent event = new HyperlinkEvent(pane, eventID,
                                                  HTML.resolveURL(urlString,
                                                                  base),
                                                  urlString, elem);
        pane.fireHyperlinkUpdate(event);
    }

    private static Element getLinkElement(final JEditorPane pane, final int pos) {
        Element e = (((HTMLDocument)pane.getDocument()).getCharacterElement(pos));
        for (; e != null; e = e.getParentElement()) {
            if (getURLString(e) != null) {
                return e;
            }
        }

        return null;
    }
}
