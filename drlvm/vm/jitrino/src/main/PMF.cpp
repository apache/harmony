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

#include "VMInterface.h"
#include "PMF.h"
#include "PMFAction.h"
#include "FixFileName.h"
#include "JITInstanceContext.h"
#include "CompilationContext.h"
#include <ctype.h>
#include <errno.h>
#include <stdlib.h>
#include <stdio.h>
#include <assert.h>
#include <algorithm>
#include <fstream>
#include <iostream>
#include <iomanip>

#ifdef _WIN32
    #pragma pack(push)
        #include <windows.h>
    #pragma pack(pop)
    #include <direct.h>
    #define vsnprintf _vsnprintf
    #define PATHSEPCHAR '\\'
#else   //PLATFORM_POSIX
    #include <stdarg.h>
    #include <sys/stat.h>
    #include <sys/types.h>
    #define PATHSEPCHAR '/'
#endif //ifdef _WIN32


using namespace std;


namespace Jitrino
{


//------ helper functions ---------------------------------------------------//


void  create_dir (const char* dirname)
{
#ifdef _WIN32
    int err;
    if (_mkdir(dirname) != 0 && (err = errno) != 17)
        cerr << "mkdir errno#" << err << " for <" << dirname << ">" << endl;
#else
    int err;
    if (mkdir(dirname, 0777 /*rwxrwxrwx*/)  != 0 && (err = errno) != 17)
        cerr << "mkdir errno#" << err << " for <" << dirname << ">" << endl;
#endif
}


void mkdir (char* fname)
{
    for (char* sptr = fname; *sptr != 0; ++sptr)
        if (*sptr == PATHSEPCHAR)
        {
            *sptr = 0;

            if (*fname != 0)
            create_dir(fname);

            *sptr = PATHSEPCHAR;
        }
}


static const char* c_str (Str& str)
{
    if (str.count != 0)
    {
        const_cast<char*>(str.ptr)[str.count] = 0;
        return str.ptr;
    }
    else
        return "";
}

 
static const char* c_str (MemoryManager& mm, Str& str)
{
    if (str.count != 0)
    {
        char* buffp = new (mm) char[str.count + 1];
        memcpy(buffp, str.ptr, str.count);
        buffp[str.count] = 0;
        return buffp;
    }

    return "";
}


static const char* c_str (MemoryManager& mm, Str* fqnamep, size_t fqnsize)
{
    size_t count = 0;

    Str* strp = fqnamep;
    for (size_t n = fqnsize; n != 0; --n, ++strp)
        count += strp->count + 1;

    if (count != 0)
    {
        char* buffp;
        char* ptr = buffp = new (mm) char[count+1];
        strp = fqnamep;
        for (size_t n = fqnsize; n != 0; --n, ++strp)
        {
            const Str& str = *strp;
            memcpy(ptr, str.ptr, str.count);
            ptr += str.count;
            *ptr++ = '.';
        }
        --ptr;  // skip the last '.'
        *ptr = 0;
        return buffp;
    }

    return "";
}


static bool operator < (const Str& a, const Str& b)
{
    return (a.count == b.count) ? strncmp(a.ptr, b.ptr, a.count) < 0 : a.count < b.count;
}


static bool operator == (const Str& a, const Str& b)
{
    return (a.count == b.count) ? strncmp(a.ptr, b.ptr, a.count) == 0 : false;
}


static bool operator == (const Str& a, const char* s)
{
    if (s == 0 || *s == 0)
        return a.count == 0;
    else
        return (a.count == strlen(s)) ? strncmp(a.ptr, s, a.count) == 0 : false;
}


static bool compare (const Str* p, const Str* q, size_t count)
{
    for (; count != 0; --count, ++p, ++q)
        if (!(*p == *q))
            return false;

    return true;
}


ostream& operator << (ostream& os, const Str& a)
{
    return os.write(a.ptr, a.count);
}


static ostream& operator << (ostream& os, const Strs& fqn)
{
    for (size_t n = 0; n != fqn.size(); ++n)
    {
        if (n != 0)
            os << '.';
        os << fqn[n];
    }
    return os;
}


//---------------------------------------------------------------------------//


bool getBool (const char* val, bool def)
{
    if (val != NULL) 
    {
        if (strcmp(val, "true") == 0 || strcmp(val, "yes") == 0 || strcmp(val, "on") == 0)
            def  = true;
        else if (strcmp(val, "false") == 0 || strcmp(val, "no") == 0 || strcmp(val, "off") == 0)
            def  = false;
        else
            crash("PMF: invalid value for bool parameter '%s' (true|false or yes|no or on|off is expected)\n", val);
    }
    return def;
}


//------ Str implementation -------------------------------------------------//


bool Str::trim ()
{
    // trim the left end
    while (count != 0 && isspace(*ptr))
        --count, ++ptr;

    // trim the right end
    while (count != 0 && isspace(ptr[count-1]))
        --count;

    return count != 0;
}


const char* Str::findNext (char x, const char* nxt) const
{
    if (count != 0)
    {
        const char* end = ptr + count;
        if (nxt == 0)
            nxt = ptr;
        for (; nxt < end; ++nxt)
            if (*nxt == x)
                return nxt;
    }
    return 0;
}


const char* Str::find (const char* what) const
{
    if (count != 0)
    {
        size_t n = strlen(what);
        assert(n != 0);
        const char* end = ptr + count - n;
        for (const char* nxt = ptr; nxt <= end; ++nxt)
            if (strncmp(nxt, what, n) == 0)
                return nxt;
    }
    return 0;
}

//------ StrTokenizer declaration and implementation ------------------------//


struct StrTokenizer
{
    const char* ptr;
    const char* end;
    char sep;
    Str  token;


    StrTokenizer (char c, Str& s)   : ptr(s.ptr), end(s.ptr + s.count), sep(c) {}
    StrTokenizer (char c, const char* p, const char* e) : ptr(p), end(e), sep(c) {}

    bool next ();
};


bool StrTokenizer::next ()
{
    if (ptr == end)
        return false;

    token.ptr = ptr;
    while (ptr != end && *ptr != sep)
        ++ptr;
    token.count = ptr - token.ptr;
    if (ptr != end)
        ++ptr;
    token.trim();
    return true;
};


//------ StrSubstitution declaration and implementation ---------------------//


struct StrSubstitution
{
    typedef void (StrSubstitution::*Replacer) ();

    struct Replace
    {
        Str what;
        Replacer replace;
    };

    static Replace replaces[];

//  Parameters for replacements
    Str jitname;
    const char* classname,
              * methodname,
              * signature;
    int threadnb, 
        seqnb;
    Str streamname;

//  Some results of replecement
    bool failure, 
         jit_specific,
         thread_specific;

    size_t rescount;
    char   result[MAXLOGFILENAME];

    char* dptr,
        * dend;

    bool substitute (const Str& from);

    void replace_jit    ()      
    {
        insert(jitname, false);
        jit_specific = true;
    }

    void replace_class  ()      
    {
        if (classname) insert(Str(classname), true);
    }

    void replace_tree   ()      
    {
        if (classname) insert(Str(classname), false);
    }

    void replace_method ()      
    {
        if (methodname) insert(Str(methodname), true);
        if (signature) insert(Str(signature), true);
    }

    void replace_seqnb  ()      
    {
        dptr += sprintf(dptr, "%.6u", seqnb);
    }

    void replace_log  ()        
    {
        insert(Str(streamname), false);
    }

    void replace_thread  ()     
    {
        dptr += sprintf(dptr, "%u", threadnb);
        thread_specific = true;
    }

    void insert (Str src, bool fix);

