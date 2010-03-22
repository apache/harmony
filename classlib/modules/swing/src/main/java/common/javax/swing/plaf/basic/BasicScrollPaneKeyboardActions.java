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

package javax.swing.plaf.basic;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import org.apache.harmony.x.swing.Utilities;


class BasicScrollPaneKeyboardActions {
    private abstract static class PassThroughAction extends AbstractAction {
        protected static void passThroughEvent(final ActionEvent event, final JComponent destinationComponent, final String correspondingCommand) {
            if (!destinationComponent.isVisible()) {
                return;
            }
            Action action = destinationComponent.getActionMap().get(correspondingCommand);
            if (action != null) {
                action.actionPerformed(new ActionEvent(destinationComponent, ActionEvent.ACTION_PERFORMED, correspondingCommand, event.getWhen(), event.getModifiers()));
            }
        }
    }

    private abstract static class PassThroughToVerticalScrollbarAction extends PassThroughAction {
        protected static void passThroughEvent(final ActionEvent event, final String correspondingCommand) {
            JScrollPane scrollPane = (JScrollPane)event.getSource();
            JScrollBar vsb = scrollPane.getVerticalScrollBar();
            passThroughEvent(event, vsb, correspondingCommand);
        }
    }

    private abstract static class PassThroughToHorizontalScrollbarAction extends PassThroughAction {
        protected static void passThroughEvent(final ActionEvent event, final String correspondingCommand) {
            JScrollPane scrollPane = (JScrollPane)event.getSource();
            JScrollBar hsb = scrollPane.getHorizontalScrollBar();
            passThroughEvent(event, hsb, correspondingCommand);
        }
    }


    private static PassThroughAction unitScrollRightAction = new PassThroughToHorizontalScrollbarAction() {
        public void actionPerformed(final ActionEvent e) {
            passThroughEvent(e, "positiveUnitIncrement");
        }
    };

    private static PassThroughAction unitScrollLeftAction = new PassThroughToHorizontalScrollbarAction() {
        public void actionPerformed(final ActionEvent e) {
            passThroughEvent(e, "negativeUnitIncrement");
        }
    };

    private static PassThroughAction unitScrollUpAction = new PassThroughToVerticalScrollbarAction() {
        public void actionPerformed(final ActionEvent e) {
            passThroughEvent(e, "negativeUnitIncrement");
        }
    };

    private static PassThroughAction unitScrollDownAction = new PassThroughToVerticalScrollbarAction() {
        public void actionPerformed(final ActionEvent e) {
            passThroughEvent(e, "positiveUnitIncrement");
        }
    };

    private static PassThroughAction scrollDownAction = new PassThroughToVerticalScrollbarAction() {
        public void actionPerformed(final ActionEvent e) {
            passThroughEvent(e, "positiveBlockIncrement");
        }
    };

    private static PassThroughAction scrollUpAction = new PassThroughToVerticalScrollbarAction() {
        public void actionPerformed(final ActionEvent e) {
            passThroughEvent(e, "negativeBlockIncrement");
        }
    };

    private static PassThroughAction scrollLeftAction = new PassThroughToHorizontalScrollbarAction() {
        public void actionPerformed(final ActionEvent e) {
            passThroughEvent(e, "negativeBlockIncrement");
        }
    };

    private static PassThroughAction scrollRightAction = new PassThroughToHorizontalScrollbarAction() {
        public void actionPerformed(final ActionEvent e) {
            passThroughEvent(e, "positiveBlockIncrement");
        }
    };

    private static PassThroughAction scrollHomeAction = new PassThroughAction() {
        public void actionPerformed(final ActionEvent e) {
            JScrollPane scrollPane = (JScrollPane)e.getSource();

            JScrollBar vsb = scrollPane.getVerticalScrollBar();
            passThroughEvent(e, vsb, "minScroll");
            JScrollBar hsb = scrollPane.getHorizontalScrollBar();
            passThroughEvent(e, hsb, "minScroll");
        }
    };

    private static PassThroughAction scrollEndAction = new PassThroughAction() {
        public void actionPerformed(final ActionEvent e) {
            JScrollPane scrollPane = (JScrollPane)e.getSource();

            JScrollBar vsb = scrollPane.getVerticalScrollBar();
            passThroughEvent(e, vsb, "maxScroll");
            JScrollBar hsb = scrollPane.getHorizontalScrollBar();
            passThroughEvent(e, hsb, "maxScroll");
        }
    };



    public static void installKeyboardActions(final JScrollPane scrollPane) {
        Utilities.installKeyboardActions(scrollPane, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "ScrollPane.ancestorInputMap", "ScrollPane.ancestorInputMap.RightToLeft");

        scrollPane.getActionMap().put("unitScrollRight", unitScrollRightAction);
        scrollPane.getActionMap().put("unitScrollDown", unitScrollDownAction);
        scrollPane.getActionMap().put("unitScrollLeft", unitScrollLeftAction);
        scrollPane.getActionMap().put("unitScrollUp", unitScrollUpAction);
        scrollPane.getActionMap().put("scrollUp", scrollUpAction);
        scrollPane.getActionMap().put("scrollDown", scrollDownAction);
        scrollPane.getActionMap().put("scrollLeft", scrollLeftAction);
        scrollPane.getActionMap().put("scrollRight", scrollRightAction);
        scrollPane.getActionMap().put("scrollHome", scrollHomeAction);
        scrollPane.getActionMap().put("scrollEnd", scrollEndAction);
    }

    public static void uninstallKeyboardActions(final JScrollPane scrollPane) {
        Utilities.uninstallKeyboardActions(scrollPane, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }


    private static void appendInputMap(final InputMap source, final InputMap additional) {
        KeyStroke[] keyStrokes = additional.keys();
        for (int i = 0; i < keyStrokes.length; i++) {
            source.put(keyStrokes[i], additional.get(keyStrokes[i]));
        }
    }
}
