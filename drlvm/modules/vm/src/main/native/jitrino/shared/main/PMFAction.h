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

#ifndef _PMFACTION_H_
#define _PMFACTION_H_

#include "PMF.h"

#include <string.h>

namespace Jitrino
{


class IActionFactory
{
public:

    IActionFactory (const char* name);
    virtual ~IActionFactory ()                      {}

    virtual Action*         createAction (MemoryManager&)           {return 0;}
    virtual SessionAction*  createSessionAction (MemoryManager&)    = 0;
    virtual void            showHelp (std::ostream&)                {}

    void   showHelp (std::ostream&, const char* help) const;
    const  char* getName () const                   {return name;}

    static IActionFactory* find (Str&);
    static IActionFactory* getFirst ()              {return head;}
    IActionFactory* getNext () const                {return next;}

protected:

    const char* name;
    IActionFactory* next;
    static IActionFactory* head;
};


class IAction
{
public:

    IAction ()                                      : step(0) {}

    IActionFactory* getFactory () const             {return step->factory;}
    const char* getName () const                    {return step->factory->getName();}
    const char* getArg (const char* key) const      {return (step->args) ? step->args->get(key) : 0;}
    bool  getArg (const char* key, unsigned int&  v) const;
    bool  getArg (const char* key, unsigned long& v) const;
    bool  getArg (const char* key, bool& v) const;

    typedef PMF::HPipeline HPipeline;
    HPipeline getPipeline () const                  {return step->pipeline;}
    HPipeline getPipeline (const char* name) const  {return step->pipeline->pmf.getPipeline(name);}
    const char* getArg (HPipeline p, const char* key) const;

    const char* getStringArg  (const char* key, const char* def) const
        {return getString(getArg(key), def);}

    const char* getStringArg  (const std::string& key, const char* def) const 
        {return getString(getArg(key.c_str()), def);}

    int   getIntArg  (const char* key, int def) const
        {return getInt(getArg(key), def);}

    int   getIntArg  (const std::string& key, int  def) const 
        {return getInt(getArg(key.c_str()), def);}

    bool  getBoolArg (const char* key, bool def) const
        {return getBool(getArg(key), def);}

    bool  getBoolArg (const std::string& key, bool def) const
        {return getBool(getArg(key.c_str()), def);}

    const char* getStringArg  (HPipeline p, const char* key, const char* def) const
        {return getString(getArg(p, key), def);}

    const char* getStringArg  (HPipeline p, const std::string& key, const char* def) const
        {return getString(getArg(p, key.c_str()), def);}

    int   getIntArg  (HPipeline p, const char* key, int  def) const
        {return getInt(getArg(p, key), def);}

    int   getIntArg  (HPipeline p, const std::string& key, int  def) const
        {return getInt(getArg(p, key.c_str()), def);}

    bool  getBoolArg (HPipeline p, const char* key, bool def) const
        {return getBool(getArg(p, key), def);}

    bool  getBoolArg (HPipeline p, const std::string& key, bool def) const
        {return getBool(getArg(p, key.c_str()), def);}

    JITInstanceContext& getJITInstanceContext () const      {return step->pipeline->pmf.jitInstanceContext;}

#ifndef _NOLOG
    bool isLogEnabled (LogStream::SID) const;
    bool isLogEnabled (const char* streamname) const;

    LogStream& log (LogStream::SID) const;
    LogStream& log (const char* streamname) const;
#else
    bool isLogEnabled (LogStream::SID) const            {return false;}
    bool isLogEnabled (const char* streamname) const    {return false;}

    LogStream& log (LogStream::SID) const               {return LogStream::log_sink();}
    LogStream& log (const char* streamname) const       {return LogStream::log_sink();}
#endif

    bool getLogStreamID (LogStream::SID&, const char* streamname) const;

protected:

    /*const*/ PMF::Pipeline::Step* step;

friend struct PMF::Pipeline;
friend class  PMF::PipelineIterator;
};


class Action : public IAction
{
public:

    virtual ~Action ()                              {}

    virtual void init ()                            {}
    virtual void deinit ()                          {}

    Action* createAuxAction (const char* name, const char* suffix) const;
    Action* createAuxAction (MemoryManager&, const char* name, const char* suffix) const;
    SessionAction* createSession (MemoryManager&);
};


class CompilationContext;

class SessionAction : public IAction
{
public:

    virtual ~SessionAction ()                       {}

    virtual void run ()                             = 0;
    
    Action* getAction () const                      {return step->action;}

    void setCompilationContext(CompilationContext* _cc) {cc = _cc;}
    CompilationContext* getCompilationContext() const {return cc;}

    void start ()                                   {step->start();}
    void stop ()                                    {step->stop();}

private:

    CompilationContext* cc;

friend class  Action;
};


template <class S, class A = Action>
class ActionFactory : public IActionFactory
{
public:

    ActionFactory (const char* name, const char* h = 0)     :IActionFactory(name), help(h) {}

    Action*         createAction (MemoryManager& mm)        {return new (mm) A();}
    SessionAction*  createSessionAction (MemoryManager& mm) {return new (mm) S();}
    void            showHelp (std::ostream& os)             {IActionFactory::showHelp(os, help);}

private:

    const char* help;
};


} //namespace Jitrino

#endif //#ifndef _PMFACTION_H_
