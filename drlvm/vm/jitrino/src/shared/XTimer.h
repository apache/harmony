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

#ifndef _XTIMER_H_
#define _XTIMER_H_

#include "Counter.h"
#include "open/types.h"
#include <vector>


namespace Jitrino 
{

         
class XTimer
{
public:

    XTimer ()                                   :  totalTime(0), state(0) {}

    static void initialize (bool on);

    void reset ();
    void start ();
    void  stop ();
    int64  getTotal   () const                  {return totalTime;}
    double getSeconds () const;

    //static double getFrequency ();    

protected:

    int64 startTime,
          totalTime;
    int   state;
};


class CountTime : public CounterBase, public XTimer
{
public:

    CountTime (const char* name)                : CounterBase(name) {}
    virtual ~CountTime ()                       {}

    /*virtual*/void write (CountWriter& logs)   {logs.write(key, getSeconds());}
};


class AutoTimer
{
public:

    AutoTimer (CountTime& c)                    :counter(c) {counter.start();}
    ~AutoTimer ()                               {counter.stop();}

protected:

    CountTime& counter;
};


struct SummTimes : public std::vector<std::pair<const char*, double> >, public CounterBase
{
    SummTimes (const char* name)                : CounterBase(name) {}

    void add (const char* key, double seconds);

    /*virtual*/void write  (CountWriter&);
};


} //namespace Jitrino 


#endif //#ifndef _XTIMER_H_
