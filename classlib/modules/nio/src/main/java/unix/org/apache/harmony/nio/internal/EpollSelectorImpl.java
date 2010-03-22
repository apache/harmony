/* Licensed to the Apache Software Foundation (ASF) under one or more
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
package org.apache.harmony.nio.internal;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelectionKey;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.harmony.luni.platform.FileDescriptorHandler;

/*
 * Default implementation of java.nio.channels.Selector
 * 
 */
final class EpollSelectorImpl extends AbstractSelector {

    private static final int MOCK_WRITEBUF_SIZE = 1;

    private static final int MOCK_READBUF_SIZE = 8;

    private static final int NA = 0;

    private static final int READABLE = 1;

    private static final int WRITABLE = 2;

    private static final int SELECT_BLOCK = -1;

    private static final int SELECT_NOW = 0;

    // keysLock is used to brief synchronization when get selectionKeys snapshot
    // before selection
    final Object keysLock = new Object();

    boolean keySetChanged = true;

    private SelectionKey[] keys = new SelectionKey[1];

    private final Set<SelectionKey> keysSet = new HashSet<SelectionKey>();

    private Set<SelectionKey> unmodifiableKeys = Collections
            .unmodifiableSet(keysSet);

    private final Set<SelectionKey> selectedKeys = new HashSet<SelectionKey>();

    private Set<SelectionKey> unaddableSelectedKeys = new UnaddableSet<SelectionKey>(
            selectedKeys);

    // sink and source are used by wakeup()
    private Pipe.SinkChannel sink;

    private Pipe.SourceChannel source;

    private FileDescriptor sourcefd;

    private int[] keyFDs;

    private int[] readyFDs;

    private int[] readyOps;

    private int keysCount = 0;

    private long epollFD;

    private int countReady;

    public Class fileDescriptorClass;

    static native int resolveFD(Class cfd, FileDescriptor ofd);

    static native long prepare();

    static native long addFileDescriptor(long epollFD, int mode, int fd);

    static native long delFileDescriptor(long epollFD, long fd);

    static native int epoll(long epollFD, int count, int[] FDs, int[] ops,
            long timeout);

    private InternalKeyMap<EpollSelectionKeyImpl> quickMap = new InternalKeyMap<EpollSelectionKeyImpl>();

    public EpollSelectorImpl(SelectorProvider selectorProvider) {
        super(selectorProvider);
        try {
            Pipe mockSelector = selectorProvider.openPipe();
            sink = mockSelector.sink();
            source = mockSelector.source();
            sourcefd = ((FileDescriptorHandler) source).getFD();
            source.configureBlocking(false);

            fileDescriptorClass = sourcefd.getClass();

            keyFDs = new int[1];
            readyFDs = new int[1];
            readyOps = new int[1];

            // register sink channel
            keyFDs[0] = resolveFD(fileDescriptorClass, sourcefd);
            keys[0] = source.keyFor(this);
            epollFD = prepare();

            keysCount = 1;

            quickMap.put(keyFDs[0], (EpollSelectionKeyImpl) keys[0]);
            addFileDescriptor(epollFD, 1, keyFDs[0]);

        } catch (IOException e) {
            // do nothing
        }
    }

    /*
     * @see java.nio.channels.spi.AbstractSelector#implCloseSelector()
     */
    protected void implCloseSelector() throws IOException {
        synchronized (this) {
            synchronized (keysSet) {
                synchronized (selectedKeys) {
                    doCancel();
                    for (int c = 0; c < keysCount; c++) {
                        if (keys[c] != null) {
                            deregister((AbstractSelectionKey) keys[c]);
                        }
                    }
                    wakeup();
                }
            }
        }
    }

    private void ensureCapacity(int c) {
        // TODO: rewrite array handling as some internal class
        if (c >= keys.length) {
            SelectionKey[] t = new SelectionKey[(keys.length + 1) << 1];
            System.arraycopy(keys, 0, t, 0, keys.length);
            keys = t;
        }

        if (c >= readyFDs.length) {
            int[] t = new int[(readyFDs.length + 1) << 1];
            System.arraycopy(readyFDs, 0, t, 0, readyFDs.length);
            readyFDs = t;
        }

        if (c >= keyFDs.length) {
            int[] t = new int[(keyFDs.length + 1) << 1];
            System.arraycopy(keyFDs, 0, t, 0, keyFDs.length);
            keyFDs = t;
        }

        if (c >= readyOps.length) {
            int[] t = new int[(readyOps.length + 1) << 1];
            System.arraycopy(readyOps, 0, t, 0, readyOps.length);
            readyOps = t;
        }
    }

