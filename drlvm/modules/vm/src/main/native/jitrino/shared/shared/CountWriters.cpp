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
 * @author Sergey L. Ivashin
 *
 */

#include "CountWriters.h"
#include <iostream>
#include <fstream>
#include <algorithm>
#include <vector>
#include <stdio.h>
#include <assert.h>
#include <string.h>

#ifdef _WIN32
#pragma pack(push)
    #include <windows.h>
#pragma pack(pop)

#endif


using namespace std;


#define MAILSLOT "Jitrino.Counters"
#define RUNSTART "---Dump of counters begins---"
#define RUNSTOP  "---Dump of counters ends---"


namespace Jitrino 
{

//=============================================================================================
// class CounterBase implementation
//=============================================================================================


CounterBase::CounterBase (const char* s)
: key(s)
{
    next = head;
    head = this;
}


CounterBase::~CounterBase ()
{
}


void CounterBase::link ()
{
    for (CounterBase* ptr = head; ptr != 0; ptr = ptr->next)
        if (ptr == this)
            return;

    next = head;
    head = this;
}


CounterBase* CounterBase::head = 0;

   
//=============================================================================================
// class CountWriterFile implementation
//=============================================================================================


CountWriterFile::CountWriterFile (const char* s)
:file(0)
{
    open(s);
}


CountWriterFile::~CountWriterFile ()
{
    close();
}


bool CountWriterFile::open (const char* fname)
{
    assert(file == 0);

    if (fname != 0 && *fname != 0)
    {
        file = new ofstream;
        file->open(fname, ios::app);
        if (!*file)
        {
            delete file;
            file = 0;
            return false;
        }
    }

    return true;
}


static bool compareCounters (CounterBase* a, CounterBase* b)
{
    return strcmp(a->key, b->key) < 0;
}


void CountWriterFile::close ()
{
    if ((os = file) == 0)
        os = &cout;

    *os << RUNSTART << endl;

    vector<CounterBase*> counts;

    for (CounterBase* ptr = CounterBase::head; ptr != 0 ; ptr = ptr->next)
        if (ptr->key != 0)
            counts.push_back(ptr);

    sort(counts.begin(), counts.end(), compareCounters);

    for (vector<CounterBase*>::iterator ptr = counts.begin(), end = counts.end(); ptr != end; ++ptr)
        (*ptr)->write(*this);

    *os << RUNSTOP << endl;

    if (file != 0)
    {
        delete file;
        file = 0;
    }
}


void CountWriterFile::write (const char* key, const char* value)
{
    *os << key << "=" << value << endl;
}


void CountWriterFile::write (const char* key, int value)
{
    *os << key << "=" << value << endl;
}


void CountWriterFile::write (const char* key, size_t value)
{
    *os << key << "=" << value << endl;
}


void CountWriterFile::write (const char* key, double value)
{
    *os << key << "=" << value << endl;
}


//=============================================================================================
// class CountWriterMail implementation
//=============================================================================================


#ifdef _WIN32

CountWriterMail::CountWriterMail (const char* s)
: sloth(INVALID_HANDLE_VALUE)
{
    open(s);
}


CountWriterMail::~CountWriterMail ()
{
    if (sloth != INVALID_HANDLE_VALUE)
        close();
}


//  'server' should be name of the computer to which mail is sent or 0
//
bool CountWriterMail::open (const char* server)
{
    assert(sloth == INVALID_HANDLE_VALUE);

    char slotname[128];
    _snprintf(slotname, sizeof(slotname), "\\\\%s\\mailslot\\"MAILSLOT, (server == 0 || *server == 0) ? "." : server);

    sloth = CreateFile(slotname, GENERIC_WRITE, FILE_SHARE_READ, 0, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, 0); 

    mail(RUNSTART"\0", strlen(RUNSTART));

    if (sloth == INVALID_HANDLE_VALUE)
        cerr << "CountWriter failed to write to '" << slotname << "' (server not running?)" << endl;

    return sloth != INVALID_HANDLE_VALUE;
}


void CountWriterMail::close ()
{
    for (CounterBase* ptr = CounterBase::head; ptr != 0 && sloth != INVALID_HANDLE_VALUE; ptr = ptr->next)
        if (ptr->key != 0)
            ptr->write(*this);

    mail(RUNSTOP"\0", strlen(RUNSTOP));

    if (sloth != INVALID_HANDLE_VALUE)
    {
        CloseHandle(sloth);
        sloth = INVALID_HANDLE_VALUE;
    }
}


void CountWriterMail::write (const char* key, const char* value)
{
    char buff[128];
    mail(buff, _snprintf(buff, sizeof(buff), "%s=%s", key, value));
}


void CountWriterMail::write (const char* key, int value)
{
    char buff[128];
    mail(buff, _snprintf(buff, sizeof(buff), "%s=%i", key, value));
}


void CountWriterMail::write (const char* key, size_t value)
{
    char buff[128];
    mail(buff, _snprintf(buff, sizeof(buff), "%s=%u", key, value));
}


void CountWriterMail::write (const char* key, double value)
{
    char buff[128];
    mail(buff, _snprintf(buff, sizeof(buff), "%s=%G", key, value));
}


void CountWriterMail::mail (const char* str, size_t bytes)
{
    assert(str[bytes] == 0);
    bytes++;
    DWORD written;
    if (sloth != INVALID_HANDLE_VALUE && 
        WriteFile(sloth, str, static_cast<DWORD>(bytes), &written, 0) == 0)
    { 
        CloseHandle(sloth);
        sloth = INVALID_HANDLE_VALUE;
    } 
}

#endif //#ifdef _WIN32


} //namespace Jitrino 
