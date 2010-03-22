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

package org.apache.harmony.awt.text;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.text.Element;
import javax.swing.text.View;

public abstract class TextFactory {
    private static final String FACTORY_IMPL_CLS_NAME =
        "javax.swing.text.TextFactoryImpl"; //$NON-NLS-1$

    private static final TextFactory viewFactory = createTextFactory();

    public static TextFactory getTextFactory() {
        return viewFactory;
    }

    private static TextFactory createTextFactory() {
        PrivilegedAction<TextFactory> createAction = new PrivilegedAction<TextFactory>() {
            public TextFactory run() {
                try {
                    Class<?> factoryImplClass = Class
                        .forName(FACTORY_IMPL_CLS_NAME);
                    Constructor<?> defConstr =
                        factoryImplClass.getDeclaredConstructor(new Class[0]);
                    defConstr.setAccessible(true);
                    return (TextFactory)defConstr.newInstance(new Object[0]);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();

                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }  catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        return AccessController.doPrivileged(createAction);
    }

    public abstract RootViewContext createRootView(final Element element);

    public abstract View createPlainView(final Element e);

    public abstract View createWrappedPlainView(final Element e);

    public abstract View createFieldView(final Element e);

    public abstract View createPasswordView(Element e);

    public abstract TextCaret createCaret();
}
