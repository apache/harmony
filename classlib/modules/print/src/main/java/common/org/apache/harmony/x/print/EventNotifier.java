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
 * @author Aleksei V. Ivaschenko 
 */ 

package org.apache.harmony.x.print;

import java.util.HashMap;
import java.util.ArrayList;

import javax.print.PrintService;
import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.event.PrintServiceAttributeEvent;
import javax.print.event.PrintServiceAttributeListener;

/*
 * Unified class EventNotifier is used for notifying attribute
 * listeners of print services about attribute events. There is
 * only one instance of EventNotifier which can be used by any
 * number print services. Generally, it is used by instances of
 * DefaultPrintService class.
 */
public class EventNotifier extends Thread {

    private static ArrayList services = new ArrayList();
    private static HashMap listeners = new HashMap();
    private static HashMap attributes = new HashMap();
    private static EventNotifier notifier = new EventNotifier();
    private boolean running = false;
    
    private EventNotifier() {
        setPriority(Thread.NORM_PRIORITY);
        setDaemon(true);
    }

    /*
     * This method returns the only instance of this class.
     */
    public static EventNotifier getNotifier() {
        return notifier;
    }
    
    /*
     * Adds pair service - listener to the map of listeners.
     * Added listener is notified as soon as any attribute
     * event occurs in service. 
     */
    public void addListener(PrintService service,
            PrintServiceAttributeListener listener) {
        if (service == null || listener == null) {
            return;
        }
        
        if (services.contains(service)) {
            ArrayList serviceListeners = (ArrayList)listeners.get(service);
            serviceListeners.add(listener);
        } else {
            services.add(service);
            ArrayList serviceListeners = new ArrayList();
            serviceListeners.add(listener);
            listeners.put(service, serviceListeners);
            PrintServiceAttributeSet serviceAttributes =
                service.getAttributes();
            attributes.put(service, serviceAttributes);
        }

        if (!running) {
            start();
        }
    }

    /*
     * Removes pair service - listener from the map of listeners.
     * Removed listener never receive notifications again, except
     * it is not added again. 
     */
    public void removeListener(PrintService service,
            PrintServiceAttributeListener listener) {
        if (service == null || listener == null) {
            return;
        }
        
        if (services.contains(service)) {
            ArrayList serviceListeners = (ArrayList)listeners.get(service);
            serviceListeners.remove(listener);
            if (serviceListeners.size() == 0) {
                listeners.remove(service);
                attributes.remove(service);
                services.remove(service);
            }
        }

        if (services.size() == 0) {
            running = false;
        }
    }
    
    /*
     * Stops event notifier. While event notifier is stopped,
     * all added listeners do not receive any notifications.
     */
    public void Finish() {
        running = false;
    }
    
    /*
     * Starts event notifier. Event notifier starts automatically
     * when at least one listener added, and stops when all
     * listeners removed.
     */
    public void run() {
        try {
            running = true;
            while (running) {
                Thread.sleep(1000);
                
                for (int i = 0; i < services.size(); i++) {
                    PrintService service = (PrintService)services.get(i);
                    PrintServiceAttributeSet lastSet =
                        (PrintServiceAttributeSet)attributes.get(service);
                    PrintServiceAttributeSet newSet = service.getAttributes();
                    if (!lastSet.equals(newSet)) {
                        PrintServiceAttributeSet updated =
                            getUpdatedAttributeSet(lastSet, newSet);
                        if (updated.size() > 0) {
                            PrintServiceAttributeEvent event =
                                new PrintServiceAttributeEvent(service,updated);
                            ArrayList serviceListeners =
                                (ArrayList)listeners.get(service);
                            for (int j = 0; j < serviceListeners.size(); j++) {
                                PrintServiceAttributeListener listener =
                                    (PrintServiceAttributeListener)
                                    serviceListeners.get(j);
                                listener.attributeUpdate(event);
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException ie) {
            // EventNotifier interrupted.
            running = false;
        }
    }
    
    private PrintServiceAttributeSet getUpdatedAttributeSet(
            PrintServiceAttributeSet oldSet, PrintServiceAttributeSet newSet) {
        Attribute[] newAttributes = newSet.toArray();
        PrintServiceAttributeSet updated = new HashPrintServiceAttributeSet();
        for (int i = 0; i < newAttributes.length; i++) {
            if (!oldSet.containsValue(newAttributes[i])) {
                updated.add(newAttributes[i]);
            }
        }
        return updated;
    }
}
