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
 * @author Alexander T. Simbirtsev
 */
package javax.swing.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.EventListener;

public class EventListenerList implements Serializable {

    protected transient Object[] listenerList = new Object[0];

    public synchronized <T extends java.util.EventListener> void remove(
            final Class<T> listenerClass,
            final T listener
    ) {
        if (listener == null) {
            return;
        }

        int position = -1;
        for(int i = listenerList.length-1; i > 0; i -= 2) {
            if (listenerClass == listenerList[i-1] && listener.equals(listenerList[i])) {
                position = i - 1;
                break;
            }
        }
        if (position >= 0) {
            Object[] newList = new Object[listenerList.length-2];
            System.arraycopy(listenerList, 0, newList, 0, position);
            System.arraycopy(listenerList, position + 2, newList, position,
                             listenerList.length - position - 2);

            listenerList = newList;
        }
    }

    public synchronized <T extends java.util.EventListener> void add(
            final Class<T> listenerClass,
            final T listener
    ) {
        if (listener == null) {
            return;
        }

        Object[] newList = new Object[listenerList.length+2];
        System.arraycopy(listenerList, 0, newList, 0, listenerList.length);
        newList[listenerList.length] = listenerClass;
        newList[listenerList.length+1] = listener;

        listenerList = newList;
    }

    public <T extends java.util.EventListener> T[] getListeners(final Class<T> listenerClass) {
        int numClassListeners = getListenerCount(listenerClass);
        T[] listeners = (T[]) (Array.newInstance(
                listenerClass, numClassListeners));
        if (numClassListeners > 0) {
            for (int innerIndex = 0, outerIndex = 0;
                    outerIndex < numClassListeners; innerIndex += 2) {

                if (listenerList[innerIndex] == listenerClass) {
                    listeners[numClassListeners - 1 - outerIndex] = (T) listenerList[innerIndex + 1];
                    ++outerIndex;
                }
            }
        }
        return listeners;
    }

    /*
     * The format of the string is based on 1.5 release behavior
     * which can be revealed using the following code:
     *
     *     EventListenerList obj = new EventListenerList();
     *     obj.add(BasicButtonListener.class, new BasicButtonListener(new JButton()));
     *     System.out.println(obj.toString());
     */
    public String toString() {
        String str = "EventListenerList: ";
        str += getListenerCount() + " listeners:";
        for(int i = 0; i < listenerList.length; i += 2) {
            str += " type " + ((Class)listenerList[i]).getName() +
                   " listener " + listenerList[i+1].toString();
        }
        return str;
    }

    public Object[] getListenerList() {
        return listenerList;
    }

    public int getListenerCount(final Class<?> listenerClass) {
        int counter = 0;
        for (int i = 0; i < listenerList.length; i += 2){
            if (listenerList[i] == listenerClass) {

                counter++;
            }
        }
        return counter;
    }

    public int getListenerCount() {
        return listenerList.length >> 1;
    }

    private void writeObject(final ObjectOutputStream outStream) throws IOException {
        outStream.defaultWriteObject();

        for (int i = 0; i < listenerList.length; i += 2) {
            Object listener = listenerList[i+1];
            if ((listener != null) && (listener instanceof Serializable)) {
                outStream.writeObject(listenerList[i]);
                outStream.writeObject(listener);
            }
        }
        outStream.writeObject(null);
    }


    private void readObject(final ObjectInputStream inStream) throws IOException,
                                                        ClassNotFoundException {
        inStream.defaultReadObject();

        ArrayList list = new ArrayList();
        Object markerObject = null;
        while ((markerObject = inStream.readObject()) != null) {
            list.add(markerObject);
            list.add(inStream.readObject());
        }
        listenerList = list.toArray();
    }

}

