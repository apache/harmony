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

#ifndef _EVENT_H_
#define _EVENT_H_

namespace Jitrino {

///////////////////////////////////////////////////////////////////////////////
//
//  Event objects can be used for debugging purposes as well as to contain the
//  amount of optimizations done to put bounds on compile time/space, etc.
//  For debugging the client would create an event object and associate it with 
//  optimization being debugged. On each optimization step the event would be
//  incremented. The client would cease to apply the optimization once the
//  associated event overflows. With this we can do a binary search to find the
//  minimum number of optimization steps needed to cause a failure. Then we can
//  generate a "good" and "bad" compilation output that are very close to each 
//  other thereby enabling easy debug.
//
///////////////////////////////////////////////////////////////////////////////

class Event {
public:
    enum State {
        Inactive = 0,
        Counting,
        Overflowed,
        Handled,
    };
    //
    // Constructor
    //
    Event(char *nm) : counter(0), threshold((U_32) -1), 
                      state(Inactive), name(nm) {}
    //
    // Increment the event counter by reported number of events
    //
    void reportEvent(int count) {
        if (state != Inactive) {
            counter += count;
            if (counter > threshold && state == Counting)
                state = Overflowed;
        }
    }
    //
    // Set the state to indicate the event overflow has been handled. Does not
    // reset the counter to start counting again. Client has to explicitly call
    // startMonitor.
    //
    void reportHandling() {
        assert(state == Overflowed);
        state = Handled;
    }
    //
    // Set overflow threshold
    //
    void setThreshold(U_32 th) { threshold = th; }
    //
    // Is monitoring active on this event?
    //
    bool isCounting()      { return state == Counting; }
    bool isInactive()      { return state == Inactive; }
    bool isOverflowed()    { return state == Overflowed; }
    bool isHandled()       { return state == Handled; }
    //
    // Start the event monitoring. Called at the beginning of monitoring and 
    // after handling an overflow when the client wishes to continue 
    // monitoring.
    //
    void startMonitor() {
        assert(threshold != ((U_32) -1));
        counter = 0;
        state   = Counting;
    }
    //
    // Stop the event monitoring. Meant to either temporarily or permanently 
    // suspend monitoring. Event occurrences are reported when event monitoring is
    // suspended. They are not counted.
    //
    void stopMonitor() {
        // Monitoring cannot be suspended unless it is in Counting or Overflowed
        // states as otherwise we will not know how to reset state upon 
        // restarting.
        assert(state != Handled);
        state = Inactive;
    }
    //
    // Restart the event monitoring without resetting the counter to zero. 
    // Meant for use after a temporary suspension of monitoring. Counting 
    // continues where it was left off at suspension.
    //
    void reStartMonitor() {
        assert(state == Inactive);
        if (counter > threshold)
            state = Overflowed;
        else
            state = Counting;
    }
    //
    // Print data on the event
    //
    void print(::std::ostream & os) {
        os << ::std::endl << "Event: " << name;
        os << "  State = ";
        if (state == Inactive)
            os << "Inactive";
        else if (state == Counting)
            os << "Counting";
        else if (state == Overflowed)
            os << "Overflowed";
        else {
            assert(state == Handled);
            os << "Handled";
        }
        os << "  Counter = " << (int) counter;
        os << "  Threshold = " << (int) threshold;
        os << ::std::endl;
    }

private:
    // Fields
    U_32 counter;     // Counts number of occurrences of the event 
    U_32 threshold;   // Threshold for overflow
    State  state;       // Active indicates that the counter is being tracked
    char * name;        // Name for the event counter
};

} //namespace Jitrino 

#endif // _EVENT_H_
