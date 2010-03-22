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
package javax.swing.plaf.basic;


import java.awt.AWTKeyStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentInputMapUIResource;
import javax.swing.plaf.InputMapUIResource;
import javax.swing.plaf.TextUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.Position;
import javax.swing.text.TextAction;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import org.apache.harmony.awt.text.RootViewContext;
import org.apache.harmony.awt.text.TextFactory;
import org.apache.harmony.awt.text.TextUtils;
import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;
import org.apache.harmony.x.swing.internal.nls.Messages;


public abstract class BasicTextUI extends TextUI implements ViewFactory {

    public static class BasicCaret extends DefaultCaret implements UIResource {
        public BasicCaret() {
            super();
        }
    }

    public static class BasicHighlighter extends DefaultHighlighter implements
            UIResource {
        public BasicHighlighter() {
            super();
        }
    }

    // Text Component, concerned with this BasicTextUI
    private JTextComponent         component;

    // Editor Kit for all components excluding JTextPane and JEditorPane
    private static final EditorKit EDITOR_KIT = new DefaultEditorKit();

    // The Parent of all view Hierarchy. In never changes. When document
    // changes,
    // a child of rootView will be replaced only.
    private RootViewContext        rootViewContext;

    // PropertyChangeListener, DocumentListener
    private Listener               listener             = new Listener();

    // Document, concerned with this BasicTextUI (component.getDocument)
    private Document               document;

    // Simple TransferHandler to support cut, copy, paste operations
    private static TransferHandler transferHandler;

    // This flag is used in paintSafely, modelToView, viewToModel methods.
    private boolean                isActive             = false;

    // This flag is used in Listener.InsertUpdate method. It need not to lock
    // document in DocumentListener. Then modelChanged method defines, it need
    // to lock document or not
    private boolean                needLock             = true;

    // if document i18n property changes, then ViewHierarchy must be rebuild.
    private boolean                i18nProperty         = false;

    // Action to request focus on Text Component (support FocusAcceleratorKey
    //on JTextComponent).
    private final Action           focusAction = new RequestFocusAction();

    // Key for FOCUS_ACTION in ActionMap
    private static final String    FOCUS_ACTION_NAME    = "request-focus";

    // Flag is used in UpdateFocus AcceleratorKey method
    private char                   lastFocusAccelerator = '\0';

    //Listener for drop target
    private DropListener           dropListener;

    //Initiator for DnD
    private GestureRecognizer      gestureRecognizer;

    private final RootViewContext.ViewFactoryGetter viewFactoryGetter =
        new RootViewContext.ViewFactoryGetter() {

        public ViewFactory getViewFactory() {
            ViewFactory viewFactory = getEditorKit(component).getViewFactory();
            return (viewFactory == null) ? BasicTextUI.this : viewFactory;
        }
    };

    /**
     * Action to request focus on Text Component, concerned with this
     * BasicTextUI
     */
    private class RequestFocusAction extends TextAction {
        public RequestFocusAction() {
            super(FOCUS_ACTION_NAME);
        }

        public void actionPerformed(final ActionEvent e) {
            JTextComponent jtc = null;
            if (e != null && e.getSource() != null) {
                jtc = (JTextComponent)e.getSource();
            }
            if (jtc != null) {
                jtc.requestFocusInWindow();
            }
        }
    }

    /**
     * Transfer Handler to support basic transfer operation. It need to redefine
     * javax.swing.TransferHandler because it doesn't support required
     * operations. See java.beans (Introspector, BeanInfo,...)
     */
    private static class TextTransferHandler extends TransferHandler
        implements UIResource {
        boolean isDrag = false;

        public boolean canImport(final JComponent comp,
                                 final DataFlavor[] dataFlavor) {
            //Note: temporary solution
            return true; //super.canImport(arg0, arg1);
        }

        public void exportToClipboard(final JComponent c, final Clipboard clip,
                                      final int action) {
            TextUtils.exportToClipboard(TextUtils.getTextKit(c), clip, action);
        }

        public boolean importData(final JComponent c, final Transferable t) {
            if (isDrag || !(c instanceof JTextComponent)) {
                return false;
            }
            return TextUtils.importData(TextUtils.getTextKit(c), t);
        }

        protected void exportDone(final JComponent c, final Transferable data,
                                  final int action) {
            isDrag = false;
            TextUtils.exportDone(TextUtils.getTextKit(c), data, action);
        }

        protected Transferable createTransferable(final JComponent c) {
            return TextUtils.createTransferable(TextUtils.getTextKit(c));
        }

        public int getSourceActions(final JComponent c) {
            return TextUtils.getSourceActions(TextUtils.getTextKit(c));
        }

        public void exportAsDrag(final JComponent comp, final InputEvent ie,
                                 final int action) {
            isDrag = true;
            super.exportAsDrag(comp, ie, action);
        }
    }

