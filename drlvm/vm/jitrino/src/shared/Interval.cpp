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

#include "Interval.h"
namespace Jitrino
{

bool    Interval::startOrExtend (size_t instIdx)
{   
    Span * last = spans.size() > 0 ? &spans.back() : NULL;

    if ( last != NULL && instIdx == last->beg ){ // open the last span 
        last->beg=0; 
        return true;
    }
    if (last == NULL || last->beg != 0)
    {// insert open span
        Span tmp={ 0, instIdx };
        spans.push_back(tmp);
        return true;
    }
//  continue the same open use
    return false;
}

void    Interval::stop (size_t instIdx)
{   
    Span * last=spans.size()>0?&spans.back():NULL;
    if ( last==NULL || ( last->beg != 0 && last->beg !=instIdx ) ){ // closed
        Span tmp={ instIdx, instIdx }; // probably dead def, add the shortest possible span
        spans.push_back(tmp);
    }else{ // last!=NULL && ( last->beg == 0 || last->beg ==instIdx  ) 
        last->beg=instIdx;
    }   
}   

void Interval::finish(bool sort)
{
    if (sort)
        ::std::sort(spans.begin(), spans.end(), Span::less);
    if (spans.size() != 0){
        beg = spans.front().beg;
        end = spans.back().end;
        assert(beg <= end);
    }
}


//  Determinate if two operands conflict (i.e. can be assigned to the same register)
//
bool Interval::conflict (const Interval * r, int& adj) const
{
    int d;
    if (beg <= r->end && r->beg <= end)
    {
        Spans::const_iterator 
            i = spans.begin(),      iend = spans.end(), 
            k = r->spans.begin(),   kend = r->spans.end(); 

        while (i != iend && k != kend)
        {
            if ((d = int(i->end - k->beg)) < 0)
            {
                if (d == -1)
                    ++adj;
                ++i;
            }
            else if ((d = int(i->beg - k->end)) > 0)
            {
                if (d == 1)
                    ++adj;
                ++k;
            }
            else
                return true;
        }
    }

    return false;
}


void Interval::unionWith(const Interval * r)
{
    for (Spans::const_iterator it = r->spans.begin(); it != r->spans.end(); it++)
        spans.push_back(*it);
    finish();
}


::std::ostream& operator << (::std::ostream& os, const Interval::Span& x)
{
    os << " [" << x.beg << "," << x.end << "]";
    return os;
}

::std::ostream& operator << (::std::ostream& os, const Interval::Spans& x)
{
    for (Interval::Spans::const_iterator i = x.begin(); i != x.end(); ++i)
        os << *i;
    return os;
}

::std::ostream& operator << (::std::ostream& os, const Interval& x)
{
    os << "beg: " << x.beg << " end: " << x.end << " spans: " << x.spans;
    return os;
}
}

