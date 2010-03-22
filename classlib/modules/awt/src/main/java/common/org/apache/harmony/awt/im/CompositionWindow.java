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
 * @author Dmitry A. Durnev
 */
package org.apache.harmony.awt.im;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.InputMethodEvent;
import java.awt.event.KeyEvent;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodRequests;
import java.text.CharacterIterator;

import javax.swing.text.DefaultCaret;

import org.apache.harmony.awt.ComponentInternals;
import org.apache.harmony.awt.text.InputMethodListenerImpl;
import org.apache.harmony.awt.text.InputMethodRequestsImpl;
import org.apache.harmony.awt.text.TextKit;

/**
 * Active IM client used to implement "below-the-spot" input style
 * with active clients & "root-window" style with passive clients.
 */
class CompositionWindow extends IMWindow {
    private class ActiveClient extends TextField {

        private final InputMethodRequestsImpl imRequests;
        private final InputMethodListenerImpl imListener;
        private final DefaultCaret caret;
        
        ActiveClient() throws HeadlessException {
            // prevent IM invocation on this component
            enableInputMethods(false);
            ComponentInternals ci = ComponentInternals.getComponentInternals();
            TextKit textKit = ci.getTextKit(this); 
            caret = (DefaultCaret) textKit.getCaret();
            caret.setBlinkRate(0);
            caret.setVisible(true);
            imRequests = new InputMethodRequestsImpl(textKit) {
                @Override
                public TextHitInfo getLocationOffset(int x, int y) {
                    if (!isShowing()) {
                        return null;
                    }
                    return super.getLocationOffset(x, y);
                }
            };
            imListener = new InputMethodListenerImpl(textKit) {
                @Override
                public void inputMethodTextChanged(InputMethodEvent ime) {                   
                    super.inputMethodTextChanged(ime);
                    // create KEY_TYPED event for each committed char
                    // and send it to passive client or just
                    // redirect this event to active client
                    if (client == null) {
                        return;
                    }
                    InputMethodRequests imr = client.getInputMethodRequests();
                    if (imr != null) {
                        if (IMManager.belowTheSpot()) {
                            // position window below the spot:
                            TextHitInfo offset = TextHitInfo.leading(0);
                            Rectangle textLoc = imr.getTextLocation(offset); 
                            setLocationBelow(textLoc);                           
                        } else {
                            client.dispatchEvent(ime);
                            return;
                        }
                    }
                    sendCommittedText(ime); 
                    
                }
            };
            addInputMethodListener(imListener);
        }
        
        private void sendCommittedText(InputMethodEvent ime) {
            int n = ime.getCommittedCharacterCount();
            // remove each committed char from text component
            if (n > 0) {
                setText(getText().substring(n));
            }
            char c;
            CharacterIterator text = ime.getText();
            if (text != null) {
                c = text.first();
                while (n-- > 0) {
                    sendChar((Component) ime.getSource(), ime.getWhen(), c);                    
                    c = text.next();
                }
            }
            
        }

        private void sendChar(Component src, long when, char c) {
            KeyEvent ke = new KeyEvent(src, KeyEvent.KEY_TYPED, when,
                                       0, KeyEvent.VK_UNDEFINED, c);
            src.dispatchEvent(ke);
            
        }

        @Override
        public InputMethodRequests getInputMethodRequests() {
            return imRequests;
        }
        
        @Override
        public void paint(Graphics g) {
            caret.paint(g);
        }
    
    }
    
    private Component client;
    private final ActiveClient activeClient;

    public CompositionWindow(Component client) {
        setClient(client);
        activeClient = new ActiveClient();
        add(activeClient);
        setSize(500, 40);
        // use root window input style by default:
        Dimension screenSize = getScreenSize();
        int x = screenSize.width - getWidth();
        int y = screenSize.height - 3 * getHeight();
        setLocation(x, y);
    }

    private Dimension getScreenSize() {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }
    
    @Override
    public InputMethodRequests getInputMethodRequests() {
        return getActiveClient().getInputMethodRequests();
    }
    
    Component getActiveClient() {
        return activeClient;
    }

    final void setClient(Component client) {
        this.client = client;
    }

    boolean isEmpty() {
        return (activeClient.getText().length() == 0);
    }

    private void setLocationBelow(Rectangle textLoc) {
        Point loc = textLoc.getLocation();
        loc.translate(0, textLoc.height);        
        Dimension screenSize = getScreenSize();
        int h = getHeight();
        int w = getWidth();
        if (loc.y + h > screenSize.height) {
            loc.y = textLoc.y - h;
        }            
        if (loc.x + w > screenSize.width) {
            loc.x = screenSize.width - w;
        }    
       setLocation(loc);
    }

}