    private class Listener implements DocumentListener, PropertyChangeListener {
        /**
         * Call rootView.changeUpdate
         */
        public void changedUpdate(final DocumentEvent e) {
            getRootView().changedUpdate(e, getVisibleEditorRect(),
                                        getRootView().getViewFactory());
        }

        /**
         * If "i18n" property of document is changed call modelChanged.
         * Otherwise, call rooView.insertUpdate
         */
        public void insertUpdate(final DocumentEvent e) {
            boolean currentI18nProperty = ((Boolean)document
                    .getProperty(StringConstants.BIDI_PROPERTY)).booleanValue();
            if (currentI18nProperty && !i18nProperty) {
                needLock = false;
                modelChanged();
                needLock = true;
                i18nProperty = true;
            } else {
                getRootView().insertUpdate(e, getVisibleEditorRect(),
                                           getRootView().getViewFactory());
            }
        }

        /**
         * Call rootView.removeUpdate
         */
        public void removeUpdate(final DocumentEvent e) {
            getRootView().removeUpdate(e, getVisibleEditorRect(), getRootView()
                                  .getViewFactory());
        }

        /**
         * If document is changed on Text Component, handle this event. In
         * particular remove DocumentListener from old document, add Document
         * Listener to new document, Rebuild view hierarchy. If
         * "componentOrientaion" property is changes, call modelChanged. If
         * JTextComponent.FOCUS_ACCELERATOR_KEY property is changes, call
         * private method UpdateFocusAcceleratorBinding (to reflect changes on
         * InputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)). Call propertyChange
         * method.
         */
        public void propertyChange(final PropertyChangeEvent e) {
            String name = e.getPropertyName();
            if (StringConstants.TEXT_COMPONENT_DOCUMENT_PROPERTY.equals(name)) {
                if (document != null) {
                    document.removeDocumentListener(listener);
                }
                Object doc = e.getNewValue();
                if (doc != null) {
                    setDocument((Document)doc);
                    document.addDocumentListener(listener);
                    modelChanged();
                }
            } else if (StringConstants.COMPONENT_ORIENTATION.equals(name)) {
                    modelChanged();


            } else  if (JTextComponent.FOCUS_ACCELERATOR_KEY.equals(name)) {
                lastFocusAccelerator = ((Character)e.getOldValue()).charValue();
                updateFocusAcceleratorBinding(true);
            }
            propertyChangeImpl(e);
            BasicTextUI.this.propertyChange(e);
        }
    }

    /*
     * Performs drop, instances of this class are added to
     * component.getDropTarget()
     */
    final class DropListener implements DropTargetListener {
        String         str   = null;

        int            start = 0;

        int            end   = 0;

        JTextComponent textComponent;

        public void dragEnter(final DropTargetDragEvent e) {
        }

        public void dragExit(final DropTargetEvent e) {
        }

        public void dragOver(final DropTargetDragEvent e) {
            /*
             * Component comp = e.getDropTargetContext().getComponent(); if
             * (comp == null || ! (comp instanceof JTextComponent)) return;
             * textComponent = (JTextComponent)comp; Transferable t =
             * e.getTransferable(); start = textComponent.getSelectionStart();
             * end = textComponent.getSelectionEnd(); try { str = (String) t
             * .getTransferData(DataFlavor.stringFlavor); } catch (final
             * UnsupportedFlavorException ufe) { } catch (final IOException
             * ioe) {}
             */
        }

        public void dropActionChanged(final DropTargetDragEvent arg0) {
        }

