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

//
// Decides whether or not to "accept" a method based on an environment
// variable and an optional file.
//
// Assume FOO is the environment variable that gives the information.
//
// FOO=file:range_list
// 
// "file" is the name of the file that contains the list of all
// methods.
//
// If "range_list" is empty, no methods are jitted, and the list of
// methods is printed to "file".
//
// "range_list" contains a list of methods and ranges, separated by
// commas.  A single number denotes a one-based index into the method
// list.  A range (start-end) denotes a range of indices into the
// method list.
// The string "class::method" denotes a specific method.
// "class::" matches all methods of the given class.
// "method" (not containing a colon) matches that method of
// any class.
// A "-" preceding a list element means to exclude that method(s) from
// the list of methods to jit.
//
// FOO=c:\tmp\mlist:  (jit nothing, create a method list file)
// FOO=mlist:  (jit nothing, create a file in the current directory)
// FOO=list:1-1000 (jit the first 1000 methods from file "list")
// FOO=java/lang/Object::<init>  (jit just a single method)
// FOO=-<init>,-<clinit> (jit all but initializers)
// FOO=list:1-100,-50-60 (jit methods 1-49 and 61-100)
// FOO=(I)  (jit only methods with a single int in the signature)
// FOO=(Ljava/lang/Object;)
// FOO=(L;)  (jit only methods with a single class ptr in signature)

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "VMInterface.h"
#include "Type.h"
#include "methodtable.h"

