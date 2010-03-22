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
 * @author Sergey Burlak
 */
package javax.swing.plaf.metal;

import java.awt.Font;
import java.util.Properties;
import javax.swing.SwingTestCase;
import javax.swing.plaf.FontUIResource;

public class DefaultMetalThemeTest extends SwingTestCase {
    private DefaultMetalTheme theme;

    private Properties props;

    @Override
    public void setUp() {
        theme = new DefaultMetalTheme();
        props = new Properties();
        props.putAll(System.getProperties());
    }

    @Override
    public void tearDown() {
        theme = null;
        System.getProperties().clear();
        System.getProperties().putAll(props);
    }

    public void testGetSystemTextFontFromProperty() {
        String font = "aaa-bold-10";
        System.setProperty("swing.plaf.metal.systemFont", font);
        assertEquals(new FontUIResource(Font.decode(font)), theme.getSystemTextFont());
    }

    public void testGetControlTextFontFromProperty() {
        String font = "test-plain-5";
        System.setProperty("swing.plaf.metal.controlFont", font);
        assertEquals(new FontUIResource(Font.decode(font)), theme.getControlTextFont());
    }

    public void testGetSmallTextFontFromProperty() {
        String font = "aaa-bold-3";
        System.setProperty("swing.plaf.metal.smallFont", font);
        assertEquals(new FontUIResource(Font.decode(font)), theme.getSubTextFont());
    }

    public void testGetUserTextFontFromProperty() {
        String font = "aaa-bold-1";
        System.setProperty("swing.plaf.metal.userFont", font);
        assertEquals(new FontUIResource(Font.decode(font)), theme.getUserTextFont());
    }

    public void testGetWindowTitleFont() {
        assertEquals(new FontUIResource("Dialog", Font.BOLD, 12), theme.getWindowTitleFont());
    }

    public void testGetUserTextFont() {
        assertEquals(new FontUIResource("Dialog", Font.PLAIN, 12), theme.getUserTextFont());
    }

    public void testGetSystemTextFont() {
        assertEquals(new FontUIResource("Dialog", Font.PLAIN, 12), theme.getSystemTextFont());
    }

    public void testGetSubTextFont() {
        assertEquals(new FontUIResource("Dialog", Font.PLAIN, 10), theme.getSubTextFont());
    }

    public void testGetMenuTextFont() {
        assertEquals(new FontUIResource("Dialog", Font.BOLD, 12), theme.getMenuTextFont());
    }

    public void testGetControlTextFont() {
        assertEquals(new FontUIResource("Dialog", Font.BOLD, 12), theme.getControlTextFont());
    }

    public void testGetSecondary3() {
        assertNotNull(theme.getSecondary3());
    }

    public void testGetSecondary2() {
        assertNotNull(theme.getSecondary2());
    }

    public void testGetSecondary1() {
        assertNotNull(theme.getSecondary1());
    }

    public void testGetPrimary3() {
        assertNotNull(theme.getPrimary3());
    }

    public void testGetPrimary2() {
        assertNotNull(theme.getPrimary2());
    }

    public void testGetPrimary1() {
        assertNotNull(theme.getPrimary1());
    }

    public void testGetName() {
        assertNotNull(theme.getName());
    }
}