        public void drop(final DropTargetDropEvent e) {
            int length = end - start;
            int insertPosition = textComponent.viewToModel(e.getLocation());
            try {
                textComponent.getDocument().remove(start, length);
                if (insertPosition > end) {
                    insertPosition -= length;
                } else {
                    if (insertPosition >= start) {
                        insertPosition = start;
                    }
                }
                textComponent.getDocument().insertString(insertPosition, str,
                                                         null);
                textComponent.replaceSelection("");
                textComponent.setCaretPosition(insertPosition + length);

            } catch (final BadLocationException ex) {
            }
            e.dropComplete(true);
        }
    }

    /*
     * Initiates DnD, instances of this class are added to current component
     */
    final class GestureRecognizer extends MouseInputAdapter {
        boolean wasMousePressed = false;

        public void mousePressed(final MouseEvent arg0) {
            String selectedText = component.getSelectedText();
            if (selectedText != null && selectedText != ""
                && component.getDragEnabled()) {
                wasMousePressed = true;
            }
        }

        public void mouseDragged(final MouseEvent me) {
            if (wasMousePressed) {
                wasMousePressed = false;
                component.getTransferHandler()
                        .exportAsDrag(component, me, TransferHandler.MOVE);
            }
        }
    }

    /**
     * Creates parent of all view hierarchy.
     */
    public BasicTextUI() {
        initRootViewContext(null);
        dropListener = new DropListener();
        gestureRecognizer = new GestureRecognizer();
    }

    public View create(final Element elem) {
        return null;
    }

    public View create(final Element elem, final int p0, final int p1) {
        return null;
    }

    protected Caret createCaret() {
        return new BasicCaret();
    }

    protected Highlighter createHighlighter() {
        return new BasicHighlighter();
    }

    protected Keymap createKeymap() {
        String name = getKeymapName();
        Keymap keymap = JTextComponent.getKeymap(name);
        if (keymap == null) {
            keymap = JTextComponent.addKeymap(name, JTextComponent
                    .getKeymap(JTextComponent.DEFAULT_KEYMAP));
        }
        Object bindings = UIManager.get(addPrefix(".keyBindings"));
        if (bindings != null) {
            JTextComponent.loadKeymap(keymap,
                                      (JTextComponent.KeyBinding[])bindings,
                                      component.getActions());
        }
        return keymap;
    }

    public void damageRange(final JTextComponent c, final int p0,
                            final int p1) {
        damageRange(c, p0, p1, Position.Bias.Forward, Position.Bias.Forward);
    }

    /**
     * Calculates rectangle(r, for example), corresponding these position and
     * biases. Then call component.repaint(r.x, r.y, r.width, r.height)
     */
    public void damageRange(final JTextComponent c, final int p0, final int p1,
                            final Position.Bias b1, final Position.Bias b2) {
        Shape shape = null;
        try {
            shape = getRootView()
                    .modelToView(p0, b1, p1, b2, getVisibleEditorRect());
        } catch (final BadLocationException e) {
        }
        Rectangle rect;
        if (shape != null) {
            rect = shape.getBounds();
            component.repaint(rect.x, rect.y, rect.width, rect.height);
        }
    }

    protected final JTextComponent getComponent() {
        return component;
    }

    /**
     * Always returns DefaultEditorKit
     */
    public EditorKit getEditorKit(final JTextComponent comp) {
        return EDITOR_KIT;
    }

    protected String getKeymapName() {
        String className = getClass().getName();
        final int lastDot = className.lastIndexOf('.');
        return className.substring(lastDot + 1);
    }

