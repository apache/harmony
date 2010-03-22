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
#ifndef _LOCK_MANAGER_H
#define _LOCK_MANAGER_H 

#include "open/hythread_ext.h"
#include "open/types.h"

#ifdef __cplusplus

class Lock_Manager {

public:
    Lock_Manager();
    ~Lock_Manager();

    void _lock();
    void _unlock();
    bool _tryLock();

    void _lock_enum();
    void _unlock_enum();

    bool _lock_or_null();
    void _unlock_or_null();

    void _unlock_enum_or_null ();
    bool _lock_enum_or_null (bool return_null_on_fail);

private:
    osmutex_t    lock;
};


// Auto-unlocking class for Lock_Manager
class LMAutoUnlock {
public:
    LMAutoUnlock( Lock_Manager* lock ):m_innerLock(lock), m_unlock(true)
    { m_innerLock->_lock(); }
    ~LMAutoUnlock() { if( m_unlock ) m_innerLock->_unlock(); }
    void ForceUnlock() { m_innerLock->_unlock(); m_unlock = false; }
private:
    Lock_Manager* m_innerLock;
    bool m_unlock;
};

#endif // __cplusplus

#endif /* _LOCK_MANAGER_H */