    void seekz ();
};


StrSubstitution::Replace StrSubstitution::replaces[] = 
{
    {Str("%jit%"),          &StrSubstitution::replace_jit},
    {Str("%class%"),        &StrSubstitution::replace_class},
    {Str("%class_tree%"),   &StrSubstitution::replace_tree},
    {Str("%method%"),       &StrSubstitution::replace_method},
    {Str("%seqnb%"),        &StrSubstitution::replace_seqnb},
    {Str("%log%"),          &StrSubstitution::replace_log},
    {Str("%thread%"),       &StrSubstitution::replace_thread}
};


bool StrSubstitution::substitute (const Str& from)
{
    failure = jit_specific = thread_specific = false;

    const char* sptr = from.ptr,
              * send = from.ptr + from.count;

    dptr = result,
    dend = result + sizeof(result);

    while (sptr != send)
    {
        bool replace = false;

        size_t n;
        Replace* rpend = replaces + sizeof(replaces)/sizeof(Replace);
        for (Replace* rp = replaces; rp != rpend ; ++rp)
            if (sptr + (n = rp->what.count) <= send && 
                strncmp(sptr, rp->what.ptr, n) == 0)
            {
                (this->*rp->replace)();
                sptr += n;
                replace = true;
                break;
            }

        if (!replace)
            *dptr++ = *sptr++;
    }

    *dptr++ = 0;

    rescount = dptr - result;
    return !failure;
};


void StrSubstitution::insert (Str s, bool fix)
{
    bool truncate = !(dptr + s.count < dend);
    int max_len = truncate ? dend - dptr - 1 : s.count;
    assert(max_len > 0);

    if (s.empty())
    {
        failure = true;
        return;
    }

    if (fix)
    {
        for (const char* sptr = s.ptr, * send = s.ptr + max_len; sptr != send;)
        {
            char c = *sptr++;
            if (c == '/')
                c = '_';
            *dptr++ = c;
        }
    }
    else
    {
        memcpy(dptr, s.ptr, max_len);
        dptr += max_len;
    }
}


void StrSubstitution::seekz ()
{
    while (dptr < dend && *dptr != 0);
        ++dptr;
}


//------ Known Streams ------------------------------------------------------//


static const char* deffmask     = "log/%jit%/%class%/%method%/%log%.log";
static const char* ct_deffmask  = "log/%jit%/%class%/%method%/ct.log";
static const char* dot_deffmask = "log/%jit%/%class%/%method%/dot/.dot";

struct KnownStream 
{
    LogStream::SID sid;
    const char* streamname;
    const char* deffmask;
};

static KnownStream knownstreams[] =
{
    {LogStream::INFO,       "info",     "log/info.log"},
    {LogStream::RT,         "rt",       "log/rt.log"},
    {LogStream::CT,         "ct",       ct_deffmask},
    {LogStream::IRDUMP,     "irdump",   ct_deffmask},
    {LogStream::DOTDUMP,    "dotdump",  dot_deffmask},
    {LogStream::DBG,        "dbg",          deffmask}
};

const size_t nb_knownstreams = sizeof(knownstreams)/sizeof(KnownStream);


static KnownStream* isKnownStream (const Str& streamname)
{
    for (size_t i = 0; i != nb_knownstreams; ++i)
        if (streamname == knownstreams[i].streamname)
            return &knownstreams[i];

    return 0;
}


//------ LogStream implementation -------------------------------------------//


/*static*/ bool LogStream::ison = false;
/*static*/ LogStream  LogStream::logsink;
/*static*/ LogStream* LogStream::logrtp = &LogStream::logsink;


LogStream::LogStream ()
:fname(0), enabled(false), append(false), pending_open(false), was_open(false), mutexp(0), ref_count(0)
{
}


LogStream::LogStream (/*const*/ char* s)
:fname(s), enabled(true), append(false), pending_open(true), was_open(false), mutexp(0), ref_count(0)
{
}


LogStream::~LogStream ()
{
    close();
}


void LogStream::open (/*const*/ char* s)
{
    assert(!was_open && !pending_open);
    fname = s;
    enabled = true;
    pending_open = true;
}



void LogStream::close ()
{
    if (was_open)
    {
        was_open = false;
        os.close();
    }
}

 
#ifndef _NOLOG
int LogStream::printf (const char* fmt, ...)    
{
    AutoUnlock lock(mutexp);
    if (lazy_open())
    {
        va_list args;
        va_start(args, fmt);

        char buff[256];
        int n = vsnprintf(buff, sizeof(buff), fmt, args);

        os << buff;
        return n;
    }
    else
        return 0;
}
#endif


bool LogStream::lazy_open ()
{
    if (pending_open)
    {
        pending_open = false;

        ios_base::openmode mode = ios_base::out;
        if (append)
            mode |= ios_base::app;

        char temp[MAXFILENAMESIZE];
        fix_file_name(temp, sizeof(temp), fname);

        mkdir(temp);
        os.open(temp, mode);
        was_open = os.is_open();
        if (!was_open)
            cerr << "Failed to open log file '" << temp << "'" << endl;
    }
    return was_open;
}


//TODO: Is it possible to remove locking there?
void LogStream::addRef ()
{
    AutoUnlock lock(mutexp);
    ++ref_count;
}


bool LogStream::releaseRef ()
{
    AutoUnlock lock(mutexp);

    if (--ref_count == 0)
        close();

    assert(ref_count >= 0);

    return ref_count == 0;
}


LogStream& LogStream::log (LogStream::SID sid, const char* pipename)
{
    return log(sid, pipename, pipename == 0 ? 0 : strlen(pipename));
}


LogStream& LogStream::log (LogStream::SID sid, const char* pipename, size_t namesz)
{
    if (isOn())
    {
        Str pipe(pipename, namesz);
        LogStreams& streams = LogStreams::current();
        LogStream* lsp = 0;
        for (size_t idx = 0, n = streams.size(); idx != n; ++idx)
        {
            const LogTemplate& lt = streams.logtemplate(idx);
            if ((lt.pathp == 0 || lt.pathp->empty()) && lt.sid == sid) {
                if (lt.filtername == pipe)
                    return streams.logstream(idx);
                else if (lt.filtername.empty())
                    lsp = &streams.logstream(idx);
            }
        }
        if (lsp != 0)
            return *lsp;
    }

    return log_sink();
}


LogStream& LogStream::log (const char* streamname, const char* pipename)
{
    return log(streamname, pipename, pipename == 0 ? 0 : strlen(pipename));
}


LogStream& LogStream::log (const char* streamname, const char* pipename, size_t namesz)
{
    if (isOn())
    {
        assert(streamname != 0 && *streamname != 0);
        Str pipe(pipename, namesz);
        LogStreams& streams = LogStreams::current();
        LogStream* lsp = 0;
        for (size_t idx = 0, n = streams.size(); idx != n; ++idx)
        {
            const LogTemplate& lt = streams.logtemplate(idx);
            if ((lt.pathp == 0 || lt.pathp->empty()) && lt.streamname == streamname) {
                if (lt.filtername == pipe)
                    return streams.logstream(idx);
                else if (lt.filtername.empty())
                    lsp = &streams.logstream(idx);
            }
        }
        if (lsp != 0)
            return *lsp;
    }

    return log_sink();
}


//------ LogStreams implementation ------------------------------------------//


LogStreams::LogStreams (MemoryManager& mm_, PMF& pmf_, int t)
:mm(mm_), pmf(pmf_), nbos(pmf.getLogTemplates().size())
, streams(mm, nbos), streamsstack(mm, nbos)
, depth(0), threadnb(t), methodnb(0)
{
    StrSubstitution ss;
    ss.jitname = pmf.jitname;
    ss.classname  = 0;
    ss.methodname = 0;
    ss.signature  = 0;
    ss.threadnb   = threadnb;
    ss.seqnb      = methodnb;

    for (size_t sx = 0; sx != nbos; ++sx)
    {
        streams[sx] = &LogStream::logsink;

        const LogTemplate& ltemplate = pmf.getLogTemplates()[sx];
        if (ltemplate.enabled)
        {
            streamsstack[sx] = new (mm) Streams(mm);

            ss.streamname = ltemplate.streamname;
            if (ss.substitute(ltemplate.fmask))
            {// File name doesn't require compilation context (class or method name)
                AutoUnlock lock(*pmf.pfiles);
                assign(sx, ss.result, ss.rescount);
            }
        }

    //  Turn on the whole logging if there are log templates defined
        LogStream::ison = true;
    }
}


LogStreams::~LogStreams ()
{
    LogStream* lsp;
    for (size_t sx = 0; sx != nbos; ++sx)
        if ((lsp = streams[sx])->enabled)
        {
            AutoUnlock lock(*pmf.pfiles);
            if (lsp->releaseRef())
                (*pmf.pfiles)[lsp->fname] = 0;
#ifdef _DEBUG_PMF_STREAMS
cout << "LogStreams[" << threadnb << "] depth:" << depth << " idx:" << sx
     << "  free '" << lsp->fname
     << "' closed:" << ((*pmf.pfiles)[lsp->fname] == 0)
     << endl;
#endif
        }
}


void LogStreams::beginMethod (const char* cname, 
                              const char* mname, 
                              const char* sig,
                              int nb)
{
    ++depth;

    StrSubstitution ss;
    ss.jitname = pmf.jitname;
    ss.classname  = cname;
    ss.methodname = mname;
    ss.signature  = sig;
    ss.threadnb   = threadnb;
    ss.seqnb      = nb;

    for (size_t sx = 0; sx != nbos; ++sx)
    {
        const LogTemplate& ltemplate = pmf.getLogTemplates()[sx];
        if (ltemplate.enabled)
        {
            LogStream* lsp = streams[sx];
            Streams* sstack = streamsstack[sx];

            ss.streamname = ltemplate.streamname;
            ss.substitute(ltemplate.fmask);
            if (!lsp->enabled || strcmp(lsp->fname, ss.result) != 0)
            {// File name changed: stream will be changed, so save stream on stack
                sstack->push_back(lsp);
#ifdef _DEBUG_PMF_STREAMS
cout << "LogStreams[" << threadnb << "] depth:" << depth << " idx:" << sx
     << "pushed '" << (lsp->enabled ? lsp->fname : "-none-")
     << "'"
     << endl;
#endif
            //  change stream - use new or reuse already open stream
                AutoUnlock lock(*pmf.pfiles);
                assign(sx, ss.result, ss.rescount);
            }
            else
            {// File name didn't changed, continue the current stream
                sstack->push_back(0);
            }
        }
    }
}


void LogStreams::endMethod ()
{
    assert(depth > 0);

    for (size_t sx = 0; sx != nbos; ++sx)
    {
        LogStream* lsp = streams[sx];
        if (lsp->enabled)
        {
            Streams* sstack = streamsstack[sx];

            if (sstack->back() != 0)
            {// Return to the saved stream
                AutoUnlock lock(*pmf.pfiles);

            //  If this file no longer needed, close it but remember its name.
            //  Next time the file will be open, it will be open in append mode.
                if (lsp->releaseRef())
                    (*pmf.pfiles)[lsp->fname] = 0;

#ifdef _DEBUG_PMF_STREAMS
cout << "LogStreams[" << threadnb << "] depth:" << depth << " idx:" << sx
     << "  free '" << lsp->fname
     << "' closed:" << ((*pmf.pfiles)[lsp->fname] == 0)
     << endl;
#endif
                streams[sx] = sstack->back();

#ifdef _DEBUG_PMF_STREAMS
cout << "LogStreams[" << threadnb << "] depth:" << depth << " idx:" << sx
     << " poped '" << (streams[sx]->enabled ? streams[sx]->fname : "-none-")
     << "'"
     << endl;
#endif
            }
            else
            {// Stream didn't changed
            }
     
            sstack->pop_back();
        }
    }

    --depth;
}


void LogStreams::assign (size_t sx, const char* fname, size_t fnamesz)
{
    const LogTemplate& ltemplate = pmf.getLogTemplates()[sx];
    LogStream* lsp;    

    PMF::Files::iterator ptr = pmf.pfiles->find(fname);
    if (ptr == pmf.pfiles->end() || ptr->second == 0)
    {// There is no file open with this name.
        lsp = new (pmf.mm) LogStream();

    //  If %jit% macros used in the file name template, then this file will 
    //  not be shared by several thread. Otherwise, synchronization is
    //  neccessary.
        if (!ltemplate.thread_specific)
            lsp->mutexp  = new (mm) Mutex();

        lsp->append = ltemplate.append;
        if (!ltemplate.append && ptr != pmf.pfiles->end())
            lsp->append = true; // the file was opened before

        lsp->enabled = lsp->pending_open = true;

        lsp->fname = new (mm) char[fnamesz];
        memcpy(lsp->fname, fname, fnamesz);
        (*pmf.pfiles)[lsp->fname] = lsp;

    //  Special case of rt stream - only the single instance of LogStream object can exist
        if (ltemplate.sid == LogStream::RT)
            LogStream::logrtp = lsp;

#ifdef _DEBUG_PMF_STREAMS
cout << "LogStreams[" << threadnb << "] depth:" << depth << " idx:" << sx
     << "   new '" << lsp->fname 
     << "' seen before:" << (ptr != pmf.pfiles->end())
     << endl;
#endif
    }
    else
    {
        lsp = ptr->second;
#ifdef _DEBUG_PMF_STREAMS
cout << "LogStreams[" << threadnb << "] depth:" << depth << " idx:" << sx
     << " reuse '" << lsp->fname 
     << "'"
     << endl;
#endif
    }

    lsp->addRef();
    streams[sx] = lsp;
}


const LogTemplate& LogStreams::logtemplate (size_t idx) const
{
    return pmf.logtemplates.at(idx);
}


//------ logDisplay implementation ------------------------------------------//


LogDisplay::LogDisplay (MemoryManager& mm)
:streamidxs(mm, nb_knownstreams)
{
    for (size_t sid = 0; sid != nb_knownstreams; ++sid)
        streamidxs[sid] = 0;    // index of sink stream
}


void LogDisplay::add (size_t idx, LogTemplate& lt)
{
    KnownStream* ksp = isKnownStream(lt.streamname);
    if (ksp != 0)
        streamidxs.at(ksp->sid) = idx;
    else
        streamidxs.push_back(idx);
}


LogStream& LogDisplay::log (LogStream::SID sid) const
{
    size_t idx = streamidxs.at(sid);
    return LogStreams::current().logstream(idx);
}


//------ PMF::Cmd declaration and implementation ----------------------------//


struct PMF::Cmd
{
    char* buff;         // own copy of the command source