    private void limitCapacity() {
        // TODO: implement array squeezing
    }

    /**
     * Adds the specified key to storage and updates the indexes accordingly
     * 
     * @param sk
     *            key to add
     * @return index in the storage
     */
    private int addKey(SelectionKey sk) {

        // make sure that enough space is available
        ensureCapacity(keysCount);

        // get channel params
        int ops = sk.interestOps();
        int fd = resolveFD(fileDescriptorClass, ((FileDescriptorHandler) sk
                .channel()).getFD());

        int eops = 0;
        if (((SelectionKey.OP_READ | SelectionKey.OP_ACCEPT) & ops) != 0) {
            eops = eops + READABLE;
        }
        ;
        if (((SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT) & ops) != 0) {
            eops = eops + WRITABLE;
        }
        ;

        keys[keysCount] = sk;
        keyFDs[keysCount] = fd;

        quickMap.put(fd, (EpollSelectionKeyImpl) sk);
        addFileDescriptor(epollFD, eops, fd);

        return keysCount++;
    }

    /**
     * Deletes the key from the internal storage and updates the indexes
     * accordingly
     * 
     * @param sk
     *            key to delete
     */
    private void delKey(SelectionKey sk) {

        // get the key index in the internal storage
        int index = ((EpollSelectionKeyImpl) sk).getIndex();

        // deregister FD in native
        delFileDescriptor(epollFD, keyFDs[index]);

        if (quickMap.remove(keyFDs[index]) == null) {
            throw new RuntimeException();
        }
        // key is null now
        keys[index] = null;

        // key compaction to ensure lack of holes
        // we can simply exchange latest and current keys
        if (keys[keysCount - 1] != null) {
            keys[index] = keys[keysCount - 1];
            keys[keysCount - 1] = null;

            keyFDs[index] = keyFDs[keysCount - 1];
            keyFDs[keysCount - 1] = -1;

            // update key index
            ((EpollSelectionKeyImpl) keys[index]).setIndex(index);
        }
        keysCount--;
    }

    /**
     * 
     * @param sk
     */
    void modKey(SelectionKey sk) {
        // TODO: update indexes rather than recreate the key
        synchronized (keysSet) {
            delKey(sk);
            addKey(sk);
        }
    }

    /*
     * @see java.nio.channels.spi.AbstractSelector#register(java.nio.channels.spi.AbstractSelectableChannel,
     *      int, java.lang.Object)
     */
    protected SelectionKey register(AbstractSelectableChannel channel,
            int operations, Object attachment) {
        if (!provider().equals(channel.provider())) {
            throw new IllegalSelectorException();
        }
        synchronized (this) {
            synchronized (keysSet) {

                // System.out.println("Registering channel");
                // create the key
                SelectionKey sk = new EpollSelectionKeyImpl(channel,
                        operations, attachment, this);

                int index = addKey(sk);
                ((EpollSelectionKeyImpl) sk).setIndex(index);

                // System.out.println(" channel registered with index = " +
                // index);
                return sk;
            }
        }
    }

    /*
     * @see java.nio.channels.Selector#keys()
     */
    public synchronized Set<SelectionKey> keys() {
        closeCheck();

        keysSet.clear();

        if (keys.length != keysCount) {
            SelectionKey[] chompedKeys = new SelectionKey[keysCount];
            System.arraycopy(keys, 0, chompedKeys, 0, keysCount);
            keysSet.addAll(Arrays.asList(chompedKeys));
        } else {
            keysSet.addAll(Arrays.asList(keys));
        }

        keysSet.remove(source.keyFor(this));
        return unmodifiableKeys;
    }

    private void closeCheck() {
        if (!isOpen()) {
            throw new ClosedSelectorException();
        }
    }

    /*
     * @see java.nio.channels.Selector#select()
     */
    public int select() throws IOException {
        return selectInternal(SELECT_BLOCK);
    }

    /*
     * @see java.nio.channels.Selector#select(long)
     */
    public int select(long timeout) throws IOException {
        if (timeout < 0) {
            throw new IllegalArgumentException();
        }
        return selectInternal((0 == timeout) ? SELECT_BLOCK : timeout);
    }

