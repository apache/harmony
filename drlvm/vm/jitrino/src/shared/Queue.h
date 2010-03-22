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
 * @author Pavel A. Ozhdikhin
 */

#ifndef _QUEUE_H_
#define _QUEUE_H_

#include "MemoryManager.h"
#include "Dlink.h"

namespace Jitrino {

class queueImpl
{ 
public:
    queueImpl(MemoryManager& m) : mm(m) {}
    void* pop() {
        if (isEmpty()) return NULL;
        queueElem* e = workList.next();
        free(e); // put it back to freeList
        return e->elem;
    }
    void  push(void *elem) {
        queueElem* se = getFreeQueueElem();
        if (se == NULL)
            se = new (mm) queueElem();
        se->elem = elem;
        se->insertBefore(&workList);
    };
    // if stack is empty, NULL is returned
    void* top() {return workList.next()->elem;}
    // returns true if empty
    bool  isEmpty() {return workList.next() == &workList;}
private:
    class queueElem : public Dlink 
    {
    public:
        void     *elem;

        queueElem() : elem(NULL) {}
        queueElem* next() {return (queueElem*)_next;}
        queueElem* prev() {return (queueElem*)_prev;}
    };

    MemoryManager& mm;
    queueElem freeList;
    queueElem workList; 

    void  free(queueElem* n) {
        n->unlink();
        n->insertBefore(&freeList); 
    }
    queueElem* getFreeQueueElem() {
        queueElem *n = freeList.next();
        if (n != &freeList) // there is a free node
        {
            n->unlink();
            n->elem = NULL;
            return n;
        }
        return NULL;
    }
};

template <class T>
class Queue : public queueImpl {
public:
    Queue(MemoryManager& m) : queueImpl(m) {}
    T*    pop()         {return (T*)queueImpl::pop();}
    void  push(T* elem) {queueImpl::push(elem);}
    T*    top()         {return (T*)queueImpl::top();}
};

} //namespace Jitrino 

#endif // _QUEUE_H_
