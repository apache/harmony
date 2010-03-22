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
package javax.swing;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Hashtable;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleHyperlink;
import javax.accessibility.AccessibleHypertext;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleText;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.ChangedCharSetException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.WrappedPlainView;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.harmony.luni.util.NotImplementedException;
import org.apache.harmony.x.swing.StringConstants;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <p>
 * <i>JEditorPane</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JEditorPane extends JTextComponent {
    private static final long serialVersionUID = -767121239635831550L;

    protected class AccessibleJEditorPane extends JTextComponent.AccessibleJTextComponent {
        private static final long serialVersionUID = -6869835326921704467L;

        @Override
        public String getAccessibleDescription() {
            return getContentType();
        }

        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet set = super.getAccessibleStateSet();
            set.add(AccessibleState.MULTI_LINE);
            return set;
        }
    }

    protected class AccessibleJEditorPaneHTML extends AccessibleJEditorPane {
        private static final long serialVersionUID = -5072331196784098614L;

        AccessibleText text;

        @Override
        public AccessibleText getAccessibleText() {
            if (text == null) {
                text = new JEditorPaneAccessibleHypertextSupport();
            }
            return text;
        }
    }

    protected class JEditorPaneAccessibleHypertextSupport extends AccessibleJEditorPane
            implements AccessibleHypertext {
        private static final long serialVersionUID = -1462897229238717575L;

        //Not implemented
        public class HTMLLink extends AccessibleHyperlink {
            public HTMLLink(Element e) throws NotImplementedException {
                super();
                throw new NotImplementedException();
            }

            @Override
            public boolean doAccessibleAction(int i) throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public Object getAccessibleActionAnchor(int i) throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public int getAccessibleActionCount() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public String getAccessibleActionDescription(int i) throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public Object getAccessibleActionObject(int i) throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public int getEndIndex() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public int getStartIndex() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public boolean isValid() throws NotImplementedException {
                throw new NotImplementedException();
            }
        }

        public JEditorPaneAccessibleHypertextSupport() throws NotImplementedException {
            super();
            throw new NotImplementedException();
        }

        public AccessibleHyperlink getLink(int linkIndex) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getLinkCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getLinkIndex(int charIndex) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public String getLinkText(int linkIndex) throws NotImplementedException {
            throw new NotImplementedException();
        }
    }

    public static final String HONOR_DISPLAY_PROPERTIES = "JEditorPane.honorDisplayProperties";

    public static final String W3C_LENGTH_UNITS = "JEditorPane.w3cLengthUnits";

    private static final String uiClassID = "EditorPaneUI";

    static final class PlainEditorKit extends DefaultEditorKit implements ViewFactory {
        private static final long serialVersionUID = 1L;

        public View create(Element elem) {
            return new WrappedPlainView(elem);
        }

        @Override
        public ViewFactory getViewFactory() {
            return this;
        }
    }

    private static final String PLAIN_CONTENT_TYPE = "text/plain";

    private static final String HTML_CONTENT_TYPE = "text/html";

    private static final String RTF_CONTENT_TYPE = "text/rtf";

    private static final String RTF2_CONTENT_TYPE = "application/rtf";

    private static final String REFERENCE_TAIL_PATTERN = "#.*";

    private static final String RTF_HEADER = "{\\rtf";

    private static final String HTML_HEADER = "<html";

    private static Map<String, ContentTypeRegistration> contentTypes =
            new Hashtable<String, ContentTypeRegistration>();

    private static Map<String, EditorKit> localContentTypes =
            new Hashtable<String, EditorKit>();

    private EditorKit editorKit;

    private URL currentPage;

    private AccessibleContext accessible;

    private AccessibleContext accessibleHTML;

    static {
        contentTypes.put(PLAIN_CONTENT_TYPE,
                new ContentTypeRegistration("javax.swing.JEditorPane$PlainEditorKit", null));
        contentTypes.put(HTML_CONTENT_TYPE,
                new ContentTypeRegistration("javax.swing.text.html.HTMLEditorKit", null));
        contentTypes.put(RTF_CONTENT_TYPE,
                new ContentTypeRegistration("javax.swing.text.rtf.RTFEditorKit", null));
        contentTypes.put(RTF2_CONTENT_TYPE,
                new ContentTypeRegistration("javax.swing.text.rtf.RTFEditorKit", null));
    }

    public static EditorKit createEditorKitForContentType(final String contentType) {
        ContentTypeRegistration registration = contentTypes.get(contentType);

        if (registration != null) {
            try {
                if (registration.editorKit == null) {
                    registration.editorKit = (EditorKit)
                            Class.forName(registration.className, true,
                                    registration.classLoader).newInstance();
                }
                return (EditorKit) registration.editorKit.clone();
            } catch (Throwable e) {
                // Ignore.

                /*
                 * This is rather dangerous, but is being done so for
                 * compatibility with RI that seems to do the same.
                 * See HARMONY-3454 for details.
                 * This could be tweaked in the future and changed to
                 * only catch Exception and LinkageError, for example.
                 */
            }
        }
        return null;
    }

    public static String getEditorKitClassNameForContentType(final String type) {
        if (type == null) {
            throw new NullPointerException(Messages.getString("swing.03","Content type")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        ContentTypeRegistration registration = contentTypes.get(type);

        return ((registration != null) ? registration.className : null);
    }

    public static void registerEditorKitForContentType(final String type,
            final String editorKitName) {
        registerEditorKitForContentType(type, editorKitName, null);
    }

    public static void registerEditorKitForContentType(final String type,
            final String editorKitName, final ClassLoader loader) {
        if (type == null) {
            throw new NullPointerException(Messages.getString("swing.03","Content type")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (editorKitName == null) {
            throw new NullPointerException(Messages.getString("swing.03","Class name")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        contentTypes.put(type, new ContentTypeRegistration(
                editorKitName, ((loader != null) ? loader
                        : Thread.currentThread().getContextClassLoader())));
    }

    public JEditorPane() {
        setFocusCycleRoot(true);
    }

    public JEditorPane(final String page) throws IOException {
        this();
        setPage(page);
    }

    public JEditorPane(final String type, final String text) {
        this();

        if (type == null) {
            throw new NullPointerException(Messages.getString("swing.03","Content type")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        setContentType(type);
        setText(text);
    }

    public JEditorPane(final URL page) throws IOException {
        this();
        setPage(page);
    }

    public synchronized void addHyperlinkListener(final HyperlinkListener listener) {
        listenerList.add(HyperlinkListener.class, listener);
    }

    protected EditorKit createDefaultEditorKit() {
        return new PlainEditorKit();
    }

    public void fireHyperlinkUpdate(final HyperlinkEvent event) {
        HyperlinkListener[] listeners = getHyperlinkListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].hyperlinkUpdate(event);
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (HTML_CONTENT_TYPE.equals(getContentType())) {
            if (accessibleHTML == null) {
                accessibleHTML = new AccessibleJEditorPaneHTML();
            }
            return accessibleHTML;
        }
        if (accessible == null) {
            accessible = new AccessibleJEditorPane();
        }
        return accessible;
    }

    public final String getContentType() {
        return ((editorKit != null) ? editorKit.getContentType() : null);
    }

    public EditorKit getEditorKit() {
        if (editorKit == null) {
            editorKit = createDefaultEditorKit();
        }
        return editorKit;
    }

    public EditorKit getEditorKitForContentType(final String type) {
        EditorKit kit = localContentTypes.get(type);

        if (kit == null) {
            kit = createEditorKitForContentType(type);

            if (kit == null) {
                kit = createDefaultEditorKit();
            }
        }
        return kit;
    }

    public synchronized HyperlinkListener[] getHyperlinkListeners() {
        return getListeners(HyperlinkListener.class);
    }

    public URL getPage() {
        return currentPage;
    }

    @Override
    public Dimension getPreferredSize() {
        getUI().getRootView(this).setSize(0,0);
        Dimension d = super.getPreferredSize();
        Container parent = getParent();
        if (parent instanceof JViewport) {
            Dimension min = getMinimumSize();
            if (!getScrollableTracksViewportWidth()) {
                int width = parent.getWidth();
                if (width < min.width) {
                    d.width = min.width;
                }
            }
            if (!getScrollableTracksViewportHeight()) {
                int height = parent.getHeight();
                if (height < min.height) {
                    d.height = min.height;
                }
            }
        }
        return d;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        Container parent = getParent();
        if (parent instanceof JViewport) {
            int height = parent.getHeight();
            Dimension min = getMinimumSize();
            Dimension max = getMaximumSize();
            return height >= min.height && height <= max.height;
        }
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        Container parent = getParent();
        if (parent instanceof JViewport) {
            int width = parent.getWidth();
            Dimension min = getMinimumSize();
            Dimension max = getMaximumSize();
            return width >= min.width && width <= max.width;
        }
        return false;
    }

    private String getBaseURL(final String url) {
        return (url == null) ? null : url.replaceAll(REFERENCE_TAIL_PATTERN, "");
    }

    protected InputStream getStream(final URL url) throws IOException {
        if (url.getProtocol() == "http") {
            getDocument().putProperty(Document.StreamDescriptionProperty,
                    getBaseURL(url.toString()));
        }
        URLConnection connection = url.openConnection();
        String contentType = connection.getContentType();
        setContentType((contentType != null) ? contentType : PLAIN_CONTENT_TYPE);
        return connection.getInputStream();
    }

    @Override
    public String getText() {
        StringWriter writer = new StringWriter();
        try {
            super.write(writer);
        } catch (IOException e) {
        }
        return writer.toString();
    }

    @Override
    public String getUIClassID() {
        return uiClassID;
    }

    @Override
    protected String paramString() {
        return (super.paramString() + "," + "contentType=" + getContentType() + ","
                + "editorKit=" + editorKit + "," + "document=" + getDocument() + ","
                + "currentPage=" + currentPage);
    }

    public void read(final InputStream stream, final Object type) throws IOException {
        if (type instanceof String) {
            setContentType((String) type);
        }
        try {
            Document doc = getDocument();
            doc.putProperty(StringConstants.IGNORE_CHARSET_DIRECTIVE, Boolean.TRUE);
            editorKit.read(new InputStreamReader(stream), doc, 0);
        } catch (BadLocationException e) {
        }
    }

    public synchronized void removeHyperlinkListener(final HyperlinkListener listener) {
        listenerList.remove(HyperlinkListener.class, listener);
    }

    @Override
    public synchronized void replaceSelection(final String s) {
        if (!isEditable()) {
            new DefaultEditorKit.BeepAction().actionPerformed(null);
            return;
        }
        int start = getSelectionStart();
        int end = getSelectionEnd();
        Document doc = getDocument();
        try {
            if (start != end) {
                doc.remove(start, end - start);
            }
            //May be these attributes placed in Document ????
            AttributeSet as = (editorKit instanceof StyledEditorKit) ? ((StyledEditorKit) editorKit)
                    .getInputAttributes()
                    : null;
            if (s != null) {
                doc.insertString(start, s, as);
            }
        } catch (BadLocationException e) {
        }
    }

    public void scrollToReference(final String ref) {
         Document doc = getDocument();
        if (ref == null || !(doc instanceof HTMLDocument)) {
            return;
        }
        HTMLDocument.Iterator it = ((HTMLDocument)doc).getIterator(HTML.Tag.A);
        int offset = 0;
        while (it.isValid()) {
            AttributeSet set = it.getAttributes();
            Object name = set.getAttribute(HTML.Attribute.NAME);
            if (ref.equals(name)) {
                offset = it.getStartOffset();
                break;
            }
            it.next();
        }
        Rectangle rect = null;
        try {
            rect = modelToView(offset);
        } catch (BadLocationException e) {
        }
        Rectangle visibleRect = getVisibleRect();
        if (visibleRect != null) {
            rect.height = visibleRect.height;
        }
        scrollRectToVisible(rect);
    }

    private boolean changeEditorKit(final String contentType) {
        return !(/*(RTF_CONTENT_TYPE.equals(contentType) && editorKit instanceof RTFEditorKit)
                 ||*/ (HTML_CONTENT_TYPE.equals(contentType) && editorKit instanceof HTMLEditorKit)
                 || (PLAIN_CONTENT_TYPE.equals(contentType) && editorKit instanceof PlainEditorKit));
    }

    public final void setContentType(String type) {
        if (type == null) {
            throw new NullPointerException(Messages.getString("swing.03","Content type")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        int comma = type.indexOf(';');

        if (comma >= 0) {
            type = type.substring(0, comma);
        }
        type = type.trim().toLowerCase();

        if (!contentTypes.containsKey(type)) {
            type = PLAIN_CONTENT_TYPE;
        }

        if (changeEditorKit(type)) {
            EditorKit kit = getEditorKitForContentType(type);
            updateEditorKit((kit != null) ? kit : new PlainEditorKit());
            updateDocument(editorKit);
        }
    }

    private void updateEditorKit(final EditorKit kit) {
        if (editorKit != null) {
            editorKit.deinstall(this);
        }
        EditorKit oldEditorKit = editorKit;
        if (kit != null) {
            kit.install(this);
        }
        editorKit = kit;
        firePropertyChange("editorKit", oldEditorKit, kit);
    }

    private void updateDocument(final EditorKit kit) {
        if (kit != null) {
            setDocument(kit.createDefaultDocument());
        }
    }

    public void setEditorKit(final EditorKit kit) {
        updateEditorKit(kit);
        updateDocument(kit);
    }

    public void setEditorKitForContentType(final String type, final EditorKit kit) {
        if (type == null) {
            throw new NullPointerException(Messages.getString("swing.03","Content type")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (kit == null) {
            throw new NullPointerException(Messages.getString("swing.03","Editor kit")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        localContentTypes.put(type, kit);
    }

    public void setPage(final String page) throws IOException {
        setPage(new URL(page));
    }

    private void documentLoading(final InputStream str, final Document doc, final URL url)
            throws IOException {
        try {
            editorKit.read(str, doc, 0);
        } catch (ChangedCharSetException e) {
            try {
                doc.putProperty(StringConstants.IGNORE_CHARSET_DIRECTIVE, Boolean.TRUE);
                doc.remove(0, doc.getLength());
                final String htmlAttribute = e.getCharSetSpec();
                final int charSetIndex = htmlAttribute.lastIndexOf("charset=");
                if (charSetIndex >= 0) {
                    String charSet = htmlAttribute.substring(charSetIndex + 8);
                    InputStreamReader reader = new InputStreamReader(url.openStream(), Charset
                            .forName(charSet));
                    editorKit.read(reader, doc, 0);
                }
            } catch (BadLocationException e1) {
            }
        } catch (BadLocationException e) {
        }
    }

    private static class ContentTypeRegistration {
        String className;
        ClassLoader classLoader;
        EditorKit editorKit;

        ContentTypeRegistration(String className, ClassLoader classLoader) {
            this.className = className;
            this.classLoader = classLoader;
        }
    }

    private class AsynchLoad extends Thread {
        InputStream inputStream;
        boolean successfulLoading = true;
        URL url;

        public AsynchLoad(final int priority, final InputStream stream,
                          final URL url) {
           super();
           setPriority(priority);
           inputStream = stream;
           this.url = url;
        }

        @Override
        public void run() {
            try {
                documentLoading(inputStream, getDocument(), url);
            } catch (IOException e) {
                successfulLoading = false;
            }
        }
    }

    public void setPage(final URL page) throws IOException {
        if (page == null) {
            throw new IOException(Messages.getString("swing.03","Page")); //$NON-NLS-1$ //$NON-NLS-2$
        } 

        String url = page.toString();
        String baseUrl = getBaseURL(url);
        Document oldDoc = getDocument();
        if (baseUrl != null
            && oldDoc != null
            && baseUrl.equals(oldDoc
                .getProperty(Document.StreamDescriptionProperty))) {

            scrollToReference(page.getRef());
            return;
        }
        InputStream stream = getStream(page);
        if (stream == null) {
            return;
        }
        Document newDoc = editorKit.createDefaultDocument();
        // Perhaps, it is reasonable only for HTMLDocument...
        if (newDoc instanceof HTMLDocument) {
            newDoc.putProperty(Document.StreamDescriptionProperty, baseUrl);
            newDoc.putProperty(StringConstants.IGNORE_CHARSET_DIRECTIVE,
                               new Boolean(false));
            try {
                ((HTMLDocument)newDoc).setBase(new URL(baseUrl));
            } catch (IOException e) {
            }
        }
        // TODO Asynch loading doesn't work with completely.
        // Also page property change event is written incorrectly now
        // (at the asynchrounous loading), because loading may not be
        // completed.
        // int asynchronousLoadPriority = getAsynchronousLoadPriority(newDoc);
        int asynchronousLoadPriority = -1;
        if (asynchronousLoadPriority >= 0) {
            setDocument(newDoc);
            AsynchLoad newThread = new AsynchLoad(asynchronousLoadPriority,
                                                  stream, page);
            newThread.start();
            if (newThread.successfulLoading) {
                changePage(page);
            }
        } else {
            try {
                documentLoading(stream, newDoc, page);
                stream.close();
                setDocument(newDoc);
                changePage(page);
            } catch (IOException e) {
            }
        }
    }

    private void changePage(final URL newPage) {
        URL oldPage = currentPage;
        currentPage = newPage;
        firePropertyChange("page", oldPage, currentPage);
    }

    private int getAsynchronousLoadPriority(final Document doc) {
        if (doc instanceof AbstractDocument) {
            return ((AbstractDocument)doc).getAsynchronousLoadPriority();
        }
        return -1;
    }

    @Override
    public synchronized void setText(final String content) {
        StringReader reader = new StringReader(content == null ? "" : content);

        try {
            read(reader, getContentType());
        } catch (IOException e) {
        }
    }
}
