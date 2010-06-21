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

#ifndef _DLINK_H
#define _DLINK_H

namespace Jitrino {

class Dlink {
protected:
    // add vtable to Dlink to see Dlink successors fields in VS debugger
    virtual void vtable_stub() const {} 
public:
    Dlink() {
        _next = _prev = this;
    }
    virtual ~Dlink() {}
    //
    // routines for manipulating double linked list
    //
    void unlink()
    {
        _prev->_next=_next;
        _next->_prev=_prev;
        _next=_prev=this;
    }
    // insert "this" after "prev_link"
    void insertAfter(Dlink *prev_link) {
        assert(_prev==this);
        assert(_next==this);
        _prev = prev_link;
        _next = prev_link->_next;
        prev_link->_next = this;
        _next->_prev = this;
    }
    void insertBefore(Dlink *next_link) {
        assert(_prev==this);
        assert(_next==this);
        _next = next_link;
        _prev = next_link->_prev;
        next_link->_prev = this;
        _prev->_next = this;
    }
    // move all elements in the list except for this one
    // into another list
    void moveTo(Dlink *head) {
        if (_prev == this) 
            return;
        _next->_prev = head->_prev;
        _prev->_next = head;
        head->_prev->_next = _next;
        head->_prev = _prev;
        _prev=_next=this;
    }
    //
    // move this link to be linked after the 'afterlink'
    // with all followup links until 'head' is met
    //
    void moveTailTo(Dlink* head, Dlink* afterlink) {
        assert(head != this);
        assert(head != afterlink);
        if ( head == this ) {
            return;
        }

        afterlink->_prev = head->_prev;
        _prev->_next = head;
        head->_prev = _prev;
        _prev = afterlink;
        afterlink->_next = this;
        afterlink->_prev->_next = afterlink;
    }
    Dlink *getNext() const {return _next;}
    Dlink *getPrev() const {return _prev;}
protected:
    Dlink *_next, *_prev;
};

class DlinkElem : public Dlink {
public:
    DlinkElem(void* e) : elem(e) {}
    DlinkElem()                 {elem = NULL;}
    DlinkElem*  getNext()       {return (DlinkElem*)Dlink::getNext();}
    DlinkElem*  getPrev()       {return (DlinkElem*)Dlink::getPrev();}
    void*       getElem()       {return elem;}
    void        setElem(void* e){elem = e;}
    void        moveTo(DlinkElem *head) {Dlink::moveTo((Dlink *) head);}
private:
    void* elem;
};

} //namespace Jitrino 

#endif // _DLINK_H