    Strs* left;         // left part (before '=' character)
    Str right;          // right part (after '=' character)

    bool arg,           // true for 'arg' type command
         log;           // true for 'log' type command
    Str  jitname,       // jit name string (left[0] in fact)
         filtername;    // filter name string 
    int  xkeyword,      // index of keyword found in 'left' array
         xlog;          // for 'log' type command only - index of 'log' keyword

    Cmd ()              : buff(0), arg(false), log(false) {}

    int  strength (size_t pathsz = 0) const;
    void fatal (const char* msg) const;
};


static int cmd_strength (bool jit_known, bool filter_known, size_t path_size)
{
    return int( ((1 + (filter_known ? 2 : 0) + (jit_known ? 4 : 0)) << 16) + path_size );
}


int  PMF::Cmd::strength (size_t pathsz) const  
{
    return cmd_strength(!jitname.empty(), !filtername.empty(), pathsz);
}


void PMF::Cmd::fatal (const char* msg) const
{
    crash("Error in command line '%s'\n%s\n", left->front().ptr, msg);
}


//------ PMF::MethodFilter implementation -----------------------------------//


//  Parse filter specification with the following syntax:
//      <class name> . <method name> <signature>
//  It is assumed that <signature> starts with the '(' character.
//
void PMF::MethodFilter::init (const Str& filter)
{
    const char* ptr1 = filter.findFirst('.');
    if (ptr1 == 0)
        ptr1 = filter.findFirst(':');

    const char* ptr2 = filter.findNext('(', ptr1);
    const char* ptr3 = filter.end();

    if (ptr1 == 0)
    //  no class name
        classname.count = 0,
        methodname.ptr  = filter.ptr;
    else
    {// classname, then method name
        classname.ptr   = filter.ptr,
        classname.count = ptr1 - filter.ptr,

        ++ptr1;         // skip the '.'/':' character
        if (ptr1 < filter.end() && *ptr1 == ':')
            ++ptr1;     // skip the second ':'

        methodname.ptr  = ptr1;
    }


    if (ptr2 == 0)
    //  no method signature
        methodname.count = ptr3 - methodname.ptr,
        signature.count  = 0;
    else
        methodname.count = ptr2 - methodname.ptr,
        signature.ptr    = ptr2,    // include the '(' character
        signature.count  = ptr3 - ptr2;

    classname.trim(),
    methodname.trim(),
    signature.trim();

    if (classname.empty() && methodname.findFirst('/') != 0)
        classname = methodname,
        methodname.clear();
}


bool PMF::MethodFilter::empty () const                  
{
    return  classname.empty() && 
           methodname.empty() &&
            signature.empty();
}


int PMF::MethodFilter::pass (const char* cname, const char* mname, const char* sig) const
{
    if (cname != 0 && strncmp(cname, classname.ptr,  classname.count)  != 0)
        return -1;
    if (mname != 0 && strncmp(mname, methodname.ptr, methodname.count) != 0)
        return -1;
    if (sig   != 0 && strncmp(sig,   signature.ptr,  signature.count)  != 0)
        return -1;

    return (int)(classname.count + methodname.count + signature.count);
}


const char* PMF::MethodFilter::c_str (MemoryManager& mm) const
{
    size_t count = classname.count + 1 + methodname.count + signature.count;

    char* cstr = new (mm) char[count+1];

    char* ptr = cstr;
    memcpy(ptr, classname.ptr, classname.count);
    ptr += classname.count;
    *ptr++ = '.';

    memcpy(ptr, methodname.ptr, methodname.count);
    ptr += methodname.count;

    memcpy(ptr, signature.ptr, signature.count);
    ptr += signature.count;
    *ptr = 0;

    return cstr;
}


//------ PMF::Args implementation -------------------------------------------//


void PMF::Args::add (const char* key, const char* value, int strength, Cmd* cmdp)
{
    Store::iterator end = store.end(),
                    ptr = find(store.begin(), end, key);
    if (ptr == end)
    {
        Arg arg;
        arg.key   = key,
        arg.value = value;
        arg.strength = strength;
        arg.cmdp = cmdp;
        store.push_back(arg);
    }
    else if (strength >= ptr->strength)
    {
        ptr->value = value,
        ptr->strength = strength;
        ptr->cmdp = cmdp;
    }
}


const char* PMF::Args::get (const char* key) const
{
    Store::const_iterator end = store.end(),
                          ptr = find(store.begin(), end, key);
    if (ptr == end)
        return 0;
    else
        return ptr->value;
}


//------ Alias declaration and implementation  ------------------------------//


struct PMF::Pipeline::Alias
{
    Str name;

