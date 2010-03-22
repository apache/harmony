/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.lang.management;

import java.lang.management.ManagementPermission;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedList;
import java.util.List;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

/**
 * Runtime type for {@link MemoryMXBean}.
 * <p>
 * Implementation note. This type of bean is both dynamic and a notification
 * emitter. The dynamic behaviour comes courtesy of the
 * {@link org.apache.harmony.lang.management.DynamicMXBeanImpl} superclass while the
 * notifying behaviour uses a delegation approach to a private member that
 * implements the {@link javax.management.NotificationEmitter} interface.
 * Because multiple inheritance is not supported in Java it was a toss up which
 * behaviour would be based on inheritence and which would use delegation. Every
 * other <code>*MXBeanImpl</code> class in this package inherits from the
 * abstract base class <code>DynamicMXBeanImpl</code> so that seemed to be the
 * natural approach for this class too. By choosing not to make this class a
 * subclass of {@link javax.management.NotificationBroadcasterSupport}, the
 * protected
 * <code>handleNotification(javax.management.NotificationListener, javax.management.Notification, java.lang.Object)</code>
 * method cannot be overridden for any custom notification behaviour. However,
 * taking the agile mantra of <b>YAGNI </b> to heart, it was decided that the
 * default implementation of that method will suffice until new requirements
 * prove otherwise.
 * </p>
 * 
 * @since 1.5
 */