    /*
     * @see java.nio.channels.Selector#selectNow()
     */
    public int selectNow() throws IOException {
        return selectInternal(SELECT_NOW);
    }

    private int selectInternal(long timeout) throws IOException {
        closeCheck();
        synchronized (this) {
            synchronized (keysSet) {
                synchronized (selectedKeys) {
                    doCancel();
                    boolean isBlock = (SELECT_NOW != timeout);
                    try {
                        if (isBlock) {
                            begin();
                        }
                        // System.out.println("calling native epoll(): keysCount
                        // = " + keysCount + ", readyFDs.length = " +
                        // readyFDs.length + ", readyOps.length = " +
                        // readyOps.length);
                        countReady = epoll(epollFD, keysCount, readyFDs,
                                readyOps, timeout);
                        // System.out.println(" returns " + countReady);
                    } finally {
                        if (isBlock) {
                            end();
                        }
                    }
                    return processSelectResult();
                }
            }
        }
    }

    private boolean isConnected(EpollSelectionKeyImpl key) {
        SelectableChannel channel = key.channel();
        if (channel instanceof SocketChannel) {
            return ((SocketChannel) channel).isConnected();
        }
        return true;
    }

    /*
     * Analyses selected channels and adds keys of ready channels to
     * selectedKeys list.
     * 
     * readyChannels are encoded as concatenated array of flags for readable
     * channels followed by writable channels.
     */
    private int processSelectResult() throws IOException {
        if (0 == countReady) {
            return 0;
        }
        if (-1 == countReady) {
            return 0;
        }
        // if the mock channel is selected, read the content.
        if (READABLE == readyOps[0]) {
            ByteBuffer readbuf = ByteBuffer.allocate(MOCK_READBUF_SIZE);
            while (source.read(readbuf) > 0) {
                readbuf.flip();
            }
        }
        int selected = 0;

        EpollSelectionKeyImpl key = null;
        for (int i = 0; i < countReady; i++) {

            // System.out.println("processSelectResults(): mapping readyFDs[" +
            // i + "]");
            // Lookup the key, map the index in readyFDs to real key
            key = (EpollSelectionKeyImpl) quickMap.get(readyFDs[i]);

            if (null == key) {
                continue;
            }
            // System.out.println(" ready key = " + key.getIndex());

            int ops = key.interestOps();
            int selectedOp = 0;

            if ((readyOps[i] & READABLE) != 0) {
                selectedOp = (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)
                        & ops;
            }

            if ((readyOps[i] & WRITABLE) != 0) {
                if (isConnected(key)) {
                    selectedOp = selectedOp | (SelectionKey.OP_WRITE & ops);
                } else {
                    selectedOp = selectedOp | (SelectionKey.OP_CONNECT & ops);
                }
            }

            if (0 != selectedOp) {
                if (selectedKeys.contains(key)) {
                    if (key.readyOps() != selectedOp) {
                        key.setReadyOps(key.readyOps() | selectedOp);
                        selected++;
                    }
                } else {
                    key.setReadyOps(selectedOp);
                    selectedKeys.add(key);
                    selected++;
                }
            }

        }

        // System.out.println("processSelectResult(): selected = " + selected);
        return selected;
    }

    /*
     * @see java.nio.channels.Selector#selectedKeys()
     */
    public synchronized Set<SelectionKey> selectedKeys() {
        closeCheck();
        return unaddableSelectedKeys;
    }

    private void doCancel() {
        Set<SelectionKey> cancelledKeys = cancelledKeys();
        synchronized (cancelledKeys) {
            if (cancelledKeys.size() > 0) {
                for (SelectionKey currentkey : cancelledKeys) {
                    delKey(currentkey);
                    deregister((AbstractSelectionKey) currentkey);
                    selectedKeys.remove(currentkey);
                }
            }
            cancelledKeys.clear();
            limitCapacity();
        }
    }

    /*
     * @see java.nio.channels.Selector#wakeup()
     */
    public Selector wakeup() {
        try {
            sink.write(ByteBuffer.allocate(MOCK_WRITEBUF_SIZE));
        } catch (IOException e) {
            // do nothing
        }
        return this;
    }

    private static class InternalKeyMap<E> {

