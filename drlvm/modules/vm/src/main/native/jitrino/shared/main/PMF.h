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
 */

#ifndef _PMF_H_
#define _PMF_H_

#include "XTimer.h"
#include "LogStream.h"
#include "Jitrino.h"
#include "mkernel.h"
#include "MemoryManager.h"
#include "Stl.h"
#include <string.h>
#include <iostream>
#include <iomanip>
#include <fstream>


namespace Jitrino
{


       bool        getBool   (const char* val, bool def);
inline int         getInt    (const char* val, int  def)           {return val == 0 ? def : atoi(val);}
inline const char* getString (const char* val, const char* def)    {return val == 0 ? def : val;}


struct Str
{
    const char* ptr;
    size_t count;


    Str ()                                  :ptr(0), count(0) {}
    Str (const char* s, size_t n)           :ptr(s), count(n) {}
    Str (const char* s)                     :ptr(s), count(s == 0 ? 0 : strlen(s)) {}

    void init (const char* s)               {ptr = s, count = strlen(s);}
    bool empty () const                     {return count == 0;}
    void clear ()                           {count = 0;}
    bool trim  ();
    const char* beg  () const               {return ptr;};
    const char* end  () const               {return ptr + count;};
    const char* findFirst (char x) const    {return findNext(x, ptr);}
    const char* findNext  (char x, const char*) const;
    const char* find (const char*) const;
};


std::ostream& operator << (std::ostream&, const Str&);


typedef StlVector<Str> Strs;


struct LogTemplate
{
    size_t idx;
    Str  streamname;
    Str  filtername;
    Strs* pathp;
    Str  fmask;
    LogStream::SID sid;

    bool jit_specific,          // true if fmask contains %jit% macros
         thread_specific;       // true if fmask contains %thread% macros

    bool enabled,
         append;

    LogTemplate ()                          :pathp(0), enabled(false), append(false) {}
};

typedef StlVector<LogTemplate> LogTemplates;


class PMF;


class LogStreams
{
protected:

    MemoryManager& mm;                  // thread-local MM
    PMF& pmf;                           // synchronized only access
    const size_t nbos;

    typedef StlVector <LogStream*> Streams;
    Streams streams;

    typedef StlVector <Streams*> StreamsStack;
    StreamsStack streamsstack;

    int depth,
        threadnb,
        methodnb;

    void assign (size_t sx, const char* fname, size_t fnamesz);

public:

    LogStreams (MemoryManager&, PMF&, int);
    ~LogStreams ();

    static LogStreams& current(JITInstanceContext* = 0);

//  Construct log stream file names according to the currently compiled method
    void beginMethod (const char* cname, const char* mname, const char* sig, int methodnb);
    void endMethod ();

    size_t size () const                            {return nbos;}

    const LogTemplate& logtemplate (size_t idx) const;

    LogStream&   logstream (size_t idx) const       
    {
        return nbos != 0 ? *streams.at(idx)
                         : LogStream::log_sink();
    }

friend class PMF;
};


struct LogDisplay
{
    typedef StlVector <size_t> StreamIdxs;
    StreamIdxs streamidxs;


    LogDisplay (MemoryManager&);

    void add (size_t idx, LogTemplate&);

    LogStream& log (LogStream::SID sid) const;
};


class JITInstanceContext;
class IActionFactory;
class Action;
class SessionAction;


struct CompareChars
{
    bool operator () (const char* a, const char* b) const   {return strcmp(a, b) < 0;}
};


class PMF
{
protected:

    bool initialized,
         first;
    MemoryManager& mm;
    JITInstanceContext& jitInstanceContext;
    Str jitname;
    Str help_requested;

    struct Cmd;

    typedef StlVector<Cmd*> Cmds;
    Cmds cmds;

    typedef StlMap<Str, Str*> FilterSpecs;

    struct MethodFilter
    {
        Str classname,
            methodname,
            signature;

        void init (const Str&);
        bool empty () const;                    
        int  pass (const char* cname, const char* mname, const char* sig) const;
        const char* c_str (MemoryManager&) const;
    };

    struct Args
    {
        struct Arg 
        {
            const char* key; 
            const char* value;
            int  strength;      // relative strength of this argument
            Cmd* cmdp;

            bool operator == (const char* k) const      {return strcmp(key, k) == 0;}
        };

