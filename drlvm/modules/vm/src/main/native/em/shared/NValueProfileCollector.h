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
* @author Yuri Kashnikov
*/
/*
        The idea of advanced Top-N-Value (with steady and clear parts, and clear interval)
        from <"Value profilling and optimization", B.Calder, P.Feller, Journal of Instruction-Level Parallelism, 1999>

*/
#ifndef _VALUE_PROFILE_COLLECTOR_H_
#define _VALUE_PROFILE_COLLECTOR_H_

#include "DrlProfileCollectionFramework.h"
#include "port_mutex.h"
#include "open/vm_util.h"

#include <map>

#define TNV_DEFAULT_CLEAR_VALUE 0

class ValueMethodProfile;
class VPInstructionProfileData;

struct Simple_TNV_Table
{
    POINTER_SIZE_INT value;
    U_32 frequency;
};

enum ProfileUpdateStrategy {

    // Lock when modifying the TNV table, even trivial counter increments.
    // This mode is only one that guarantees accurate profiling.
    UPDATE_LOCKED,

    // Lock when restructuring the TNV table (inserting). Trivial counter
    // increments might not be locked (in current implementation they are not
    // locked for FirstN table management only)
    UPDATE_LOCKED_INSERT,

    // When a new value is added to TNV all simultaneous updates to the table
    // are skipped
    UPDATE_FLAGGED_ALL,

    // Only simultaneous inserts to table are skipped, counter updates are
    // processed unsafely
    UPDATE_FLAGGED_INSERT,

    // Completely insafe updates of the TNV table (use with care)
    UPDATE_UNSAFE
};

class TNVTableManager {
public:
    typedef struct Simple_TNV_Table TableT;
    typedef POINTER_SIZE_INT ValueT;
    typedef VPInstructionProfileData VPData;
    TNVTableManager(U_32 steady_size, U_32 clear_size,
            U_32 clear_interval, ProfileUpdateStrategy update_strategy) :
        steadySize(steady_size),
        clearSize(clear_size),
        clearInterval(clear_interval),
        updateStrategy(update_strategy)
    {}

    VPInstructionProfileData* createProfileData();

    // finds a given value in TNV table, returns the index, (-1) if not found
    I_32 find(TableT* where, ValueT value_to_search, U_32 size);

    // clearSize elements are cleared from the top
    void clearTopElements(TableT* where);

    // returns the index of the minimum element
    I_32 findMinIdx(TableT* where, U_32 size);

    // returns the maximum value in a given steady TNV table
    ValueT findMax(TableT* TNV_where);

    // adds value to method profile with appropriate locking of the methProfile
    virtual void addNewValue(ValueMethodProfile* methProfile,
            VPData* instProfile, ValueT curr_value) = 0;

    // flush num_times_profiled data back to the TNV table
    //     note: unsynchronized method - must be called from synchronized ones
    void flushLastValueCounter(VPData* instProfile);

    void dumpValues(VPInstructionProfileData* data, std::ostream& os);

protected:
    virtual void insert(TableT* where, TableT* clear_part,
            ValueT value_to_insert, U_32 times_met) = 0;

    const U_32 steadySize, clearSize, clearInterval;
    const ProfileUpdateStrategy updateStrategy;
};

class TNVTableDividedManager : public TNVTableManager {
public:
    // c-tor
    TNVTableDividedManager(U_32 steady_size, U_32 clear_size,
            U_32 clear_interval, ProfileUpdateStrategy us) :
        TNVTableManager(steady_size, clear_size, clear_interval, us)
    {}

    virtual void addNewValue(ValueMethodProfile* methProfile,
            VPData* instProfile, ValueT curr_value);

protected:
    virtual void insert(TableT* where, TableT* clear_part,
            ValueT value_to_insert, U_32 times_met);
};

class TNVTableFirstNManager : public TNVTableManager {
public:
    // c-tor
    TNVTableFirstNManager(U_32 steady_size, U_32 clear_size,
            U_32 clear_interval, ProfileUpdateStrategy us) :
        TNVTableManager(steady_size, clear_size, clear_interval, us)
    {}

    virtual void addNewValue(ValueMethodProfile* methProfile,
            VPData* instProfile, ValueT curr_value);

private:
    void insert(TableT* where, TableT* clear_part,
            ValueT value_to_insert, U_32 times_met);

};

class ValueProfileCollector : public ProfileCollector {
public:
    enum algotypes {
        TNV_DIVIDED,
        TNV_FIRST_N
    };

    ValueProfileCollector(EM_PC_Interface* em, const std::string& name, JIT_Handle genJit,
                                                U_32 _TNV_steady_size, U_32 _TNV_clear_size,
                                                U_32 _clear_interval, algotypes _TNV_algo_type,
                                                ProfileUpdateStrategy update_strategy);

    virtual TbsEMClient* getTbsEmClient() const {return (NULL);}
    virtual ~ValueProfileCollector();
    MethodProfile* getMethodProfile(Method_Handle mh) const ;
    ValueMethodProfile* createProfile(Method_Handle mh, U_32 numkeys, U_32 keys[]);

    TNVTableManager* getTnvMgr() { return tnvTableManager; }
private:
    std::string catName;
    bool   loggingEnabled;
    typedef std::map<Method_Handle, ValueMethodProfile*> ValueProfilesMap;
    ValueProfilesMap profilesByMethod;
    mutable osmutex_t profilesLock;
    TNVTableManager* tnvTableManager;
    ProfileUpdateStrategy updateStrategy;
};

class VPInstructionProfileData
{
public:
    struct Simple_TNV_Table* TNV_Table;
    struct Simple_TNV_Table * TNV_clear_part;
public:
    VPInstructionProfileData() :
        last_value(TNV_DEFAULT_CLEAR_VALUE),
        num_times_profiled(0),
        profile_tick(0)
    {}
public:
    POINTER_SIZE_INT last_value;
    U_32 num_times_profiled;
    U_32 profile_tick;
};

typedef std::map<U_32, VPInstructionProfileData*> VPDataMap;
class ValueMethodProfile : public MethodProfile {
public:
    ValueMethodProfile(ValueProfileCollector* pc, Method_Handle mh);
    ~ValueMethodProfile();
    void lockProfile() {port_mutex_lock(&lock);}
    void unlockProfile() {port_mutex_unlock(&lock);}
    void dumpValues(std::ostream& os);
    void addNewValue(U_32 instructionKey, POINTER_SIZE_INT valueToAdd);
    POINTER_SIZE_INT getResult(U_32 instructionKey);

    // UpatingState is used to implement UPDATE_FLAGGED_* strategies.
    //     (updatingState == 1) when method profile is being updated to skip
    //         concurrent modifications.
    //     (updatingState == 0) when profile is open for modifications.
    U_8* getUpdatingStatePtr() { return &updatingState; }
private:
    ValueProfileCollector* getVPC() const;

    friend class ValueProfileCollector;
    VPDataMap ValueMap;

    // The lock and the atomically modified updatingState flag operate per
    // method to allow simultaneous updates for distinct methods.
    osmutex_t lock;
    U_8 updatingState;
};

POINTER_SIZE_INT value_profiler_get_top_value (Method_Profile_Handle mph, U_32 instructionKey);
void value_profiler_add_value (Method_Profile_Handle mph, U_32 instructionKey, POINTER_SIZE_INT valueToAdd);
Method_Profile_Handle value_profiler_create_profile(PC_Handle pch, Method_Handle mh, U_32 numkeys, U_32 keys[]);
void value_profiler_dump_values(Method_Profile_Handle mph, std::ostream& os);

#endif
