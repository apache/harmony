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
package java.awt;

import java.io.*;

import junit.framework.TestCase;

/**
 * ComponentSerialize
 */
public class ComponentSerialize extends TestCase {


    @SuppressWarnings("serial")
    static final class MyComponent extends Component {}

    public void testAll() throws IOException, ClassNotFoundException {
        writeAndRead(new MyComponent());
        writeAndRead(new Container());
        writeAndRead(new Canvas());
        writeAndRead(new Panel());
        writeAndRead(new Frame());
        writeAndRead(new Dialog(new Frame()));
    }


    public void testReadWriteComponent()
                throws IOException, ClassNotFoundException {
        MyComponent c = new MyComponent();
        MyComponent t = (MyComponent)writeAndRead(c);
        compareComponents(c, t);
    }

    public void testReadWriteFrame()
                throws IOException, ClassNotFoundException {
        Frame c = new Frame("Frame");
        Frame t = (Frame)writeAndRead(c);
        compareComponents(c, t);

        assertEquals(c.getTitle(), t.getTitle());

        assertNotNull(t.getWindowListeners());
        assertNotNull(t.getWindowStateListeners());
    }



    private void compareComponents(Component original, Component restored) {

        // assertEquals(t, c);
        assertEquals(restored.getClass(), original.getClass());
        assertEquals(restored.getBounds(), original.getBounds());

        // verify transient fields
        assertEquals(restored.toolkit, original.toolkit);

        assertNotNull(restored.behaviour);
        assertEquals(restored.behaviour.getClass(), original.behaviour.getClass());

        assertNotNull(restored.getComponentListeners());
        assertNotNull(restored.getFocusListeners());
        assertNotNull(restored.getHierarchyListeners());
        assertNotNull(restored.getHierarchyBoundsListeners());
        assertNotNull(restored.getKeyListeners());
        assertNotNull(restored.getMouseListeners());
        assertNotNull(restored.getMouseMotionListeners());
        assertNotNull(restored.getMouseWheelListeners());
        assertNotNull(restored.getInputMethodListeners());

    }

    private Component writeAndRead(Component original)
                throws IOException, ClassNotFoundException {
        File tempFile = File.createTempFile("save", ".object");
        tempFile.deleteOnExit();

        FileOutputStream fos = new FileOutputStream(tempFile);

        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(original);
        oos.close();


        FileInputStream fis = new FileInputStream(tempFile);
        ObjectInputStream ois = new ObjectInputStream(fis);

        Component restored = (Component)ois.readObject();
        ois.close();

        return restored;
    }
}

