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

package javax.swing.plaf.multi;

import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class MultiLookAndFeel extends LookAndFeel {

	private static final String prefix = "javax.swing.plaf.multi.Multi"; //$NON-NLS-1$
	
	@SuppressWarnings("nls")
	private static final UIDefaults classDefaults=new UIDefaults(new Object[] { 
			"InternalFrameUI", prefix + "InternalFrameUI", 
			"ViewportUI", prefix + "ViewportUI",
			"ScrollBarUI", prefix + "ScrollBarUI",
			"ToolTipUI", prefix + "ToolTipUI", 
			"MenuItemUI", prefix + "MenuItemUI", 
			"MenuUI", prefix + "MenuUI",
			"TextAreaUI", prefix + "TextAreaUI", 
			"PopupMenuUI", prefix + "PopupMenuUI", 
			"ScrollPaneUI",	prefix + "ScrollPaneUI", 
			"SliderUI",	prefix + "SliderUI", 
			"ComboBoxUI", prefix + "ComboBoxUI", 
			"RadioButtonUI", prefix + "RadioButtonUI", 
			"FormattedTextFieldUI",	prefix + "FormattedTextFieldUI", 
			"TreeUI", prefix + "TreeUI", 
			"MenuBarUI", prefix + "MenuBarUI",
			"RadioButtonMenuItemUI", prefix + "RadioButtonMenuItemUI",
			"ProgressBarUI", prefix + "ProgressBarUI", 
			"ToolBarUI", prefix + "ToolBarUI", 
			"ColorChooserUI", prefix + "ColorChooserUI",
			"ToolBarSeparatorUI", prefix + "ToolBarSeparatorUI", 
			"TabbedPaneUI", prefix + "TabbedPaneUI", 
			"DesktopPaneUI", prefix + "DesktopPaneUI", 
			"TableUI", prefix + "TableUI", 
			"PanelUI", prefix + "PanelUI",
			"CheckBoxMenuItemUI", prefix + "CheckBoxMenuItemUI",
			"PasswordFieldUI", prefix + "PasswordFieldUI",
			"CheckBoxUI", prefix + "CheckBoxUI", 
			"TableHeaderUI", prefix + "TableHeaderUI", 
			"SplitPaneUI", prefix + "SplitPaneUI",
			"EditorPaneUI",	prefix + "EditorPaneUI", 
			"ListUI", prefix + "ListUI",
			"SpinnerUI", prefix + "SpinnerUI",
			"DesktopIconUI", prefix + "DesktopIconUI", 
			"TextFieldUI", prefix + "TextFieldUI", 
			"TextPaneUI", prefix + "TextPaneUI", 
			"LabelUI", prefix + "LabelUI",
			"ButtonUI", prefix + "ButtonUI", 
			"ToggleButtonUI", prefix + "ToggleButtonUI", 
			"OptionPaneUI",	prefix + "OptionPaneUI", 
			"PopupMenuSeparatorUI", prefix + "PopupMenuSeparatorUI", 
			"RootPaneUI", prefix + "RootPaneUI", 
			"SeparatorUI", prefix + "SeparatorUI" });

	@Override
	public String getName() {
		return "Multiplexing Look and Feel"; //$NON-NLS-1$
	}

	@Override
	public String getID() {
		return "Multiplex"; //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return Messages.getString("swing.B5"); //$NON-NLS-1$
	}

	@Override
	public boolean isNativeLookAndFeel() {
		return false;
	}

	@Override
	public boolean isSupportedLookAndFeel() {
		return true;
	}

	@SuppressWarnings("unchecked")
	public static ComponentUI createUIs(ComponentUI mui, Vector uis,
			JComponent target) {
		
		LookAndFeel[] auxLafs = UIManager.getAuxiliaryLookAndFeels();
		ComponentUI primaryUI = UIManager.getLookAndFeel().getDefaults().getUI(
				target);
		ComponentUI auxiliaryUI;

		if (auxLafs != null) {
			for (LookAndFeel l : auxLafs) {
				auxiliaryUI = l.getDefaults().getUI(target);
				if (auxiliaryUI != null) {
					uis.add(auxiliaryUI);
				}
			}
		}
		if (uis.isEmpty()) {
			return primaryUI;
		}
		uis.add(0, primaryUI);
		return mui;
	}

	@Override
	public UIDefaults getDefaults() {
		return classDefaults;
	}

	@SuppressWarnings("unchecked")
	protected static ComponentUI[] uisToArray(Vector uis) {
		if (uis == null) {
			return new ComponentUI[] {};
		}
		if (uis.isEmpty()) {
			return null;
		}

		return (ComponentUI[]) uis.toArray(new ComponentUI[0]);

	}
}