        typedef StlVector<Arg> Store;
        Store store;

        Args (MemoryManager& mm)                : store(mm) {}

        void add (const char* key, const char* value, int strength, Cmd*);
        const char* get (const char* key) const;
    };

    LogTemplates logtemplates;

    typedef StlMap<const char*, LogStream*, CompareChars> FilesDictionary;
    struct Files : public Mutex, public FilesDictionary
    {
        Files (MemoryManager& mm) : FilesDictionary(mm) {}
    };

    static Files* pfiles;

public:

    struct Pipeline
    {
        PMF& pmf;
        Str name;
        MethodFilter method;
        bool initialized;

        struct Alias;

        Alias* root;

        typedef StlVector<Alias*> Aliases;
        Aliases* aliases;

        struct Step : public XTimer
        {
            /*const*/ Pipeline* pipeline;
            Strs* fqname;
            IActionFactory* factory;
            Action* action;
            Args*   args;           // 0 or array of arguments
            LogDisplay* logs;
            bool reused;

            void setup (MemoryManager&);
        };

        typedef StlVector<Step> Steps;
        Steps* steps;


        Pipeline (PMF& p);
        ~Pipeline ();

        void init ();
        void deinit ();
        Alias* lookup (Str*);
        Alias* findPath (Str*, size_t);
        void stop (const char* msg);
        Str getName () const                    {return name;}
    };

protected:

    typedef StlVector<Pipeline*> Pipelines;
    Pipelines pipelines;

    struct ArgIterator;


    bool parse (Cmd&, const char*, FilterSpecs&);
    void processCmd (Cmd&);
    void initStreams  ();
    LogTemplate& lookStream (Str& streamname, Cmd* cmdp, size_t xpath, size_t xlog);
    void walk (Pipeline&, Pipeline::Alias*, Strs&);
    const Cmd* lookArg (Pipeline* pipeline, const Str* fqname, size_t fqsize) const;
    Pipeline* lookup (Str* filtername, bool create = false);

public:

    typedef Pipeline* HPipeline;

    class PipelineIterator
    {
    protected:

        PMF& pmf;
        Pipeline* pipeline;
        Pipeline::Steps::iterator it;
        MemoryManager mm;
        MemoryManager* smm;
        SessionAction* session;
        bool first;

    public:

        PipelineIterator (PMF& pmf, const char* classname, const char* methodname = 0, const char* signature = 0);
        PipelineIterator (HPipeline);
        ~PipelineIterator ();

    //  pipeline specific methods

        const char* getPipeName   (MemoryManager&) const;
        const char* getFilterSpec (MemoryManager&) const;

    //  step specific methods

        const char*     getStepName () const;
        IActionFactory* getFactory() const                  {return it->factory;}
        Action*         getAction () const                  {return it->action;}
        SessionAction*  getSessionAction () const           {return session;}
        MemoryManager&  getSessionMemoryManager () const    {return *smm;}
        MemoryManager&  getMemoryManager ()                 {return mm;}

        bool next ();
    };


    PMF (MemoryManager&, JITInstanceContext&);

    void processCmd (const char*);
    void processCmd (const char*, const char*);
    void processVMProperties ();

    void init (bool first = false);
    void deinit ();

    const LogTemplates& getLogTemplates () const            {return logtemplates;}
    JITInstanceContext& getJITInstanceContext () const      {return jitInstanceContext;}
    void summTimes (SummTimes&);
    static Action* getAction (HPipeline, const char* path);
    void showHelp (std::ostream&);
    void showHelpJits (std::ostream&);

    HPipeline selectPipeline (const char* classname, const char* methname, const char* sig) const;

    HPipeline getPipeline (const char* name) const;
    const char* getArg (HPipeline, const char* key) const;

    const char* getStringArg  (HPipeline p, const char* key, const char* def) const
        {return getString(getArg(p, key), def);}

    int   getIntArg  (HPipeline p, const char* key, int  def) const
        {return getInt(getArg(p, key), def);}

    bool  getBoolArg (HPipeline p, const char* key, bool def) const
        {return getBool(getArg(p, key), def);}


friend class LogStreams;
friend class IAction;
friend class Action;
};


} //namespace Jitrino

#endif //#ifndef _PMF_H_
