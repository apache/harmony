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
 * @author Intel, Nikolay A. Sidelnikov
 *
 */


#ifndef _INTERVAL_H_
#define _INTERVAL_H_

#include "Stl.h"
#include <stdlib.h>
#include <fstream>
#include <string.h>

namespace Jitrino
{

// TODO: Shared with bin-pack reg allocator (Ia32RegAlloc2) =>
// Move to separate shared/Interval.* files or create Ia32RegAlloc2 h with these definitions
// change Ia32RegAlloc2 as necessary to use shared Interval

struct Interval
{
    struct Span
    {
        size_t beg, end;
        static bool less (const Span& x, const Span& y) 
        { 
            return x.beg < y.end; // to order properly spans like [124,130] [124,124] 
        }
    };

    typedef StlVector<Span> Spans;
    Spans spans;            // liveness data in form of list of intervals
    size_t beg, end;        // start point of first interval and end point of last interval


    Interval(MemoryManager& mm)
        :spans(mm), beg(0), end(0){}

    bool startOrExtend (size_t instIdx);
    void stop(size_t instIdx);
    void finish(bool sort = true);
    void unionWith(const Interval * r);
    bool conflict (const Interval * r, int& adj) const;

};

::std::ostream& operator << (::std::ostream& os, const Interval::Span& x);

::std::ostream& operator << (::std::ostream& os, const Interval::Spans& x);

::std::ostream& operator << (::std::ostream& os, const Interval& x);

// <= TODO: Shared with bin-pack reg allocator (Ia32RegAlloc2)
// Move to separate shared/Interval.* files or create Ia32RegAlloc2 h with these definitions
}

#endif