    public Dimension getMaximumSize(final JComponent c) {
        //See description for getRootView().getMaximumSpan
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Uses getMinimumSpan of View Hierarchy and getInsets of TextComponent
     */
    public Dimension getMinimumSize(final JComponent c) {
        Insets insets = c.getInsets();
        int minX = TextUtils.getHrzInsets(insets)
            + (int)getRootView().getMinimumSpan(View.X_AXIS);
        int minY = TextUtils.getVrtInsets(insets)
            + (int)getRootView().getMinimumSpan(View.Y_AXIS);
        return new Dimension(minX, minY);
    }


    /**
     * Calls getNextVisualPosition on root view. If root view returns -1, then
     * this method will return p0 and biasRet[0] will be the same as bias
     */
    public int getNextVisualPositionFrom(final JTextComponent c, final int p0,
                                         final Position.Bias bias,
                                         final int p1,
                                         final Position.Bias[] biasRet)
            throws BadLocationException {

        int pos = getRootView().getNextVisualPositionFrom(p0, bias,
                                                     getVisibleEditorRect(),
                                                     p1, biasRet);
        if (pos == -1) {
            pos = p0;
            biasRet[0] = bias;
        }
        return pos;
    }

    /**
     * Uses getPrefferedSpan of View Hierarchy and getInsets of TextComponent
     */
    public Dimension getPreferredSize(final JComponent c) {
        Insets insets = c.getInsets();
        int prefX = TextUtils.getHrzInsets(insets)
            + (int)getRootView().getPreferredSpan(View.X_AXIS);
        int prefY = TextUtils.getVrtInsets(insets)
            + (int)getRootView().getPreferredSpan(View.Y_AXIS);
        return new Dimension(prefX, prefY);
    }

    protected abstract String getPropertyPrefix();

    public View getRootView(final JTextComponent c) {
        return getRootView();
    }

    public String getToolTipText(final JTextComponent c, final Point p) {
        Rectangle r = getVisibleEditorRect();
        return (r != null) ? getRootView().getToolTipText(p.x, p.y, r) : null;
    }

    private TransferHandler getTransferHandler() {
        if (transferHandler == null) {
            transferHandler = new TextTransferHandler();
        }
        return transferHandler;
    }

    /**
     * Returns, component getSize, excluding insets.
     */
    protected Rectangle getVisibleEditorRect() {
        return TextUtils.getEditorRect(component);
    }

    final String addPrefix(final String property) {
        return getPropertyPrefix() + property;
    }

    protected void installDefaults() {
        LookAndFeel.installColorsAndFont(component, addPrefix(".background"),
                                                    addPrefix(".foreground"),
                                                    addPrefix(".font"));
        if (Utilities.isUIResource(component.getBorder())) {
            component.setBorder(UIManager.getBorder(addPrefix(".border")));
        }
        if (Utilities.isUIResource(component.getMargin())) {
            component.setMargin(UIManager.getInsets(addPrefix(".margin")));
        }
        //RI 6251901. Documentation error
        if (Utilities.isUIResource(component.getCaretColor())) {
            component.setCaretColor(UIManager.getColor(addPrefix(
                                                      ".caretForeground")));
        }

        if (Utilities.isUIResource(component.getSelectionColor())) {
            component.setSelectionColor(UIManager.getColor(addPrefix(
                                                      ".selectionBackground")));
        }

        if (Utilities.isUIResource(component.getSelectedTextColor())) {
            component.setSelectedTextColor(UIManager
                    .getColor(addPrefix(".selectionForeground")));
        }

        if (Utilities.isUIResource(component.getDisabledTextColor())) {
            component.setDisabledTextColor(UIManager.getColor(addPrefix(
                                         ".inactiveForeground")));
        }
    }

    final Set<AWTKeyStroke> getDefaultFocusTraversalKeys(final int mode) {
          Set<AWTKeyStroke> result = component.getFocusTraversalKeys(mode);
          
          if (result == null) {
              result = new LinkedHashSet<AWTKeyStroke>();
              if (mode == KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS) {
                  result.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,
                                              InputEvent.CTRL_DOWN_MASK));
              } else {
                  result.add(KeyStroke
                             .getKeyStroke(KeyEvent.VK_TAB,
                                     InputEvent.CTRL_DOWN_MASK
                                           | InputEvent.SHIFT_DOWN_MASK));
              }
         } else {
             result = new LinkedHashSet<AWTKeyStroke>(result);
         }
          