    struct Child
    {
        Str name;
        enum Type {REQUIRED, DEFON, DEFOFF} type;
        Alias* aliasp;

        bool operator == (const Str& s) const           {return name == s;}
    };

    typedef StlVector<Child> Childs;
    Childs childs;


    Alias (MemoryManager& mm)                           : childs(mm) {}
    Alias (MemoryManager& mm, const Alias&);
};


//  shallow copy
//
PMF::Pipeline::Alias::Alias (MemoryManager& mm, const Alias& orig)
:childs(mm)
{
    name = orig.name;
    childs.insert(childs.end(), orig.childs.begin(), orig.childs.end());
    for (Childs::iterator it = childs.begin(); it != childs.end(); ++it)
        it->aliasp = 0;
}


//------ ArgIterator declaration and implementation  ------------------------//


struct PMF::ArgIterator
{
    const Pipeline::Step& step;
    const char* key;
    Cmds::iterator it;
    bool first;


    ArgIterator (const Pipeline::Step& s, const char* k)    :step(s), key(k), first(true) {}

    bool next ();

    operator Cmd* ()                    {return *it;}
};


bool PMF::ArgIterator::next ()
{
    Cmds::iterator end = step.pipeline->pmf.cmds.end();

    for (;;)
    {
        if (first)
        {
            first = false;
            it = step.pipeline->pmf.cmds.begin();
        }
        else
        {
            if (it == end)
                return false;
            ++it;
        }

        if (it == end)
            return false;

        if (*it == 0 || !(*it)->arg)
            continue;

        Cmd& cmd = **it;

        if (!cmd.filtername.empty() && !(cmd.filtername == step.pipeline->name))
            continue;

        //  index of argument
        size_t xend = cmd.left->size() - 1;

        //  size of path in command (i.e. number of items between argument and keuword)
        size_t pathsz = xend - cmd.xkeyword - 1;

        if (pathsz > step.fqname->size()) 
            continue;

        if (key != 0 && !(cmd.left->at(xend) == key))
            continue;

        if (pathsz == 1 && cmd.left->at(cmd.xkeyword + 1) == step.factory->getName())
            return true;

        size_t xl, xs;
        for (xl = cmd.xkeyword + 1, xs = 0; xl != xend; ++xl, ++xs)
            if (!(cmd.left->at(xl) == step.fqname->at(xs)))
                break;

        if (xl == xend)
            return true;
    }
}


//------ PMF implementation  ------------------------------------------------//


bool PMF::parse (Cmd& cmd, const char* keyword, FilterSpecs& filterspecs)
{
    Strs::iterator i = find(cmd.left->begin(), cmd.left->end(), keyword);
    if (i == cmd.left->end())
        return false;

    cmd.xkeyword = (int)(i - cmd.left->begin());

    Str* tmp;
    if (cmd.xkeyword == 0)
    {// <keyword> ....
    //  no jit name
    //  no filter name

    //  obsolete form of filter command:
    //  filter.a = ...
        if (strcmp(keyword, "filter") == 0)
        {
            if (cmd.left->size() == 2 && !(tmp = &cmd.left->at(1))->empty())
                cmd.filtername = *tmp;
        }
    }
    else if (cmd.xkeyword == 1 && !(tmp = &cmd.left->at(0))->empty())
    {// <a>.<keyword> ...
    //  <a> can be name of jit name or filter name
        if (strcmp(keyword, "filter") == 0 || filterspecs.find(*tmp) != filterspecs.end())
            cmd.filtername = *tmp;
        else
            cmd.jitname = *tmp;
    }
    else if (cmd.xkeyword == 2)
    {// <a>.<b>.<keyword> ...
    //  <a> must be jit name
    //  <b> must be filter name
        if (!(tmp = &cmd.left->at(0))->empty())
            cmd.jitname = *tmp;

        if (!(tmp = &cmd.left->at(1))->empty())
        {
            cmd.filtername = *tmp;
        }
    }
    else
    {
        cmd.fatal("Keyword misplaced");
    }

    return cmd.jitname.empty() || cmd.jitname == jitname;
}

 
void PMF::processCmd (Cmd& cmd)
{
//  Split line into tokens <left0> . <left1> . .... = <right>

    const char* ptr0 = cmd.buff ;
    const char* ptr1 = ptr0;
    while (*ptr1 != '=' && *ptr1 != 0)
        ++ptr1;

    for (StrTokenizer tokens('.', ptr0, ptr1); tokens.next();)
        cmd.left->push_back(tokens.token);

    if (cmd.left->empty())
        crash("Empty left part of command line '%s'", ptr0);

    if (*ptr1 == 0)
        cmd.right.clear();
    else
    {
        cmd.right.init(ptr1+1);
        cmd.right.trim();
    }

//  Special processing for read command

    if (cmd.left->size() == 1 && cmd.left->at(0) == "read")
    {
        if (cmd.right.empty())
            cmd.fatal("File name missing in read command");

        ifstream is(c_str(mm, cmd.right));
        if (is.fail())
            cmd.fatal("File not found");

        char buff[1024];
        while (is.good())
        {
            is.getline(buff, sizeof(buff));
            processCmd(buff);
        }

        is.close();
    }

//  Special processing for help command

    else if (strncmp(cmd.buff, "help", 4) == 0 ||
             strncmp(cmd.buff, "arg.help", 8) == 0)
    {
        help_requested = cmd.right;
    }

//  Any other command lines are simply stored

    else
        cmds.push_back(&cmd);
}


void PMF::processCmd (const char* key, const char* value)
{
//  Must be called before init()
    assert(!initialized);

    assert(key != 0);
    if (*key == 0 || value == 0)
        return;

//  Copy to internal buffer

    size_t count1 = strlen(key),
           count2 = strlen(value);

    Cmd& cmd = *new (mm) Cmd();
    cmd.buff = new (mm) char[count1 + 1 + count2 + 1];
    cmd.left = new (mm) Strs(mm);
    memcpy(cmd.buff, key, count1);
    cmd.buff[count1] = '=';
    memcpy(cmd.buff+count1+1, value, count2+1);

    processCmd(cmd);
}


void PMF::processCmd (const char* ptr)
{
//  Must be called before init()
    assert(!initialized);

//  Ignore empty and comment command lines

    while (isspace(*ptr))
        ++ptr;

    size_t count = strlen(ptr);
    if (count == 0)
        return;     // skip empty command
    if (ptr[0] == '#')
        return;     // skip #comment
    if (count > 1 && ptr[0] == '/' && ptr[1] == '/')
        return;     // skip //comment

//  Copy to internal buffer

    Cmd& cmd = *new (mm) Cmd();
    cmd.buff = new (mm) char[count+1];
    cmd.left = new (mm) Strs(mm);
    memcpy(cmd.buff, ptr, count+1);

    processCmd(cmd);
}


void PMF::processVMProperties ()
{
    for (VMPropertyIterator it(mm, "jit."); it.next();) {
        processCmd(it.getKey() + 4, it.getValue());
    }
}


PMF::Files* PMF::pfiles = 0;


PMF::PMF (MemoryManager& m, JITInstanceContext& jit)    
:initialized(false),mm(m), jitInstanceContext(jit)
,cmds(mm), logtemplates(mm), pipelines(mm) 
{
    jitname.init(jitInstanceContext.getJITName().c_str());
    if (pfiles == 0)
        pfiles = new (mm) Files(mm);
}


void PMF::init (bool first_)
{
    assert(!initialized);
    first = first_;
    help_requested = false;
    processVMProperties();
    initialized = true;

#ifdef _DEBUG_PMF
    cout << endl << "Commands received:" << endl;
    for (Cmds::iterator it = cmds.begin(); it != cmds.end(); ++it)
    {
        Cmd& cmd = **it;
        cout << *cmd.left << " = <" << cmd.right << "> " << endl;
    }

    cout << endl << "ActionFactory:" << endl;
    for (IActionFactory* afp = IActionFactory::getFirst(); afp != 0; afp = afp->getNext())
        cout << "  " << afp->getName() << endl;
#endif

    if (first && !help_requested.empty() && !(help_requested == "jit"))
        showHelp(cout);

//  Create common pipeline with empty filter

    pipelines.push_back(new (mm) Pipeline(*this));

//  Process all filter statements
    
    FilterSpecs filterspecs(mm);

    Cmd* cmdp;
    for (Cmds::iterator it = cmds.begin(); it != cmds.end(); ++it)
        if ((cmdp = *it) != 0 && parse(*cmdp, "filter", filterspecs))
        {
            *it = 0;

            if (cmdp->right.empty())
                cmdp->fatal("Empty filter");

            if (cmdp->filtername.empty())
                cmdp->fatal("Invalid filter name");

            if (cmdp->filtername == jitname)
                cmdp->fatal("Invalid filter name - the same as jit name");

            if (filterspecs.find(cmdp->filtername) != filterspecs.end())
                cmdp->fatal("Duplicate filters defined");

            filterspecs[cmdp->filtername] = &cmdp->right;
        }

//  Process all path statements

    for (Cmds::iterator it = cmds.begin(); it != cmds.end(); ++it)
        if ((cmdp = *it) != 0 && parse(*cmdp, "path", filterspecs))
        {
            *it = 0;

            if (!cmdp->filtername.empty() && filterspecs.find(cmdp->filtername) == filterspecs.end())
                continue;

        //  Process left of '=" part

            Str* namep = 0;

            size_t x = cmdp->left->size() - cmdp->xkeyword - 1;
            if (x == 0)
            {
            }
            else if (x == 1)
            {
                Str& name = cmdp->left->at(cmdp->xkeyword + 1);
                if (!name.empty())
                    namep = &name;
            }
            else
                cmdp->fatal("Extra items after path name");

            Pipeline* pipeline = lookup(&cmdp->filtername, true);
            Pipeline::Alias* aliasp = pipeline->lookup(namep);
            if (aliasp == 0)
            {
                aliasp = new (mm) Pipeline::Alias(mm);
                if (namep == 0)
                    pipeline->root = aliasp;
                else
                    aliasp->name = *namep;
                pipeline->aliases->push_back(aliasp);
            }
            else
            {
                cmdp->fatal("Multiple path defined");
            }

        //  Process right of '=" part

            for (StrTokenizer tokens(',', cmdp->right); tokens.next();)
            {
                Pipeline::Alias::Child child;
                child.aliasp = 0;
                child.type = Pipeline::Alias::Child::DEFON;
                if (!tokens.token.empty())
                {
                    char x = tokens.token.ptr[tokens.token.count-1];
                    if (x == '!')
                    {
                        child.type = Pipeline::Alias::Child::REQUIRED;
                        --tokens.token.count;
                    }
                    else if (x == '+')
                    {
                        child.type = Pipeline::Alias::Child::DEFON;
                        --tokens.token.count;
                    }
                    else if (x == '-')
                    {
                        child.type = Pipeline::Alias::Child::DEFOFF;
                        --tokens.token.count;
                    }
                    tokens.token.trim();
                }
                if (tokens.token.empty())
                    cmdp->fatal("Empty path child name");
                child.name = tokens.token;

                aliasp->childs.push_back(child);                
            }
        }

        else if (cmdp != 0 && !cmdp->jitname.empty() && !(cmdp->jitname == jitname))
            *it = 0;    // not for this jit command

//  Process all arg statements

    for (Cmds::iterator it = cmds.begin(); it != cmds.end(); ++it)
        if ((cmdp = *it) != 0 && parse(*cmdp, "arg", filterspecs))
        {
            cmdp->arg = true;
            lookup(&cmdp->filtername, true);
        }

        else if (cmdp != 0 && !cmdp->jitname.empty() && !(cmdp->jitname == jitname))
            *it = 0;    // not for this jit command

//  Path resolution

    Pipeline& compipeline = **pipelines.begin();    // this is the common (empty) pipeline
    if (compipeline.root == 0)
        compipeline.stop("PMF: Invalid common pipeline for '%s' - does not have a root path\n"); 

    for (Pipelines::iterator k = pipelines.begin(); k != pipelines.end(); ++k)
    {
        Pipeline& pipeline = **k;
        Pipeline::Aliases& aliases = *pipeline.aliases;

        if (&pipeline != &compipeline)
        {
            FilterSpecs::iterator it = filterspecs.find(pipeline.name);
            if (it == filterspecs.end())
                pipeline.stop("PMF: Invalid pipeline for '%s.%s' - unspecified filter\n");

            pipeline.method.init(*it->second);
            if (pipeline.method.empty())
                pipeline.stop("PMF: Invalid pipeline for '%s.%s' - empty filter\n");
        }

        if (pipeline.root == 0)
        {
            pipeline.root = new (mm) Pipeline::Alias(mm, *compipeline.root);
            aliases.push_back(pipeline.root);
        }

        for (;;)
        {
            Pipeline::Aliases newaliases(mm);

            for (Pipeline::Aliases::iterator l = aliases.begin(); l != aliases.end(); ++l)
            {
                Pipeline::Alias::Childs::iterator end = (*l)->childs.end(),
                                                  ptr = (*l)->childs.begin();
                for (; ptr != end; ++ptr)
                {
                    ptr->aliasp = pipeline.lookup(&ptr->name);
                    if (ptr->aliasp == 0 && &pipeline != &compipeline)
                    {
                        Pipeline::Alias* comaliasp;
                        if ((comaliasp = compipeline.lookup(&ptr->name)) != 0)
                            newaliases.push_back(new (mm) Pipeline::Alias(mm, *comaliasp));
                    }
                }
            }

            if (newaliases.empty())
                break;
            else
                aliases.insert(aliases.end(), newaliases.begin(), newaliases.end());
        }

        Strs fqname(mm);
        walk(pipeline, pipeline.root, fqname);
    }

//  Build parameters map

    for (Cmds::iterator it = cmds.begin(); it != cmds.end(); ++it)
        if ((cmdp = *it) != 0 && cmdp->arg)
        {
            size_t n = cmdp->left->size() - cmdp->xkeyword - 1;
            if (n == 0)
                cmdp->fatal("No argument name");
        }

    initStreams();

    for (Pipelines::iterator k = pipelines.begin(); k != pipelines.end(); ++k)
    {
        Pipeline& pipeline = **k;
        for (Pipeline::Steps::iterator s = pipeline.steps->begin(); s != pipeline.steps->end(); ++s)
        {
            Pipeline::Step& step = *s;
            step.setup(mm);
        }
    }

//  Create actions for common filter

    for (Pipeline::Steps::iterator s = compipeline.steps->begin(); s != compipeline.steps->end(); ++s)
    {
        Pipeline::Step& step = *s;
        step.action = step.factory->createAction(mm);
        step.reused = false;
    }

//  Create actions for other filters

    for (Pipelines::iterator k = pipelines.begin() + 1; k != pipelines.end(); ++k)
    {
        Pipeline& pipeline = **k;

        for (Pipeline::Steps::iterator s = pipeline.steps->begin(); s != pipeline.steps->end(); ++s)
        {
            Pipeline::Step& step = *s;
            step.action = 0;
            step.reused = false;

        //  Check if this step has any filter-specific agruments
            bool filterspec = false;
            if (step.args != 0)
                for (Args::Store::iterator m = step.args->store.begin(); m != step.args->store.end(); ++m)
                    if (!m->cmdp->filtername.empty())
                    {
                        filterspec = true;
                        break;
                    }

        //  If step doesn't have such arguments, action from common filter can be reused
            if (!filterspec)
                for (Pipeline::Steps::iterator m = compipeline.steps->begin(); m != compipeline.steps->end(); ++m)
                    if (step.factory == m->factory)
                    {
                        step.action = m->action;
                        step.reused = true;
                        break;
                    }

        //  Otherwise action is created and initiated
            if (step.action == 0)
                step.action = step.factory->createAction(mm);
        }
    }

    if (help_requested == "jit" || help_requested == "all")
        showHelpJits(cout);

//  Debug output

#ifdef _DEBUG_PMF
    cout << endl << "PMF jit<" << jitname << ">" << endl;
    for (Pipelines::iterator k = pipelines.begin(); k != pipelines.end(); ++k)
    {
        Pipeline& pipeline = **k;
        cout << "  Pipeline<" << pipeline.name << 
            "> method<" << pipeline.method.classname 
                << "><" << pipeline.method.methodname 
                << "><" << pipeline.method.signature 
            << ">" << endl;

        for (Pipeline::Aliases::iterator l = pipeline.aliases->begin(); l != pipeline.aliases->end(); ++l)
        {
            Pipeline::Alias& alias = **l;
            cout << "    Alias<" << alias.name << ">";
            if (pipeline.root == &alias)
                cout << " *root*";
            cout << endl;

            for (Pipeline::Alias::Childs::iterator m = alias.childs.begin(); m != alias.childs.end(); ++m)
            {
                Pipeline::Alias::Child& child = *m;
                cout << "      Child<" << child.name << "> kind:" << child.type;
                if (child.aliasp!= 0)
                    cout << " to <" << child.aliasp->name << ">";
                cout << endl;
            }
        }

        for (Pipeline::Steps::iterator l = pipeline.steps->begin(); l != pipeline.steps->end(); ++l)
        {
            Pipeline::Step& step = *l;
            cout << "    Step <" << *step.fqname
                 << "> action <" << step.factory->getName()
                 << "> reused:" << step.reused
                 << endl;
            if (step.args != 0)
                for (Args::Store::iterator m = step.args->store.begin(); m != step.args->store.end(); ++m)
                {
                    Args::Arg& arg = *m;
                    cout << "      arg <" << arg.key << "> = <" << arg.value 
                         << "> strength:" << arg.strength
                         << endl;
                }

            if (step.logs != 0)
                for (size_t sid = 0; sid < step.logs->streamidxs.size(); ++sid)
                {
                    size_t idx = step.logs->streamidxs.at(sid);
                    cout << "      sid#" << sid
                         << " idx#" << idx
                         << endl;
                }
        }
    }
#endif

//  Initialize commom pipeline only (others get intialized as need by PipelineIterator)

    compipeline.init();
}


void PMF::deinit ()
{
    for (Pipelines::iterator it = pipelines.begin(); it != pipelines.end(); ++it)
        (*it)->deinit();
}


void PMF::initStreams ()
{
//  There are two forms of the log command:
//
//  1) stream enable
//      [<jit>.][<filter>.]arg.[<path>.]log = <stream>, <stream>, ...
//
//  2)  stream assignment
//      [<jit>.][<filter>.]arg.[<path>.]log.<stream>.file = <file mask>

    PMF::Cmd* cmdp;
    for (PMF::Cmds::iterator it = cmds.begin(); it != cmds.end(); ++it)
        if ((cmdp = *it) != 0 && cmdp->arg)
        {
            Strs::iterator leftbeg = cmdp->left->begin(),
                           leftend = cmdp->left->end();

            Strs::iterator i = find(leftbeg + cmdp->xkeyword + 1, leftend,  "log");
            //  log command must contain the "log" keyword
            if (i == leftend)
                continue;

            cmdp->log = true;
            cmdp->xlog = int(i - leftbeg);

            int xpath = cmdp->xkeyword + 1,
                xlast = (int)cmdp->left->size() - 1;

            if (cmdp->xlog == xlast)
            {// stream enable command
                for (StrTokenizer tokens(',', cmdp->right); tokens.next();)
                {
                    LogTemplate& lt = lookStream(tokens.token, cmdp, xpath, cmdp->xlog);
                    lt.enabled = true;
                }
            }
        }

    for (LogTemplates::iterator it = logtemplates.begin(); it != logtemplates.end(); ++it)
    {
        LogTemplate& lt = *it;

        int file_strength = 0,
            append_strength = 0;

        for (PMF::Cmds::iterator kt = cmds.begin(); kt != cmds.end(); ++kt)
            if ((cmdp = *kt) != 0 && cmdp->log)
            {
                if (!cmdp->filtername.empty() && !(cmdp->filtername == lt.filtername))
                    continue;

                int xpath  = cmdp->xkeyword + 1,
                    xlast  = (int)cmdp->left->size() - 1,
                    pathsz = cmdp->xlog - xpath;

                Str& key = cmdp->left->at(xlast);
                if (key == "log")
                    continue;

            //  compare stream name

                if (cmdp->xlog == xlast - 2)
                if (!(cmdp->left->at(cmdp->xlog+1) == lt.streamname))
                    continue;

            // compare path names

                int ltpathsz = (lt.pathp == 0)  ? 0 : (int)lt.pathp->size();
                if (pathsz > ltpathsz)
                    continue;
                if (pathsz != 0 && !compare(&cmdp->left->at(xpath), &lt.pathp->at(0), pathsz))
                    continue;

                int strength = cmdp->strength(pathsz);

                if (key == "file" && strength > file_strength)
                {//  stream assignment command
                    lt.fmask = cmdp->right;
                    file_strength = strength;
                }
                else if (key == "append" && strength > append_strength)
                {
                    lt.append = (cmdp->right == "true" || cmdp->right == "yes");
                    file_strength = strength;
                }
                else
                    cmdp->fatal("Invalid log command");
            }
    }

//  Find out if file name mask containds some key macros

    for (LogTemplates::iterator it = logtemplates.begin(); it != logtemplates.end(); ++it)
    {
        LogTemplate& lt = *it;
        lt.jit_specific = lt.fmask.find("%jit%") != 0;
        lt.thread_specific = lt.fmask.find("%thread%") != 0;
    }

#ifdef _DEBUG_PMF
    cout << endl << "LogTemplates" << endl;
    for (size_t idx = 0; idx != logtemplates.size(); ++idx)
    {
        LogTemplate& lt = logtemplates.at(idx);
        cout << idx 
                << ")  stream <" << lt.streamname 
                << "> enabled=" << lt.enabled
                << " append="   << lt.append
                << " jit_specific="  << lt.jit_specific
                << " thread_specific="  << lt.thread_specific

                << " filter <"  << lt.filtername 
                << "> path <";
        if (lt.pathp != 0)
            cout << *lt.pathp;
        cout << "> fmask <"  << lt.fmask 
                << ">" << endl;
    }
#endif
}


LogTemplate& PMF::lookStream (Str& streamname, PMF::Cmd* cmdp, size_t xpath, size_t xlog)
{
    LogTemplates::iterator end = logtemplates.end(),
                           ptr = logtemplates.begin();
    for (; ptr != end; ++ptr)
    {
        if (ptr->filtername == cmdp->filtername &&
            ptr->streamname == streamname)
        {
            if (ptr->pathp->empty() && xpath != xlog)
                continue;
            if (!ptr->pathp->empty())
            {
                if (ptr->pathp->size() != xlog - xpath)
                    continue;
                if (!compare(&ptr->pathp->at(0), &cmdp->left->at(xpath), xlog - xpath))
                    continue;
            }
            cmdp->fatal("Duplicate streams defined");
        }
    }

    if (logtemplates.empty())
    {// create the "sink' stream at index o
        logtemplates.push_back(LogTemplate());
        LogTemplate& lt = logtemplates.back();
        lt.idx = 0;
        lt.streamname.init("sink");
    }

    logtemplates.push_back(LogTemplate());
    LogTemplate& lt = logtemplates.back();
    lt.idx = logtemplates.size()-1;
    lt.filtername = cmdp->filtername;
    lt.streamname = streamname;

    lt.pathp = new (mm) Strs(mm);
    if (xpath != xlog)
    {
        Strs::iterator leftbeg = cmdp->left->begin();
        lt.pathp->insert(lt.pathp->end(), leftbeg + xpath, leftbeg + xlog);
    }

    lt.fmask = deffmask;
    lt.sid   = static_cast<LogStream::SID>(nb_knownstreams);
    KnownStream* ksp = isKnownStream(streamname);
    if (ksp != 0)
    {
        lt.fmask = ksp->deffmask;
        lt.sid   = ksp->sid;
    }

    if (ksp != 0 && ksp->sid == LogStream::RT)
    {
        if (!lt.pathp->empty() || !lt.filtername.empty())
            crash("'rt' stream must be defined at global level only\n");
    }

    return lt;
}


PMF::HPipeline PMF::selectPipeline (const char* cn, const char* mn, const char* sig) const
{
    Pipeline* pipeline = *pipelines.begin();
    int xs = 0, x;

    if (cn != 0 || mn != 0 || sig != 0)
        for (Pipelines::const_iterator i = pipelines.begin()+1; i != pipelines.end(); ++i)
            if ((x = (*i)->method.pass(cn, mn, sig)) > xs)
                pipeline = *i,
                xs = x;

    return pipeline;
}

 
PMF::HPipeline PMF::getPipeline (const char* name) const
{
//  Common filter is always the first in the array of filters
    assert(!pipelines.empty());
    if (name == 0 || *name == 0)
        return pipelines[0];

    Pipelines::const_iterator ptr = pipelines.begin()+1,
                              end = pipelines.end();
    for (; ptr != end; ++ptr)
        if ((*ptr)->name == name)
            return *ptr;

    return 0;
}

 
const char* PMF::getArg (HPipeline hpipe, const char* key) const
{
    vector<Str> keys;
    for (StrTokenizer tokens('.', key, key + strlen(key)); tokens.next();)
        keys.push_back(tokens.token);

    const Cmd* cmdp = lookArg(hpipe, &keys[0], keys.size());
    if (cmdp != 0)
        return cmdp->right.ptr;

    return 0;
}


Action* PMF::getAction (HPipeline hpipe, const char* path)
{
    vector<Str> fqname;
    for (StrTokenizer tokens('.', path, path + strlen(path)); tokens.next();)
        fqname.push_back(tokens.token);

    Pipeline::Steps::iterator ptr = hpipe->steps->begin(),
                              end = hpipe->steps->end();
    for (; ptr != end; ++ptr)
    {
        Pipeline::Step& step = *ptr;
        if (step.fqname->size() == fqname.size() &&
            compare(&step.fqname->at(0), &fqname.at(0), fqname.size()))
            return step.action;
    }

    return 0;
}

 
void PMF::walk (Pipeline& pipeline, Pipeline::Alias* aliasp, Strs& fqname)
{
    Pipeline::Alias::Childs::iterator end = aliasp->childs.end(),
                                    ptr = aliasp->childs.begin();
    for (; ptr != end; ++ptr)
    {
        Pipeline::Alias::Child& child = *ptr;
        fqname.push_back(child.name);
        //cout << "walk " << fqname << endl;;

        bool goon = (child.type != Pipeline::Alias::Child::DEFOFF);
        const Cmd* cmdp = lookArg(&pipeline, &fqname[0], fqname.size());
        if (cmdp != 0)
        {
            if (cmdp->right == "on")
            {
                goon = true;
            }
            else if (cmdp->right == "off")
            {
                if (ptr->type == Pipeline::Alias::Child::REQUIRED)
                    cmdp->fatal("This path item cannot be off");
                goon = false;
            }
            else
                cmdp->fatal("Invalid path item selection");
        }

        if (goon) {
            if (child.aliasp != 0)
                walk(pipeline, child.aliasp, fqname);
            else
            {
                Pipeline::Step step;
                if ((step.factory = IActionFactory::find(child.name)) == 0)
                    crash("PMF: Action '%s' not found\n", c_str(mm, child.name));

                step.pipeline = &pipeline;
                step.fqname = new (mm) Strs(mm, fqname.size());
                *step.fqname = fqname;
                pipeline.steps->push_back(step);
            }
        }

        fqname.pop_back();
    }
}


const PMF::Cmd* PMF::lookArg (Pipeline* pipeline, const Str* fqname, size_t fqsize) const
{
    const Cmd* cmdp = 0;
    int sx = 0;

    for (Cmds::const_iterator it = cmds.begin(); it != cmds.end(); ++it)
        if (*it != 0  && (*it)->arg)
        {
            const Cmd& cmd = **it;

            if (pipeline == 0 || pipeline->name.empty())
            {
                if (!cmd.filtername.empty())
                    continue;
            }
            else
            {
                if (!cmd.filtername.empty() && !(cmd.filtername == pipeline->name))
                    continue;
            }

            if (cmd.left->size() - cmd.xkeyword -1 != fqsize)
                continue;

            if (!compare(&cmd.left->at(cmd.xkeyword+1), fqname, fqsize))
                continue;

            int s = cmd.strength();
            if (cmdp == 0)
                cmdp = &cmd,
                sx = s;
            else
            {
                if (s == sx)
                    crash("Ambigiguity between commands '%s' and '%s'", cmdp->buff, cmd.buff);

                if (s > sx)
                    cmdp = &cmd,
                    sx = s;
            }
        }

    return cmdp;
}


PMF::Pipeline* PMF::lookup (Str* name, bool create)
{
//  Common filter is always the first in the array of filters
    assert(!pipelines.empty());
    if (name == 0 || name->empty())
        return pipelines[0];

    Pipelines::iterator ptr = pipelines.begin()+1,
                        end = pipelines.end();
    for (; ptr != end; ++ptr)
        if ((*ptr)->name == *name)
            return *ptr;

    if (!create)
        return 0;

    // add required filter
    Pipeline* pipeline = new (mm) Pipeline(*this);
    pipeline->name = *name;
    pipelines.push_back(pipeline);
    return pipeline;
}


static bool compFactories (IActionFactory* a, IActionFactory* b)
{
    return strcmp(a->getName(), b->getName()) < 0;
}


void  PMF::showHelp (std::ostream& os)
{
    IActionFactory* afp;

    if ((afp = IActionFactory::find(help_requested)) != 0)
        afp->showHelp(os);
    else
    {
        typedef vector<IActionFactory*> Factories;
        Factories tmp;

        for (afp = IActionFactory::getFirst(); afp != 0; afp = afp->getNext())
            tmp.push_back(afp);

        sort(tmp.begin(), tmp.end(), compFactories);

        os << endl << "Help for Jitrino Actions (when available)" << endl;
        for (Factories::iterator it = tmp.begin(); it != tmp.end(); ++it)
            (*it)->showHelp(os);
    }
}


void PMF::showHelpJits (std::ostream& os)
{
    os << endl << "Jit " << jitname << endl;
    for (Pipelines::iterator k = pipelines.begin(); k != pipelines.end(); ++k)
    {
        Pipeline& pipeline = **k;

        os << "  Pipeline ";
        if (&pipeline == pipelines.front())
            os << "<common>";
        else
            os << pipeline.name 
               << " filter " << pipeline.method.classname        
               << "." << pipeline.method.methodname 
               << pipeline.method.signature;
        os << endl;

    //  Print pipeline-wide args 
        for (Cmds::const_iterator i = cmds.begin(); i != cmds.end(); ++i)
            if (*i != 0 && (*i)->arg)
            {
                const Cmd& cmd = **i;
                if (cmd.filtername.empty() || (cmd.filtername == pipeline.name))
                    if (cmd.left->size() - cmd.xkeyword == 2)
                        os << "    arg " << cmd.left->at(cmd.xkeyword + 1) << "=" << cmd.right << endl;
            }


    //  Print pipeline-wide streams 
        for (size_t idx = 0; idx != logtemplates.size(); ++idx)
        {
            LogTemplate& lt = logtemplates[idx];
            if (lt.enabled && (lt.filtername == pipeline.name))
                if (lt.pathp == 0 || lt.pathp->empty())
                {
                    os << "    stream " << lt.streamname 
                       << "[" << idx << "]"
                       << " file " << lt.fmask 
                       << endl;
                }
        }

    //  Print list of actions and specific args/streams
        if (!pipeline.steps->empty())
        {
            os << "    Actions" << endl;
            for (Pipeline::Steps::iterator l = pipeline.steps->begin(); l != pipeline.steps->end(); ++l)
            {
                Pipeline::Step& step = *l;
                os << "      " << *step.fqname << endl;

                if (step.args != 0)
                    for (Args::Store::iterator m = step.args->store.begin(); m != step.args->store.end(); ++m)
                    {
                        Args::Arg& arg = *m;
                    //  Do not print pipeline-wide args
                        if (arg.cmdp->left->size() - arg.cmdp->xkeyword > 2)
                            os << "        arg " << arg.key << "=" << arg.value << endl;
                    }

                if (step.logs != 0)
                    for (size_t sid = 0; sid < step.logs->streamidxs.size(); ++sid)
                    {
                        size_t idx = step.logs->streamidxs.at(sid);
                        LogTemplate& lt = logtemplates.at(idx);
                        if (lt.enabled)
                            if (lt.pathp != 0 && !lt.pathp->empty())
                                os << "        stream " << lt.streamname 
                                << "[" << idx << "]"
                                << " file " << lt.fmask 
                                << endl;
                    }
            }
        }
    }
}

 
void PMF::summTimes (SummTimes& summtimes)
{
    Pipelines::iterator pptr = pipelines.begin(), 
                        pend = pipelines.end();
    for (; pptr != pend; ++pptr)
    {
        Pipeline::Steps::iterator sptr = (*pptr)->steps->begin(), 
                                  send = (*pptr)->steps->end(); 
        for (; sptr != send; ++sptr)
            summtimes.add(sptr->factory->getName(), sptr->getSeconds());
    }
}


//------ Pipeline implementation --------------------------------------------//


PMF::Pipeline::Pipeline (PMF& p)
:pmf(p), initialized(false), root(0) 
{
    aliases = new (pmf.mm) Aliases(pmf.mm);
    steps   = new (pmf.mm) Steps(pmf.mm);
}


PMF::Pipeline::~Pipeline ()
{
}


void PMF::Pipeline::init ()
{
    if (!initialized)
    {
        initialized = true;
        Pipeline::Steps::iterator ptr = steps->begin(),
                                  end = steps->end();
        for (; ptr != end; ++ptr)
        {
            Pipeline::Step& step = *ptr;
            if (step.action != 0 && !step.reused)
            {
                step.action->step = &step;
                step.action->init();
            }
        }
    }
}


void PMF::Pipeline::deinit ()
{
    if (initialized)
    {
        initialized = false;
        Pipeline::Steps::iterator ptr = steps->begin(),
                                  end = steps->end();
        for (; ptr != end; ++ptr)
        {
            Pipeline::Step& step = *ptr;
            if (step.action != 0 && !step.reused)
                    step.action->deinit();
            }
        }
}


PMF::Pipeline::Alias* PMF::Pipeline::lookup (Str* name)
{
    Aliases::iterator ptr = aliases->begin(),
                      end = aliases->end();
    for (; ptr != end; ++ptr)
    {
        Alias* aliasp = *ptr;
        if ((name != 0 && *name == aliasp->name) ||
            (name == 0 && aliasp->name.empty()))
            return aliasp;
    }

    return 0;
}


PMF::Pipeline::Alias* PMF::Pipeline::findPath (Str* names, size_t count)
{
    Alias* aliasp = root;

    for (; aliasp != 0 && count != 0; --count)
    {
        Alias::Childs::iterator end = aliasp->childs.end(),
                                ptr = find(aliasp->childs.begin(), end, *names);
        if (ptr == end)
            return 0;

        aliasp = ptr->aliasp;
        ++names;
    }

    return aliasp;
}


void PMF::Pipeline::stop (const char* msg)
{
    crash(msg, c_str(pmf.mm, pmf.jitname), c_str(pmf.mm, name));
}


//------ PMF::Pipeline::Step implementation ---------------------------------//


void PMF::Pipeline::Step::setup (MemoryManager& mm)
{
    args = 0;
    for (ArgIterator it(*this, 0); it.next();)
    {
        Cmd& cmd = *it;

        if (args == 0)
            args = new (mm) Args(mm);

        const char* key   = c_str(cmd.left->at(cmd.left->size() - 1)); 
        const char* value = c_str(cmd.right);
        int strength = cmd.strength(cmd.left->size() - cmd.xkeyword - 1);
        args->add(key, value, strength, &cmd);
    }

//  all templates in scope for this step
    vector<LogTemplate*> acts;

    LogTemplates& logtemplates = pipeline->pmf.logtemplates;
    for (size_t sx = 0; sx != logtemplates.size(); ++sx)
    {
        LogTemplate& lt = logtemplates[sx];

        if (!lt.enabled)
            continue;

        if (!(lt.filtername == pipeline->name))
            continue;

        const size_t pathsz = lt.pathp->size();

        if (pathsz > fqname->size())
            continue;

        if (pathsz == 1 && lt.pathp->at(0) == factory->getName())
            ;
        else if (pathsz != 0 && !compare(&lt.pathp->at(0), &fqname->at(0), lt.pathp->size()))
            continue;

        vector<LogTemplate*>::iterator it, end = acts.end();
        for (it = acts.begin(); it != end; ++it)
            if ((*it)->streamname == lt.streamname)
                break;

        if (it == end)
            acts.push_back(&lt);
        else if (cmd_strength(false, !(*it)->filtername.empty(), (*it)->pathp->size()) 
               < cmd_strength(false, !lt.filtername.empty(),     pathsz))
            *it = &lt;  // this definition is the strongest
    }

    logs = 0;
    if (!acts.empty())
    {
        logs = new (mm) LogDisplay(mm);

        vector<LogTemplate*>::iterator it, end = acts.end();
        for (it = acts.begin(); it != end; ++it)
        {
            LogTemplate lt = **it;
            logs->add(lt.idx, lt);
        }
    }
}


//------ PMF::PipelineIterator implementation -------------------------------//


PMF::PipelineIterator::PipelineIterator (PMF& p, const char* cn, const char* mn, const char* sig)
:pmf(p), mm("PMF::PipelineIterator"), smm(0), session(0), first(true)
{
    if (pmf.pipelines.empty())
        crash("PMF: No pipelines defined\n");

    pipeline = pmf.selectPipeline(cn, mn, sig);
    pipeline->init();
}


PMF::PipelineIterator::PipelineIterator (HPipeline hpipe)
:pmf(hpipe->pmf), pipeline(hpipe), mm("PMF::PipelineIterator"), smm(0), session(0), first(true)
{
    pipeline->init();
}


PMF::PipelineIterator::~PipelineIterator ()
{
}


bool PMF::PipelineIterator::next ()
{
    Pipeline::Steps::iterator end = pipeline->steps->end();

    if (first)
    {
        first = false;
        it = pipeline->steps->begin();
    }
    else 
    {
        assert(it != end);

    //  close previous session
        session->step = 0;
        session->~SessionAction();
        session = 0;
		 delete smm;
    //  go to new session
        ++it;
    }

    if (it != end)
    {// open new session
        smm = new MemoryManager(it->factory->getName());
        session = it->factory->createSessionAction(*smm);
        assert(session != 0);
        session->step = &*it;
        return true;
    }
    else
        return false;
}


const char* PMF::PipelineIterator::getPipeName (MemoryManager& mm) const
{
    assert(pipeline != 0);
    return c_str(mm, pipeline->name);
}


const char* PMF::PipelineIterator::getFilterSpec (MemoryManager& mm) const
{
    assert(pipeline != 0);
    return pipeline->method.c_str(mm);
}


const char* PMF::PipelineIterator::getStepName () const
{
    assert(!first && it != pipeline->steps->end());
    return c_str(*smm, &it->fqname->at(0), it->fqname->size());
}

 
//------ ActionFactory implementation ---------------------------------------//


IActionFactory* IActionFactory::head = 0;


IActionFactory::IActionFactory (const char* n)
:name(n)
{
    Str sname(name);
    if (find(sname) != 0)
        crash("PMF: duplicate Action '%s'\n", name);

    next = head;
    head = this;
}


void IActionFactory::showHelp (ostream& os, const char* help) const
{
    if (help != 0)
        os << endl << getName() << endl
           << help;
}

 
IActionFactory* IActionFactory::find (Str& str)
{
    for (IActionFactory* p = getFirst(); p != 0; p = p->getNext())
        if (p->name == str)
            return p;

    return 0;
}
 
        
//------ IAction implementation ---------------------------------------------//


bool IAction::getArg (const char* key, unsigned int& v) const
{
    const char* arg = getArg(key);
    if (arg == 0)
        return false;
    else
    {
        v = atoi(arg);
        return true;
    }
}


bool IAction::getArg (const char* key, unsigned long& v) const
{
    const char* arg = getArg(key);
    if (arg == 0)
        return false;
    else
    {
        v = atol(arg);
        return true;
    }
}


bool IAction::getArg (const char* key, bool& v) const
{
    const char* arg = getArg(key);
    if (arg == 0)
        return false;
    else
    {
        if (strcmp(arg, "true") == 0 || strcmp(arg, "yes") == 0)
            v = true;
        else if (strcmp(arg, "false") == 0 || strcmp(arg, "no") == 0)
            v = false;
        else
            crash("PMF: invalid value for bool parameter '%s' (true|false or yes|no expected)\n", key);

        return true;
    }
}


const char* IAction::getArg (HPipeline hp, const char* key) const 
{
    return step->pipeline->pmf.getArg(const_cast<PMF::HPipeline>(hp), key);
}


#ifndef _NOLOG
bool IAction::isLogEnabled (LogStream::SID sid) const
{
    return step->logs != 0 && step->logs->log(sid).isEnabled();
}
#endif


#ifndef _NOLOG
bool IAction::isLogEnabled (const char* streamname) const
{
    LogStream::SID sid;
    if (getLogStreamID(sid, streamname))
        return isLogEnabled(sid);
    else
        return false;
}
#endif


#ifndef _NOLOG
LogStream& IAction::log (LogStream::SID sid) const
{
    CompilationContext* currentCC = CompilationContext::getCurrentContext();
    CompilationContext* topLevelCC = CompilationContext::getCurrentContext()->getVMCompilationInterface()->getCompilationContext();
    const IAction* action = this;
    if (currentCC != topLevelCC) {
        action = topLevelCC->getCurrentSessionAction();
    }
    return (action == NULL || action->step->logs == NULL) ? LogStream::log_sink() : action->step->logs->log(sid);
}
#endif


#ifndef _NOLOG
LogStream& IAction::log (const char* streamname) const
{
    LogStream::SID sid;
    if (getLogStreamID(sid, streamname))
        return log(sid);
    else
        return LogStream::log_sink();
}
#endif


bool IAction::getLogStreamID (LogStream::SID& sid, const char* streamname) const
{
    if ((step->logs) == 0)
        return false;

    LogTemplates& logtemplates = step->pipeline->pmf.logtemplates;
    LogDisplay::StreamIdxs& idxs = step->logs->streamidxs;
    for (size_t i = 0; i != idxs.size(); ++i)
        if (logtemplates[idxs[i]].streamname == streamname)
        {
            sid = static_cast<LogStream::SID>(i);
            return true;
        }

    return false;
}


//------ Action implementation ----------------------------------------------//


Action* Action::createAuxAction (const char* name, const char* suffix) const
{
    MemoryManager& mm = step->pipeline->pmf.mm;
    return createAuxAction (mm, name, suffix);
}


Action* Action::createAuxAction (MemoryManager& mm, const char* name, const char* suffix) const
{
    PMF::Pipeline::Step* auxstep = new (mm) PMF::Pipeline::Step;
    auxstep->factory  = step->factory;
    auxstep->pipeline = getPipeline();

    Strs* fqname = new (mm) Strs(mm);
    fqname->insert(fqname->end(), step->fqname->begin(), step->fqname->end());
    fqname->push_back(Str(suffix));
    auxstep->fqname = fqname;

    Str sname(name);
    auxstep->factory = IActionFactory::find(sname);
    assert(auxstep->factory != 0);

    Action* auxaction = auxstep->factory->createAction(mm);
    auxaction->step = auxstep;
    auxstep->action = auxaction;

    auxstep->setup(mm);
    return auxaction;
}


SessionAction* Action::createSession (MemoryManager& mm)
{
    SessionAction* session = step->factory->createSessionAction(mm);
    assert(session != 0);
    session->step = step;
    return session;
}


/*
void Action::destroySession (SessionAction* session)
{
    session->step = 0;
    session->~SessionAction();
}
*/


//---------------------------------------------------------------------------//


} //namespace Jitrino
