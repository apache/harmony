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
 * @author Intel, Sergey L. Ivashin
 */

#include "Ia32IRManager.h"
#include "Log.h"


namespace Jitrino
{

namespace Ia32
{


//========================================================================================
// class Ia32RegAlloc2
//========================================================================================


static const char* help = 
"  opnds=<threshold>\n"
;


struct RegAlloc0Action : public Action
{
    HPipeline ra2, ra3;

    unsigned opnds;

    void init ();
};


struct RegAlloc0 : public SessionAction
{
    U_32 getNeedInfo () const     {return 0;}
    U_32 getSideEffects () const  {return 0;}

    void runImpl();
};


static ActionFactory<RegAlloc0, RegAlloc0Action> _regalloc("regalloc", help);



void RegAlloc0Action::init ()
{
    ra2 = getPipeline("RA2");
    assert(ra2 != 0);

    ra3 = getPipeline("RA3");
    assert(ra3 != 0);

    getArg("opnds", opnds = 2000);
}


void RegAlloc0::runImpl()
{
    CompilationContext* cc = getCompilationContext();

    RegAlloc0Action* act = (RegAlloc0Action*)getAction();
    HPipeline pipe = (getIRManager().getOpndCount() < act->opnds) ? act->ra3 : act->ra2;
    for (PMF::PipelineIterator pit(pipe); pit.next();) 
    {
        SessionAction* sa = (SessionAction*)pit.getSessionAction();
        sa->setCompilationContext(cc);
        cc->setCurrentSessionAction(sa);
        cc->stageId++;

        if (isLogEnabled(LogStream::CT))
        {
            log(LogStream::CT) 
                << "RegAlloc opnds threshold:" << act->opnds 
                << " actual opnds:" << getIRManager().getOpndCount()
                << " RA:" << sa->getTagName()
                << std::endl;
        }

        sa->start();
        sa->run();
        sa->stop();
        cc->setCurrentSessionAction(0);
        assert(!cc->isCompilationFailed() &&  !cc->isCompilationFinished());
    }
}


} //namespace Ia32
} //namespace Jitrino