         return result;
      }

    void updateFocusTraversalKeys() {
        if (component == null) {
            return;
        }
        Set<AWTKeyStroke> forwardFocusTraversalKeys =
            getDefaultFocusTraversalKeys(KeyboardFocusManager
                                         .FORWARD_TRAVERSAL_KEYS);
        Set<AWTKeyStroke> backwardFocusTraversalKeys =
            getDefaultFocusTraversalKeys(KeyboardFocusManager
                                         .BACKWARD_TRAVERSAL_KEYS);
        KeyStroke tabPressed = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
        KeyStroke shiftTabPressed = KeyStroke
            .getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK);
        
        if (component.isEditable()) {
            forwardFocusTraversalKeys.remove(tabPressed);
            backwardFocusTraversalKeys.remove(shiftTabPressed);
        } else {
            if (!forwardFocusTraversalKeys.contains(tabPressed)) {
               forwardFocusTraversalKeys.add(tabPressed);
            }
            if (!forwardFocusTraversalKeys.contains(shiftTabPressed)) {
               backwardFocusTraversalKeys.add(shiftTabPressed);
            }
        }

        component.setFocusTraversalKeys(KeyboardFocusManager
                                      .FORWARD_TRAVERSAL_KEYS,
                                      forwardFocusTraversalKeys);
        component.setFocusTraversalKeys(KeyboardFocusManager
                                      .BACKWARD_TRAVERSAL_KEYS,
                                      backwardFocusTraversalKeys);
  }

    final void installUIInputMap() {
        String propertyName = addPrefix(".focusInputMap");
        InputMapUIResource inputMap1 = new InputMapUIResource();
        InputMapUIResource inputMap2 = (InputMapUIResource)UIManager
                .get(propertyName);
        inputMap1.setParent(inputMap2);
        SwingUtilities.replaceUIInputMap(component, JComponent.WHEN_FOCUSED,
                                         inputMap1);
    }

    final void putActionToActionMap(final Action a, final ActionMap map) {
        Object name = a.getValue(Action.NAME);
        map.put(name, a);
    }

    final void installUIActionMap() {
        UIDefaults uiDefaults = UIManager.getLookAndFeelDefaults();
        String propertyName = getPropertyPrefix() + ".actionMap";
        ActionMap actionMap1 = new ActionMapUIResource();
        putActionToActionMap(focusAction, actionMap1);
        Object actionMap2 = uiDefaults.get(propertyName);
        if (actionMap2 == null) {
            ActionMapUIResource map = new ActionMapUIResource();
            Action[] actions = component.getActions();
            for (int i = 0; i < actions.length; i++) {
                putActionToActionMap(actions[i], map);
            }
            putActionToActionMap(TransferHandler.getPasteAction(), map);
            putActionToActionMap(TransferHandler.getCutAction(), map);
            putActionToActionMap(TransferHandler.getCopyAction(), map);

            actionMap2 = map;
            if (!(component instanceof JEditorPane)) {
                uiDefaults.put(propertyName, map);
            }
        }
        actionMap1.setParent((ActionMap)actionMap2);
        SwingUtilities.replaceUIActionMap(component, actionMap1);
    }

    /**
     * Sets InputMap(JComponent.WHEN_FOCUSED), ActionMap, Keymap on
     * TextComponent.
     *
     */
    protected void installKeyboardActions() {
        installUIActionMap();
        installUIInputMap();
        Keymap keymap = createKeymap();
        component.setKeymap(keymap);
    }

    protected void installListeners() {
    }

    public void installUI(final JComponent c) {
        if (!(c instanceof JTextComponent)) {
            throw new Error(Messages.getString("swing.B1")); //$NON-NLS-1$
        }
        
        super.installUI(c);

        setComponent((JTextComponent)c);

        installDefaults();
        LookAndFeel.installProperty(component, StringConstants.OPAQUE_PROPERTY,
                                    Boolean.TRUE);

        if (Utilities.isUIResource(component.getCaret())) {
            Caret caret = createCaret();
            component.setCaret(caret);
            caret.setBlinkRate(UIManager.getInt(getPropertyPrefix()
                                                  + ".caretBlinkRate"));
        }

        if (Utilities.isUIResource(component.getHighlighter())) {
            component.setHighlighter(createHighlighter());
        }

        if (Utilities.isUIResource(component.getTransferHandler())) {
            component.setTransferHandler(getTransferHandler());
        }

        if (component.getDocument() == null) {
            setDocument(getEditorKit(component).createDefaultDocument());
            component.setDocument(document);
        } else {
            setDocument(component.getDocument());
        }

        modelChanged();

        ((AbstractDocument)component.getDocument())
                .addDocumentListener(listener);
        component.addPropertyChangeListener(listener);
        JTextComponent.addKeymap(getKeymapName(), JTextComponent
                .getKeymap(JTextComponent.DEFAULT_KEYMAP));

        installListeners();

        installKeyboardActions();

        isActive = true;
        component.setAutoscrolls(true);
        updateFocusAcceleratorBinding(true);

        //DnD support
        // java.awt.Component doesn't support DnD
        /*
         * try { component.getDropTarget().addDropTargetListener(dropListener);
         *  } catch (final TooManyListenersException e){}
         */
        component.addMouseListener(gestureRecognizer);
        component.addMouseMotionListener(gestureRecognizer);
        updateFocusTraversalKeys();
    }

    /**
     * Rebuilts view hierarchy.
     *
     */
    protected void modelChanged() {
        final Document doc = document;

        if (needLock) {
            readLock(doc);
        }
        try {
            setDocument(document);
            View view = getRootView().getViewFactory()
                .create(document.getDefaultRootElement());
            setView(view);
            setViewSize();
        } finally {
            if (needLock) {
                readUnlock(doc);
            }
        }
        if (component != null) {
            component.repaint();
        }
    }

    public Rectangle modelToView(final JTextComponent c, final int p)
            throws BadLocationException {
        return modelToView(c, p, Position.Bias.Forward);
    }

    public Rectangle modelToView(final JTextComponent comp, final int p,
                                 final Position.Bias b)
            throws BadLocationException {
        final Document doc = document;
    	
        readLock(doc);
        try {
            Rectangle r = null;
            if (isActive) {
                Rectangle rect = getVisibleEditorRect();
                if (rect != null) {
                    getRootView().setSize(rect.width, rect.height);
                    Shape shape = getRootView().modelToView(p, rect, b);
                    if (shape != null) {
                        r = shape.getBounds();
                    }
                }
             }
             return r;
        } finally {
            readUnlock(doc);
        }
    }

    public final void paint(final Graphics g, final JComponent c) {
        //super.paint(g, c);//???
        paintSafely(g);
    }

    protected void paintBackground(final Graphics g) {
        Color color = component.getBackground();
        if (color != null) {
            g.setColor(color);
        }
        Rectangle rect = getVisibleEditorRect();
        if (rect != null) {
            g.fillRect(rect.x, rect.y, rect.width, rect.height);
        }
    }

    protected void paintSafely(final Graphics g) {
        if (!isActive) {
            return;
        }
        
        final Document doc = document;
        
        readLock(doc);
        try {
            Highlighter highlighter = component.getHighlighter();
            Caret caret = component.getCaret();
            if (component.isOpaque()) {
                paintBackground(g);
            }
            if (highlighter != null) {
                highlighter.paint(g);
            }
            Rectangle visibleRect = getVisibleEditorRect();
            if (visibleRect != null) {
                getRootView().setSize(visibleRect.width, visibleRect.height);
                getRootView().paint(g, visibleRect);
            }
            caret.paint(g);
        } finally {
            readUnlock(doc);
        }
    }

    void propertyChangeImpl(final PropertyChangeEvent e) {
        if (org.apache.harmony.x.swing.StringConstants.EDITABLE_PROPERTY_CHANGED
                .equals(e.getPropertyName())
                && !e.getOldValue().equals(e.getNewValue())) {
            updateFocusTraversalKeys();
        }
    }

    protected void propertyChange(final PropertyChangeEvent e) {
    }

    /**
     * Replaces child of the root view
     */
    protected final void setView(final View v) {
        component.removeAll();
        if (v != null) {
            getRootView().append(v);
        }
        if (component != null) {
            component.invalidate();
        }
    }

    protected void uninstallDefaults() {
        if (Utilities.isUIResource(component.getForeground())) {
            component.setForeground(null);
        }

        if (Utilities.isUIResource(component.getBackground())) {
            component.setBackground(null);
        }

        if (Utilities.isUIResource(component.getFont())) {
            component.setFont(null);
        }

        if (Utilities.isUIResource(component.getBorder())) {
            component.setBorder(null);
        }

        if (Utilities.isUIResource(component.getMargin())) {
            component.setMargin(null);
        }

        if (Utilities.isUIResource(component.getCaretColor())) {
            component.setCaretColor(null);
        }

        if (Utilities.isUIResource(component.getSelectionColor())) {
            component.setSelectionColor(null);
        }

        if (Utilities.isUIResource(component.getSelectedTextColor())) {
            component.setSelectedTextColor(null);
        }

        if (Utilities.isUIResource(component.getDisabledTextColor())) {
            component.setDisabledTextColor(null);
        }
    }

    /**
     * Sets ActionMap and Keymap of TextComponent to null.
     *
     */
    protected void uninstallKeyboardActions() {
        component.setKeymap(null);
        SwingUtilities.replaceUIActionMap(component, null);
    }

    protected void uninstallListeners() {
    }

    public void uninstallUI(final JComponent c) {
        if (component != c) {
            return;
        }
        isActive = false;
        super.uninstallUI(c);

        if (Utilities.isUIResource(component.getCaret())) {
            component.setCaret(null);
        }

        if (Utilities.isUIResource(component.getHighlighter())) {
            component.setHighlighter(null);
        }

        uninstallDefaults();
        uninstallKeyboardActions();
        uninstallListeners();
        ((AbstractDocument) component.getDocument())
                .removeDocumentListener(listener);
        component.removePropertyChangeListener(listener);

        //DnD support
        //java.awt.Component doesn't support DnD
        /*
         * component.getDropTarget().removeDropTargetListener(dropListener);
         */
        component.removeMouseListener(gestureRecognizer);
        component.removeMouseMotionListener(gestureRecognizer);

        setComponent(null);
    }

    public void update(final Graphics g, final JComponent c) {
        super.update(g, c);
    }

    final void updateFocusAcceleratorBinding(final boolean changed) {
        if (!changed) {
            return;
        }
        char accelerator = component.getFocusAccelerator();
        InputMap im = SwingUtilities.getUIInputMap(component,
                                  JComponent.WHEN_IN_FOCUSED_WINDOW);
        boolean wasInputMap = (im != null);
        boolean needToRemove = (lastFocusAccelerator != '\0');
        boolean needToAdd = (accelerator != '\0');
        if (needToAdd) {
            if (needToRemove && wasInputMap) {
                im.remove(KeyStroke.getKeyStroke(lastFocusAccelerator,
                                                 InputEvent.ALT_DOWN_MASK));
            } else if (!wasInputMap) {
                im = new ComponentInputMapUIResource(component);
                im.put(KeyStroke.getKeyStroke(accelerator,
                                              InputEvent.ALT_DOWN_MASK),
                       FOCUS_ACTION_NAME);
                if (!wasInputMap) {
                    SwingUtilities.replaceUIInputMap(component,
                       JComponent.WHEN_IN_FOCUSED_WINDOW, im);
                }

         } else if (wasInputMap) {
                    im.remove(KeyStroke.getKeyStroke(lastFocusAccelerator,
                                                     InputEvent.ALT_DOWN_MASK));
         }
        }
    }

    public int viewToModel(final JTextComponent c, final Point p) {
        return viewToModel(c, p, new Position.Bias[1]);
    }

    public int viewToModel(final JTextComponent c, final Point p,
                           final Position.Bias[] b) {
        return getRootView().viewToModel(p.x, p.y, getVisibleEditorRect(), b);
    }

    private void setViewSize() {
        Rectangle rect = getVisibleEditorRect();
        if (rect != null) {
            getRootView().setSize(rect.width, rect.height);
        }
    }

    private void readLock(final Document doc) {
        if (doc instanceof AbstractDocument) {
            ((AbstractDocument)doc).readLock();
        }
    }

    private void readUnlock(final Document doc) {
        if (doc instanceof AbstractDocument) {
            ((AbstractDocument)doc).readUnlock();
        }
    }

    final boolean getI18nProperty() {
        return document == null
               ? false
               : ((Boolean)document.getProperty(StringConstants.BIDI_PROPERTY))
                 .booleanValue();
    }

    private View getRootView() {
        return rootViewContext.getView();
    }

    private void setDocument(final Document document) {
        this.document = document;
        rootViewContext.setDocument(document);
    }

    private void setComponent(final JTextComponent component) {
        this.component = component;
        rootViewContext.setComponent(component);
    }

    private void initRootViewContext(final Element elem) {
        rootViewContext = TextFactory.getTextFactory().createRootView(elem);
        rootViewContext.setViewFactoryGetter(viewFactoryGetter);
        setComponent(component);
        setDocument(document);
    }
}


