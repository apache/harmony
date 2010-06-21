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
 * @author Intel, Mikhail Y. Fursov
 *
 */

#ifndef _STACK_H_
#define _STACK_H_

#include "MemoryManager.h"
#include "Dlink.h"

namespace Jitrino {

class stackImpl
{ 
public:
    stackImpl(MemoryManager& m) : mm(m) {}
    void* pop() {
        if (isEmpty()) return NULL;
        stackElem* e = workList.next();
        free(e); // put it back to freeList
        return e->elem;
    }
    void  push(void *elem) {
        stackElem* se = getFreeStackElem();
        if (se == NULL)
            se = new (mm) stackElem();
        se->elem = elem;
        se->insertAfter(&workList);
    };
    // if stack is empty, NULL is returned
    void* top() {return workList.next()->elem;}
    // returns true if empty
    bool  isEmpty() {return workList.next() == &workList;}
private:
    class stackElem : public Dlink 
    {
    public:
        void     *elem;

        stackElem() : elem(NULL) {}
        stackElem* next() {return (stackElem*)_next;}
        stackElem* prev() {return (stackElem*)_prev;}
    };

    MemoryManager& mm;
    stackElem freeList;
    stackElem workList; 

    void  free(stackElem* n) {
        n->unlink();
        n->insertBefore(&freeList); 
    }
    stackElem* getFreeStackElem() {
        stackElem *n = freeList.next();
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
class Stack : public stackImpl {
public:
    Stack(MemoryManager& m) : stackImpl(m) {}
    T*    pop()         {return (T*)stackImpl::pop();}
    void  push(T* elem) {stackImpl::push(elem);}
    T*    top()         {return (T*)stackImpl::top();}
};

} //namespace Jitrino 

#endif // _STACK_H_
