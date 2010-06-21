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

#ifndef _LIST_H_
#define _LIST_H_

#include <assert.h>

template <class T> 
class List {
public:
    List(T* i, List* t) : elem(i), tl(t) {}
    List*   getNext()   {return tl;}
    T*      getElem()   {return elem;}
private:
    T*      elem;
    List*   tl;
};

template <class T>
class ListIter {
public:
    ListIter(List<T>* hd) {next = hd;}
    bool    hasNext()   {return next != NULL;}
    T*      getNext()   {
        if (hasNext() == false) {
            assert(0);
            return NULL;
        }
        T* elem = next->getElem();
        next = next->getNext();
        return elem;
    }
private:
    List<T>*    next;
};

#endif // _LIST_H_
