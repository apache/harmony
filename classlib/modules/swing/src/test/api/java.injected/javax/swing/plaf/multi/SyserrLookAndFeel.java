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

import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;

public class SyserrLookAndFeel extends LookAndFeel {

	UIDefaults uiDefaults;

	@Override
	public String getName() {
		return "SyserrLaf"; //$NON-NLS-1$
	}

	@Override
	public String getID() {
		return "SyserrLaf"; //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return "Look and feel for testing Muttiplexing laf"; //$NON-NLS-1$
	}

	@Override
	public boolean isNativeLookAndFeel() {
		return false;
	}

	@Override
	public boolean isSupportedLookAndFeel() {
		return true;
	}

	@Override
	public UIDefaults getDefaults() {

		return new UIDefaults(new Object[] { "ButtonUI", //$NON-NLS-1$
				"javax.swing.plaf.multi.SyserrButtonUI", }) {//$NON-NLS-1$
			@Override
			protected void getUIError(String s) {
				// Remove unneded mesage
			}
		};
	}
}