        Entry<E>[] storage;

        int size;

        Entry deleted = new Entry(-1, null);

        static final int threshRatio = 4;

        public InternalKeyMap() {
            this(1);
        }

        public InternalKeyMap(int initialSize) {
            storage = new Entry[initialSize];
            size = 0;
        }

        private E putEntryNoCheck(Entry<E>[] storage, int key, Entry<E> entry) {

            for (int tryCount = 0; tryCount < storage.length; tryCount++) {

                int hash = hash(key, tryCount);
                int index = hash % storage.length;

                // System.out.println("put: hash: " + hash + ", index: " + index
                // + ", key: " + key + ", size: " + size + ", tryCount: " +
                // tryCount + ", storage.length=" + storage.length);

                if (storage[index] == null) {
                    if (entry != deleted) {
                        storage[index] = entry;
                    }
                    return null;
                } else if (storage[index].key == key
                        || (storage[index] == deleted && entry != deleted)) {
                    E t = storage[index].value;
                    storage[index] = entry;
                    return t;
                }
            }

            throw new ArrayIndexOutOfBoundsException(key);
        }

        private E putEntry(int key, Entry<E> entry) {
            if (size >= storage.length
                    || (storage.length / (storage.length - size)) >= threshRatio) {
                rehash();
            }

            E result = putEntryNoCheck(storage, key, entry);
            if (result == null) {
                size++;
            }

            return result;
        }

        public void put(int key, E value) {
            Entry<E> t = new Entry<E>(key, value);
            putEntry(key, t);
        }

        public E remove(int key) {
            E result = putEntryNoCheck(storage, key, deleted);

            if (result != null) {
                size--;
            }

            return result;
        }

        public E get(int key) {
            if (storage == null) {
                // System.out.println(" FAIL, storage=null");
                return null;
            }

            for (int tryCount = 0; tryCount < storage.length; tryCount++) {
                int hash = hash(key, tryCount);
                int index = hash % storage.length;

                // System.out.println("get: hash: " + hash + ", index: " + index
                // + ", key: " + key + ", size: " + size + ", tryCount: " +
                // tryCount + ", storage.length=" + storage.length);

                if (storage[index] == null) {
                    // System.out.println("Lookup FAIL, reached end");
                    return null;
                }

                if (storage[index].key == key) {
                    // System.out.println("Lookup OK!");
                    return storage[index].value;
                }

            }
            // System.out.println(" FAIL, tryCount > storage.length");
            return null;
        }

        private void rehash() {
            Entry<E>[] newStorage = new Entry[storage.length << 1];
            int newSize = 0;
            for (int c = 0; c < storage.length; c++) {
                if (storage[c] == null)
                    continue;
                if (storage[c] == deleted)
                    continue;
                putEntryNoCheck(newStorage, storage[c].key, storage[c]);
                newSize++;
            }
            storage = newStorage;
            size = newSize;
        }

        private int hash(int key, int tryCount) {
            int t1 = key * 31 + 1;
            int t2 = 2 * key + 1;
            return (t1 + (t2 * tryCount)) & 0x7FFFFFFF;
        }

        private static class Entry<E> {

            final int key;

            final E value;

            public Entry(int iKey, E iValue) {
                key = iKey;
                value = iValue;
            }

        }

    }

    private static class UnaddableSet<E> implements Set<E> {

        private Set<E> set;

        UnaddableSet(Set<E> set) {
            this.set = set;
        }

        public boolean equals(Object object) {
            return set.equals(object);
        }

        public int hashCode() {
            return set.hashCode();
        }

        public boolean add(E object) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            set.clear();
        }

        public boolean contains(Object object) {
            return set.contains(object);
        }

        public boolean containsAll(Collection<?> c) {
            return set.containsAll(c);
        }

        public boolean isEmpty() {
            return set.isEmpty();
        }

        public Iterator<E> iterator() {
            return set.iterator();
        }

        public boolean remove(Object object) {
            return set.remove(object);
        }

        public boolean removeAll(Collection<?> c) {
            return set.removeAll(c);
        }

        public boolean retainAll(Collection<?> c) {
            return set.retainAll(c);
        }

        public int size() {
            return set.size();
        }

        public Object[] toArray() {
            return set.toArray();
        }

        public <T> T[] toArray(T[] a) {
            return set.toArray(a);
        }
    }

}
