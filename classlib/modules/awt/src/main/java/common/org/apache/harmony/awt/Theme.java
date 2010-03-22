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
 * @author Pavel Dolgov
 */
package org.apache.harmony.awt;

import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

import org.apache.harmony.awt.state.ButtonState;
import org.apache.harmony.awt.state.CheckboxState;
import org.apache.harmony.awt.state.ChoiceState;
import org.apache.harmony.awt.state.LabelState;
import org.apache.harmony.awt.state.ListState;
import org.apache.harmony.awt.state.MenuBarState;
import org.apache.harmony.awt.state.MenuState;
import org.apache.harmony.awt.state.ScrollbarState;
import org.apache.harmony.awt.state.TextComponentState;
import org.apache.harmony.awt.state.TextState;
import org.apache.harmony.awt.theme.DefaultButton;
import org.apache.harmony.awt.theme.DefaultCheckbox;
import org.apache.harmony.awt.theme.DefaultChoice;
import org.apache.harmony.awt.theme.DefaultFileDialog;
import org.apache.harmony.awt.theme.DefaultLabel;
import org.apache.harmony.awt.theme.DefaultList;
import org.apache.harmony.awt.theme.DefaultMenu;
import org.apache.harmony.awt.theme.DefaultMenuBar;
import org.apache.harmony.awt.theme.DefaultScrollbar;
import org.apache.harmony.awt.theme.DefaultStyle;
import org.apache.harmony.awt.theme.DefaultTextComponent;


/**
 * Standard appearance of standard components
 */
public class Theme {

    public void drawButton(Graphics g, ButtonState s) {
        drawButtonBackground(g, s);
        drawButtonText(g, s);
    }

    public void calculateButton(ButtonState s) {
        DefaultButton.calculate(s);
    }

    public void drawLabel(Graphics g, LabelState s) {
        drawLabelBackground(g, s);
        drawLabelText(g, s);
    }

    public void calculateLabel(LabelState s) {
        DefaultLabel.calculate(s);
    }

    public void drawCheckbox(Graphics g, CheckboxState s) {
        Rectangle textRect = DefaultCheckbox.getTextRect(s);
        drawCheckboxBackground(g, s, textRect);
        drawCheckboxText(g, s, textRect);
    }

    public void calculateCheckbox(CheckboxState s) {
        DefaultCheckbox.calculate(s);
    }

    public void drawScrollbar(Graphics g, ScrollbarState s) {
        DefaultScrollbar.draw(g, s);
    }

    public void calculateScrollbar(ScrollbarState s) {
        DefaultScrollbar.calculate(s);
    }

    public void layoutScrollbar(ScrollbarState s) {
        DefaultScrollbar.layout(s);
    }

    public void drawChoice(Graphics g, ChoiceState s) {
        drawChoiceBackground(g, s);
        drawChoiceText(g, s);

    }

    public void drawTextComponentBackground(Graphics g, TextComponentState s) {
        DefaultTextComponent.drawBackground(g, s);
    }

    public void drawList(Graphics g, ListState s, boolean flat) {
        drawListBackground(g, s, flat);
        drawListItems(g, s);
    }

    public void drawMenu(MenuState s, Graphics gr) {
        DefaultMenu.drawMenu(s, gr);
    }

    public Dimension calculateMenuSize(MenuState s) {
        return DefaultMenu.calculateSize(s);
    }

    public int getMenuItemIndex(MenuState s, Point p) {
        return DefaultMenu.getItemIndex(s, p);
    }

    public Point getMenuItemLocation(MenuState s, int index) {
        return DefaultMenu.getItemLocation(s, index);
    }

    public void drawMenuBar(MenuBarState s, Graphics gr) {
        DefaultMenuBar.drawMenuBar(s, gr);
    }

    public void layoutMenuBar(MenuBarState s, int width) {
        DefaultMenuBar.layoutMenuBar(s, width);
    }

    public int getMenuBarItemIndex(MenuBarState s, Point p) {
        return DefaultMenuBar.getItemIndex(s, p);
    }

    public Point getMenuBarItemLocation(MenuBarState s, int index) {
        return DefaultMenuBar.getItemLocation(s, index);
    }


    protected void drawListItems(Graphics g, ListState s) {
        DefaultList.drawItems(g, s);

    }

    protected void drawListBackground(Graphics g, ListState s, boolean flat) {
        DefaultList.drawBackground(g, s, flat);
    }

    protected void drawChoiceText(Graphics g, ChoiceState s) {
        DefaultChoice.drawText(g, s);
    }

    protected void drawChoiceBackground(Graphics g, ChoiceState s) {
        DefaultChoice.drawBackground(g, s);
    }

    protected void drawButtonBackground(Graphics g, ButtonState s) {
        DefaultButton.drawBackground(g, s);
    }

    protected void drawButtonText(Graphics g, ButtonState s) {
        DefaultButton.drawText(g, s);
    }

    protected void drawLabelBackground(Graphics g, LabelState s) {
        DefaultLabel.drawBackground(g, s);
    }

    protected void drawLabelText(Graphics g, LabelState s) {
        DefaultLabel.drawText(g, s);
    }

    protected void drawCheckboxBackground(Graphics g, CheckboxState s,
            Rectangle focusRect) {
        DefaultCheckbox.drawBackground(g, s, focusRect);
    }

    protected void drawCheckboxText(Graphics g, CheckboxState s,
                                         Rectangle r) {
        DefaultCheckbox.drawText(g, s, r);
    }


    protected void drawFocusRect(Graphics g, TextState s, Rectangle r) {
        if (s.isFocused()) {
            DefaultStyle.drawFocusRect(g, r.x, r.y, r.width, r.height);
        }

    }

    public boolean showFileDialog(FileDialog fd) {
        DefaultFileDialog dfd = new DefaultFileDialog(fd);
        return dfd.show();
    }
    
    public boolean hideFileDialog(FileDialog fd) {
        return true;
    }

}