public final class MemoryMXBeanImpl extends DynamicMXBeanImpl implements
        java.lang.management.MemoryMXBean, NotificationEmitter {

    /**
     * The delegate for all notification management.
     */
    private NotificationBroadcasterSupport notifier = new NotificationBroadcasterSupport();

    private static MemoryMXBeanImpl instance = new MemoryMXBeanImpl();

    private List<MemoryManagerMXBean> memoryManagerList;

    /**
     * Constructor intentionally private to prevent instantiation by others.
     * Sets the metadata for this bean.
     */
    private MemoryMXBeanImpl() {
        setMBeanInfo(ManagementUtils
                .getMBeanInfo(java.lang.management.MemoryMXBean.class.getName()));
        memoryManagerList = new LinkedList<MemoryManagerMXBean>();
        createMemoryManagers();
    }

    /**
     * Singleton accessor method.
     * 
     * @return the <code>ClassLoadingMXBeanImpl</code> singleton.
     */
    static MemoryMXBeanImpl getInstance() {
        return instance;
    }

    /**
     * Instantiates MemoryManagerMXBean and GarbageCollectorMXBean instance(s)
     * for the current VM configuration and stores them in memoryManagerList.
     */
    private native void createMemoryManagers();

    /**
     * A helper method called from within the native
     * {@link #createMemoryManagers()} method to construct new instances of
     * MemoryManagerMXBean and GarbageCollectorMXBean and add them to the
     * {@link #memoryManagerList}.
     * 
     * @param name
     *            the name of the corresponding memory manager
     * @param internalID
     *            numerical identifier associated with the memory manager for
     *            the benefit of the VM
     * @param isGC
     *            boolean indication of the memory manager type.
     *            <code>true</code> indicates that the runtime type of the
     *            object to be created is
     *            <code>GarbageCollectorMXBeanImpl</code> while
     *            <code>false</code> indicates a
     *            <code>MemoryManagerMXBeanImpl</code>
     */
    @SuppressWarnings("unused")
    // IMPORTANT: for use by VM
    private void createMemoryManagerHelper(String name, int internalID,
            boolean isGC) {
        if (isGC) {
            memoryManagerList.add(new GarbageCollectorMXBeanImpl(name,
                    internalID, this));
        } else {
            memoryManagerList.add(new MemoryManagerMXBeanImpl(name, internalID,
                    this));
        }
    }

    /**
     * Retrieves the list of memory manager beans in the system.
     * 
     * @return the list of <code>MemoryManagerMXBean</code> instances
     */
    List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
        return memoryManagerList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryMXBean#gc()
     */
    public void gc() {
        System.gc();
    }

    /**
     * @return an instance of {@link MemoryUsage}which can be interrogated by
     *         the caller.
     * @see #getHeapMemoryUsage()
     */
    private native MemoryUsage getHeapMemoryUsageImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryMXBean#getHeapMemoryUsage()
     */
    public MemoryUsage getHeapMemoryUsage() {
        return this.getHeapMemoryUsageImpl();
    }

    /**
     * @return an instance of {@link MemoryUsage}which can be interrogated by
     *         the caller.
     * @see #getNonHeapMemoryUsage()
     */
    private native MemoryUsage getNonHeapMemoryUsageImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryMXBean#getNonHeapMemoryUsage()
     */
    public MemoryUsage getNonHeapMemoryUsage() {
        return this.getNonHeapMemoryUsageImpl();
    }

    /**
     * @return the number of objects awaiting finalization.
     * @see #getObjectPendingFinalizationCount()
     */
    private native int getObjectPendingFinalizationCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryMXBean#getObjectPendingFinalizationCount()
     */
    public int getObjectPendingFinalizationCount() {
        return this.getObjectPendingFinalizationCountImpl();
    }

    /**
     * @return <code>true</code> if verbose output is being produced ;
     *         <code>false</code> otherwise.
     * @see #isVerbose()
     */
    private native boolean isVerboseImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryMXBean#isVerbose()
     */
    public boolean isVerbose() {
        return this.isVerboseImpl();
    }

    /**
     * @param value
     *            <code>true</code> enables verbose output ;
     *            <code>false</code> disables verbose output.
     * @see #setVerbose(boolean)
     */
    private native void setVerboseImpl(boolean value);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryMXBean#setVerbose(boolean)
     */
    public void setVerbose(boolean value) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("control"));
        }
        this.setVerboseImpl(value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.NotificationEmitter#removeNotificationListener(javax.management.NotificationListener,
     *      javax.management.NotificationFilter, java.lang.Object)
     */
    public void removeNotificationListener(NotificationListener listener,
            NotificationFilter filter, Object handback)
            throws ListenerNotFoundException {
        notifier.removeNotificationListener(listener, filter, handback);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener,
     *      javax.management.NotificationFilter, java.lang.Object)
     */
    public void addNotificationListener(NotificationListener listener,
            NotificationFilter filter, Object handback)
            throws IllegalArgumentException {
        notifier.addNotificationListener(listener, filter, handback);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
     */
    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        notifier.removeNotificationListener(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.NotificationBroadcaster#getNotificationInfo()
     */
    public MBeanNotificationInfo[] getNotificationInfo() {
        // We know what kinds of notifications we can emit whereas the
        // notifier delegate does not. So, for this method, no delegating.
        // Instead respond using our own metadata.
        return this.getMBeanInfo().getNotifications();
    }

    /*
     * (non-Javadoc)
     * 
     * Send notifications to registered listeners. This will be called when
     * either of the following situations occur: <ol><li> With the method
     * {@link java.lang.management.MemoryPoolMXBean#isUsageThresholdSupported()}
     * returning <code> true </code> , a memory pool increases its size and, in
     * doing so, reaches or exceeds the usage threshold value. In this case the
     * notification type will be
     * {@link MemoryNotificationInfo#MEMORY_THRESHOLD_EXCEEDED}. <li> With the
     * method
     * {@link java.lang.management.MemoryPoolMXBean#isCollectionUsageThresholdSupported()}
     * returning <code> true </code> , a garbage-collected memory pool has
     * reached or surpassed the collection usage threshold value after a system
     * garbage collection has taken place. In this case the notification type
     * will be
     * {@link MemoryNotificationInfo#MEMORY_COLLECTION_THRESHOLD_EXCEEDED}.
     * </ol>
     * 
     * @param notification For this type of bean the user data will consist of a
     * {@link CompositeData}instance that represents a
     * {@link MemoryNotificationInfo}object.
     */
    public void sendNotification(Notification notification) {
        notifier.sendNotification(notification);
    }
}
