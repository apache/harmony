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
 * @author Intel, Evgueni Brevnov
 */  
/**
 * This file is a part of tool library.
 */
namespace tl {

    /**
     * Linked list interface.
     */
    template <class _ListElement,
        class _Allocator = DefaultAllocator,
        class _RWLock = DefaultRWLock>
    class List
    {
        _ReadWriteLock m_readWriteLock;
        _E
        typedef ListElement<_ReadWriteLock> ListElementType;
        ListElementType* m_next, m_prev;
    public:
        void SetNextElement(ListElement* element_ptr) {
            g_readWriteLock.lockForWrite();
            m_next = element_ptr;
            element_ptr->prev = *this;
            g_readWriteLock.unlock();
        }

        void SetPrevElement(ListElement* element_ptr) {
            g_readWriteLock.lockForWrite();
            m_prev = element_ptr;
            element_ptr->next = *this;
            g_readWriteLock.unlock();
        }

        void Remove() {
            g_readWriteLock.lockForWrite();
            m_prev->m_next = m_next;
            m_next->m_prev = m_prev;
            g_readWriteLock.unlock();
        }

        bool Contains(_Key _key);
        /**
        * Return an iterator. Collection should be locked
        * to iterate over the iterator.
        * Destruction of iterator unlocks collecton.
        */
        Iterator GetIterator();
    } // ListElement
} // tl