namespace Jitrino {

#define CLASS_METHOD_SEPARATOR_CHAR ':'
#define DESCRIPTOR_START_CHAR '('
#define DESCRIPTOR_END_CHAR ')'
#define REF_START_CHAR 'L'
#define REF_END_CHAR ';'
#define FILE_CHAR ':'


char* strdup(MemoryManager& mm , const char* str) {
    size_t len  = strlen(str);
    char* newstr = new (mm) char[len+1];
    strncpy(newstr, str, len);
    newstr[len]='\0';
    return newstr;
}

void Method_Table::make_filename(char *str, int len)
{
    _method_file = new (_mm) char[1+len];
    strncpy(_method_file, str, len);
    _method_file[len] = '\0';
}

// Decides whether two signatures match.  The strictest test is
// strcmp(), but we are more lenient by allowing "L;" instead of,
// e.g., "Ljava/lang/Object;".  Anything following the ')' (i.e.,
// the return value) of either string is ignored.
static bool matching_signature(const char *lenient, const char *exact)
{
    int i, j;
    int len1 = (int) strlen(lenient);
    int len2 = (int) strlen(exact);
    for (i=j=0; i<len1 && j<len2; i++,j++)
    {
        if (lenient[i] != exact[j])
            return false;
        if (lenient[i] == DESCRIPTOR_END_CHAR)
            return true;
        if (lenient[i] == REF_START_CHAR)
        {
            i++; j++;
            if (lenient[i] == REF_END_CHAR) // accept any classname
            {
                while (j < len2 && exact[j] != REF_END_CHAR)
                    j ++;
            }
            else
            {
                while (i < len1 && j < len2 && lenient[i] != REF_END_CHAR)
                {
                    if (lenient[i] != exact[j])
                        return false;
                    i++; j++;
                }
            }
        }
    }
    return (i == len1 && j == len2);
}

// [class::][method][(signature)]
static void parse_method_string(MemoryManager& _mm, char *str, Method_Table::method_record *rec)
{
    int i;
    int len = (int) strlen(str);
    
    rec->class_name = rec->method_name = rec->signature = NULL;
    if (len == 0)
        return;

    bool is_at_class_method_separator = false;
    // Search forward for CLASS_METHOD_SEPARATOR_CHAR or DESCRIPTOR_START_CHAR
    for (i=0; i<len; i++)
    {
        is_at_class_method_separator =
            (str[i] == CLASS_METHOD_SEPARATOR_CHAR && str[i+1] == CLASS_METHOD_SEPARATOR_CHAR);
        if (is_at_class_method_separator)
        {
            i ++; // make i point to the last character of the separator
            break;
        }
        if (str[i] == DESCRIPTOR_START_CHAR)
            break;
    }    
    if (i >= len) // no class or descriptor
    {
        if (str[0] != '\0')
            rec->method_name = strdup(_mm, str);
        return;
    }
    if (is_at_class_method_separator)
    {
        // extract the class
        rec->class_name = new (_mm) char[i-1 + 1];
        strncpy(rec->class_name, str, i-1);
        rec->class_name[i-1] = '\0';
        // skip ahead to the descriptor
        str += (i+1);
        len -= (i+1);
        for (i=0; i<len; i++)
        {
            if (str[i] == DESCRIPTOR_START_CHAR)
                break;
        }
        if (i >= len) // no descriptor
        {
            if (str[0] != '\0')
                rec->method_name = strdup(_mm, str);
            return;
        }
    }
    // we're at the start of the signature, with the method name preceding
    if (i > 0)
    {
        rec->method_name = new (_mm) char[i+1];
        strncpy(rec->method_name, str, i);
        rec->method_name[i] = '\0';
    }
    rec->signature = strdup(_mm, &str[i]);
}

// Returns true on success, false on failure
bool Method_Table::read_method_table()
{
    const size_t max_size = 1000;
    char buf[max_size];
    FILE *file = fopen(_method_file, "r");
    if (file == NULL)
    {
        fprintf(stderr, "Couldn't open method table file %s\n", _method_file);
        return false;
    }
    while (fgets(buf, max_size, file) != NULL)
    {
        // strip out any newline at the end
        int buflen = (int) strlen(buf);
        if (buf[buflen-1] == '\n')
            buf[buflen-1] = '\0';
        if (buf[buflen-2] == '\r') // file generated on NT and used on Linux,
            buf[buflen-2] = '\0';    // this case happens
        method_record* rec = new (_mm) method_record();
        parse_method_string(_mm, buf, rec);
        _method_table.push_back(rec);
    }
    fclose(file);
    return true;
}

static bool matches(Method_Table::method_record *test_entry,
                    const char *class_name, const char *method_name, const char *signature)
{
    if (test_entry->class_name != NULL && strcmp(test_entry->class_name, class_name) != 0)
        return false;
    if (test_entry->method_name != NULL && strcmp(test_entry->method_name, method_name) != 0)
        return false;
    if (test_entry->signature != NULL && !matching_signature(test_entry->signature, signature))
        return false;
    return true;
}

void Method_Table::init(const char *default_envvar, const char *envvarname)
{
    if (default_envvar == NULL || default_envvar[0] == '\0')
    {
        return;
    }

    char *rangestr;
    char *envvar = strdup(_mm, default_envvar);
    
   // strip away double-quote characters
    if (envvar[0] == '"') {
        envvar ++;
    }
    int evlen = (int) strlen(envvar);
    if (evlen > 0 && envvar[evlen-1] == '"') {
        envvar[--evlen] = '\0';
    }
    if (evlen == 0) {
        return;
    }
    int i;
    for (i=evlen-1; i>=0; i--)
    {
        if (FILE_CHAR == CLASS_METHOD_SEPARATOR_CHAR)
        {
            // If we are looking at FILE_CHAR and the character to the left is not FILE_CHAR,
            // then this is a legitimate FILE_CHAR.
            if (envvar[i] == FILE_CHAR && (i == 0 || envvar[i-1] != FILE_CHAR))
                break;
            // If we are looking at FILE_CHAR and the character to the left is also FILE_CHAR,
            // then we are actually looking at a double CLASS_METHOD_SEPARATOR_CHAR, which
            // we should skip over.
            if (envvar[i] == FILE_CHAR && i != 0 && envvar[i-1] == FILE_CHAR)
                i --;
        } else {
            if (envvar[i] == FILE_CHAR)
                break;
        }
    }
    if (i == 0) // no legitimate filename given
    {
        return;
    }
    else if (i == evlen-1) // filename only, no ranges
    {
        make_filename(envvar, evlen-1);
        _dump_to_file = true;
        FILE *file = fopen(_method_file, "w");
        if (file == NULL)
        {
            fprintf(stderr, "Couldn't truncate method table file %s\n",
                _method_file);
            _dump_to_file = false;
        }
        else
            fclose(file);
        return;
    }
    else if (i >= 0) // filename plus ranges
    {
        make_filename(envvar, i);
        rangestr = &envvar[i+1];
        read_method_table();
    }
    else // no filename, only ranges
    {
        rangestr = envvar;
    }
    // parse the ranges in rangestr
    for (rangestr=strtok(rangestr, ","); rangestr!=NULL; rangestr=strtok(NULL, ","))
    {
        int opposite = 0;
        int start, end;
        if (rangestr[0] == '-')
        {
            opposite = 1;
            rangestr ++;
        }
        else
        {
            _default_decision = mt_rejected;
        }
        if (rangestr[0] >= '0' && rangestr[0] <= '9') {
            // look for a range of numbers
            sscanf(rangestr, "%d", &start);
            end = start;
            while (rangestr[0] != '\0' && rangestr[0] != '-') {
                rangestr ++;
            }
            if (rangestr[0] == '-') {
                sscanf(rangestr+1, "%d", &end);
            }
            start --;
            if (start < 0) {
                start = 0;
            }
            end --;
            for (i=start; i<=end && i<(int)_method_table.size(); i++) {
                _method_table[i]->decision = (opposite ? mt_rejected : mt_accepted);
            }
        } else {
            method_record* rec = new (_mm) method_record();
            parse_method_string(_mm, rangestr, rec);
            rec->decision = (opposite ? mt_rejected : mt_accepted);
            _decision_table.push_back(rec);
        }
    }

    // change all "undecided" to default decision
    for (i=0; i<(int)_method_table.size(); i++) {
        if (_method_table[i]->decision == mt_undecided) {
            _method_table[i]->decision = _default_decision;
        }
    }
    
}

Method_Table::Method_Table(MemoryManager& memManager, 
                           const char *default_envvar,
                           const char *envvarname,
                           bool accept_by_default):
  _mm(memManager),
  _method_table     (_mm),
  _decision_table   (_mm),
  _dump_to_file     (false),
  _method_file      (NULL)
{
    _default_decision = accept_by_default ? mt_accepted : mt_rejected;
    init(default_envvar, envvarname);
}
  
bool Method_Table::accept_this_method(MethodDesc &md) {
    const char* classname = md.getParentType()->getName();
    const char *methodname = md.getName();
    const char *signature = md.getSignatureString();

    return accept_this_method(classname, methodname, signature);
}

bool Method_Table::accept_this_method(const char* classname, const char *methodname, const char *signature)
{
    int i;
    
    
    if (_dump_to_file)
    {
        FILE *file = fopen(_method_file, "a");
        if (file != NULL)
        {
            fprintf(file, "%s%c%c%s%s\n", classname, CLASS_METHOD_SEPARATOR_CHAR, CLASS_METHOD_SEPARATOR_CHAR, methodname, signature);
            fclose(file);
        }
        return false;
    }

    // First look through the decision_table strings.
    for (i=0; i<(int)_decision_table.size(); i++)
    {
        if (matches(_decision_table[i], classname, methodname, signature)) {
            return (_decision_table[i]->decision == mt_accepted);
        }
    }

    // Then look through the method table.
    for (i=0; i<(int)_method_table.size(); i++) {
        if ((_method_table[i]->class_name==NULL || !strcmp(_method_table[i]->class_name, classname)) &&
            (_method_table[i]->method_name==NULL || !strcmp(_method_table[i]->method_name, methodname)) &&
            (_method_table[i]->signature==NULL || matching_signature(_method_table[i]->signature, signature)))
        {
            return (_method_table[i]->decision == mt_accepted);
        }
    }
    return (_default_decision == mt_rejected ? false : true);
}

bool Method_Table::is_in_list_generation_mode() {
    return _dump_to_file;
}

void Method_Table::add_method_record(const char* className, const char* methodName, const char* signature, Method_Table::Decision decision, bool copyVals) {
    method_record* rec = new (_mm) method_record();
    if (copyVals) {
        size_t len = strlen(className)+1; //+1 == '\0' char
        rec->class_name = new (_mm) char[len];
        strncpy(rec->class_name, className, len);
        
        len = strlen(methodName)+1; //+1 == '\0' char
        rec->method_name= new (_mm) char[len];
        strncpy(rec->method_name, methodName, len);

        len = strlen(signature)+1; //+1 == '\0' char
        rec->signature= new (_mm) char[len];
        strncpy(rec->signature, signature, len);
    } else {
        rec->class_name = (char*)className;
        rec->method_name = (char*)methodName;
        rec->signature = (char*)signature;
    }
    rec->decision = decision;
    _method_table.push_back(rec);
}

} //namespace Jitrino 

