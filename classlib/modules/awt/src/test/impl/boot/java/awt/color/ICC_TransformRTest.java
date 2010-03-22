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

package java.awt.color;

import org.apache.harmony.awt.gl.color.ICC_Transform;

import junit.framework.TestCase;

public class ICC_TransformRTest extends TestCase {

	public void testHarmony_4509() {
		new ICC_Transform(new ICC_Profile[] {
				ICC_Profile.getInstance(ColorSpace.CS_sRGB),
				ICC_Profile.getInstance(ColorSpace.CS_GRAY),
				ICC_Profile.getInstance(ColorSpace.CS_CIEXYZ),
                ICC_Profile.getInstance(ColorSpace.CS_LINEAR_RGB) });

        //TODO PYCC profile isn't supported yet
        //new ICC_Transform(new ICC_Profile[] {
        //        ICC_Profile.getInstance(ColorSpace.CS_PYCC) });
	}
}
